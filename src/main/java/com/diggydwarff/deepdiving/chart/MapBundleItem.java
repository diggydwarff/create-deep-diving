package com.diggydwarff.deepdiving.chart;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ComplexItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;

import java.util.List;

/** Real map stacks in vanilla container components, not copied map pixels or map IDs alone. */
public final class MapBundleItem extends ComplexItem {
    public static final int CAPACITY = 16;
    private static final String PAGE = "deepdiving_page";

    public MapBundleItem(Properties properties) {
        super(
                properties
                        .stacksTo(1)
                        .component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
    }

    public static boolean accepts(ItemStack stack) {
        return stack.getItem() instanceof DepthMapItem
                && stack.has(DataComponents.MAP_ID)
                && stack.getCount() == 1;
    }

    public static NonNullList<ItemStack> contents(ItemStack bundle) {
        var items = NonNullList.withSize(CAPACITY, ItemStack.EMPTY);
        bundle.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    public static int page(ItemStack bundle) {
        return Math.clamp(
                bundle.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag()
                        .getInt(PAGE),
                0,
                CAPACITY - 1);
    }

    public static void setPage(ItemStack bundle, int page) {
        var tag = bundle.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(PAGE, Math.clamp(page, 0, CAPACITY - 1));
        bundle.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static ItemStack resolveMap(ItemStack stack) {
        if (!(stack.getItem() instanceof MapBundleItem)) return stack;
        var items = contents(stack);
        ItemStack selected = items.get(page(stack));
        if (accepts(selected)) return selected;
        for (var item : items) if (accepts(item)) return item;
        return ItemStack.EMPTY;
    }

    public static boolean containsMap(ItemStack stack, MapId id) {
        if (id == null || !(stack.getItem() instanceof MapBundleItem)) return false;
        return stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyStream()
                .limit(CAPACITY)
                .anyMatch(map -> accepts(map) && id.equals(map.get(DataComponents.MAP_ID)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack bundle = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inventory, owner) -> new MapBundleMenu(id, inventory, slot),
                            bundle.getHoverName()),
                    buffer -> buffer.writeVarInt(slot));
        }
        return InteractionResultHolder.sidedSuccess(bundle, level.isClientSide);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        ItemStack map = resolveMap(stack);
        if (map.getItem() instanceof DepthMapItem chart) {
            chart.tickChart(
                    map,
                    level,
                    entity,
                    selected
                            || entity instanceof Player player && player.getOffhandItem() == stack);
        }
    }

    @Override
    public Packet<?> getUpdatePacket(ItemStack stack, Level level, Player player) {
        ItemStack map = resolveMap(stack);
        return map.getItem() instanceof DepthMapItem chart
                ? chart.getUpdatePacket(map, level, player)
                : null;
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        long count = contents(stack).stream().filter(MapBundleItem::accepts).count();
        lines.add(Component.translatable("tooltip.deepdiving.map_bundle.count", count, CAPACITY));
        lines.add(Component.translatable("tooltip.deepdiving.map_bundle.use"));
        lines.add(Component.translatable("tooltip.deepdiving.map_bundle.survey"));
    }
}
