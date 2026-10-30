package com.diggydwarff.deepdiving.chart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.List;

/** Vanilla map storage, packets and rendering, with a bounded server-side bathymetry sampler. */
public final class DepthMapItem extends MapItem {
    private static final String SURVEY_BUDGET = "deepdiving.chartBudget";

    public DepthMapItem(Properties properties) {
        super(properties);
    }

    @Override
    protected MapItemSavedData getCustomMapData(ItemStack stack, Level level) {
        MapItemSavedData data = super.getCustomMapData(stack, level);
        if (data != null && !level.isClientSide) ((DiveChartData) data).deepdiving$markDepthChart();
        return data;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (!stack.has(DataComponents.MAP_ID)) {
                ItemStack template =
                        MapItem.create(
                                level,
                                player.blockPosition().getX(),
                                player.blockPosition().getZ(),
                                (byte) 0,
                                true,
                                false);
                ((DiveChartData) MapItem.getSavedData(template, level)).deepdiving$markDepthChart();
                assignOneMap(player, stack, template.get(DataComponents.MAP_ID));
                player.getCooldowns().addCooldown(this, 10);
            } else if (player.isShiftKeyDown()) {
                MapItemSavedData data = MapItem.getSavedData(stack, level);
                if (data != null && data.scale < 4 && !data.locked) {
                    MapId id = level.getFreeMapId();
                    level.setMapData(id, data.scaled());
                    assignOneMap(player, stack, id);
                    player.getCooldowns().addCooldown(this, 10);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Using one stacked map must not initialize or zoom every copy in the hand. */
    private static void assignOneMap(Player player, ItemStack held, MapId id) {
        if (held.getCount() == 1) {
            held.set(DataComponents.MAP_ID, id);
            return;
        }
        ItemStack processed = held.copyWithCount(1);
        processed.set(DataComponents.MAP_ID, id);
        held.shrink(1);
        if (!player.getInventory().add(processed)) player.drop(processed, false);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        tickChart(
                stack,
                level,
                entity,
                selected || entity instanceof Player player && player.getOffhandItem() == stack);
    }

    /** Also used by a held bundle's selected page; inactive pages never spend survey budget. */
    public void tickChart(ItemStack stack, Level level, Entity entity, boolean held) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player)) return;
        MapItemSavedData data = MapItem.getSavedData(stack, level);
        if (data == null) return;
        data.tickCarriedBy(player, stack);
        if (!held || !surveyable(data, level)) return;

        MapId id = stack.get(DataComponents.MAP_ID);
        int heldMaps = heldMapCount(player, level);
        int pixels = claimBudget(player, level, id, ChartMath.pixelsPerMap(heldMaps));
        if (pixels == 0) return;

        int position = ChartMath.surveyStart(level.getGameTime(), heldMaps);
        int size = 1 << data.scale;
        int radius = Math.max(1, 72 / size);
        double playerX = (player.getX() - data.centerX) / size + 64;
        double playerZ = (player.getZ() - data.centerZ) / size + 64;

        for (int sample = 0; sample < pixels; sample++) {
            // The odd multiplier visits every pixel once and reveals scattered tiles progressively.
            int index = ((position + sample) * 73) & (ChartMath.PIXEL_COUNT - 1);
            int pixelX = index & 127;
            int pixelZ = index >> 7;
            if (squared(pixelX - playerX) + squared(pixelZ - playerZ) > radius * radius) continue;

            int worldX = data.centerX + (pixelX - 64) * size + size / 2;
            int worldZ = data.centerZ + (pixelZ - 64) * size + size / 2;
            int sounding = depth(server, worldX, worldZ);
            if (sounding < 0) continue;

            byte color =
                    ChartPalette.color(
                            sounding,
                            depth(server, worldX - size, worldZ),
                            depth(server, worldX, worldZ - size));
            int labelX = pixelX % 32 - 4;
            int labelZ = pixelZ % 32 - 4;
            if (labelX >= 0 && labelX < 16 && labelZ >= 0 && labelZ < 5 && sounding > 0) {
                int anchorX = data.centerX + (pixelX / 32 * 32 + 4 - 64) * size + size / 2;
                int anchorZ = data.centerZ + (pixelZ / 32 * 32 + 4 - 64) * size + size / 2;
                if (ChartMath.labelPixel(depth(server, anchorX, anchorZ), labelX, labelZ)) {
                    color = ChartPalette.label(sounding);
                }
            }
            data.updateColor(pixelX, pixelZ, color);
        }
    }

    private static boolean surveyable(MapItemSavedData data, Level level) {
        return data != null && !data.locked && data.dimension.equals(level.dimension());
    }

    private static int heldMapCount(Player player, Level level) {
        ItemStack main = MapBundleItem.resolveMap(player.getMainHandItem());
        ItemStack off = MapBundleItem.resolveMap(player.getOffhandItem());
        if (!(main.getItem() instanceof DepthMapItem)
                || !(off.getItem() instanceof DepthMapItem)
                || !surveyable(MapItem.getSavedData(main, level), level)
                || !surveyable(MapItem.getSavedData(off, level), level)) return 1;
        return main.get(DataComponents.MAP_ID).equals(off.get(DataComponents.MAP_ID)) ? 1 : 2;
    }

    /** Both hands share 64 samples, and duplicate map IDs are processed only once per tick. */
    private static int claimBudget(Player player, Level level, MapId id, int requested) {
        CompoundTag root = player.getPersistentData();
        CompoundTag budget = root.getCompound(SURVEY_BUDGET);
        String dimension = level.dimension().location().toString();
        if (!budget.contains("tick")
                || budget.getLong("tick") != level.getGameTime()
                || !budget.getString("dimension").equals(dimension)) {
            budget = new CompoundTag();
            budget.putLong("tick", level.getGameTime());
            budget.putString("dimension", dimension);
            budget.putInt("first", -1);
            budget.putInt("second", -1);
            root.put(SURVEY_BUDGET, budget);
        }
        if (budget.getInt("first") == id.id() || budget.getInt("second") == id.id()) return 0;
        int used = budget.getInt("used");
        int granted = Math.min(requested, ChartMath.SAMPLES_PER_TICK - used);
        if (granted <= 0) return 0;
        budget.putInt(used == 0 ? "first" : "second", id.id());
        budget.putInt("used", used + granted);
        return granted;
    }

    /** Two maintained height maps provide an O(1), loaded-chunk-only surface-water sounding. */
    public static int depth(ServerLevel level, int x, int z) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) return -1;
        int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
        int floor = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x & 15, z & 15);
        if (top < level.getMinBuildHeight()
                || !chunk.getFluidState(new BlockPos(x, top, z)).is(FluidTags.WATER)) return 0;
        return Math.max(0, top - floor);
    }

    private static double squared(double value) {
        return value * value;
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(
                Component.translatable(
                        stack.has(DataComponents.MAP_ID)
                                ? "tooltip.deepdiving.depth_map.use"
                                : "tooltip.deepdiving.depth_map.create"));
        lines.add(
                Component.translatable(
                        "tooltip.deepdiving.depth_map.contours", ChartMath.CONTOUR_INTERVAL));
        lines.add(Component.translatable("tooltip.deepdiving.depth_map.colors"));
        lines.add(Component.translatable("tooltip.deepdiving.depth_map.sites"));
        lines.add(Component.translatable("tooltip.deepdiving.depth_map.table"));
    }
}
