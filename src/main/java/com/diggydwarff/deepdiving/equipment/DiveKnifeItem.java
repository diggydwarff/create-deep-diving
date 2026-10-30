package com.diggydwarff.deepdiving.equipment;

import com.diggydwarff.deepdiving.DeepDiving;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;

public final class DiveKnifeItem extends SwordItem {
    private static final ThreadLocal<Boolean> RIGHT_CLICK_PERMISSION =
            ThreadLocal.withInitial(() -> false);
    private static final TagKey<EntityType<?>> AQUATIC =
            TagKey.create(Registries.ENTITY_TYPE, DeepDiving.id("aquatic"));

    public DiveKnifeItem(Properties properties) {
        super(Tiers.IRON, properties.attributes(SwordItem.createAttributes(Tiers.IRON, 1, -2.2f)));
    }

    private static boolean plant(BlockState state) {
        return state.is(Blocks.KELP)
                || state.is(Blocks.KELP_PLANT)
                || state.is(Blocks.SEAGRASS)
                || state.is(Blocks.TALL_SEAGRASS)
                || state.is(Blocks.VINE)
                || state.is(Blocks.COBWEB);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return plant(state) ? 15 : super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return plant(state) || super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility action) {
        return action == ItemAbilities.SHEARS_DIG || super.canPerformAction(stack, action);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null
                || !context.getPlayer().mayBuild()
                || !context.getPlayer().isEyeInFluid(FluidTags.WATER)
                || !grass(context.getLevel().getBlockState(context.getClickedPos()))) {
            return InteractionResult.PASS;
        }

        if (!context.getLevel().isClientSide) {
            if (!(context.getPlayer() instanceof ServerPlayer player)
                    || !context.getLevel().mayInteract(player, context.getClickedPos())) {
                return InteractionResult.FAIL;
            }
            // A right-click harvest must consult the same protection event as ordinary mining.
            // Skip our own mining callback during this permission-only event, then harvest once.
            RIGHT_CLICK_PERMISSION.set(true);
            try {
                var event =
                        net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(
                                context.getLevel(),
                                player.gameMode.getGameModeForPlayer(),
                                player,
                                context.getClickedPos(),
                                context.getLevel().getBlockState(context.getClickedPos()));
                if (event.isCanceled()) return InteractionResult.FAIL;
            } finally {
                RIGHT_CLICK_PERMISSION.remove();
            }
            shear(
                    context.getLevel(),
                    context.getClickedPos(),
                    context.getPlayer(),
                    context.getItemInHand(),
                    context.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                            ? EquipmentSlot.MAINHAND
                            : EquipmentSlot.OFFHAND);
        }

        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    private static boolean grass(BlockState state) {
        return state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS);
    }

    private static void shear(
            Level level, BlockPos pos, Player player, ItemStack tool, EquipmentSlot slot) {
        BlockState state = level.getBlockState(pos);

        int count = state.is(Blocks.TALL_SEAGRASS) ? 2 : 1;

        if (state.is(Blocks.TALL_SEAGRASS)) {
            BlockPos other =
                    state.getValue(TallSeagrassBlock.HALF)
                                    == net.minecraft.world.level.block.state.properties
                                            .DoubleBlockHalf.LOWER
                            ? pos.above()
                            : pos.below();

            if (level.getBlockState(other).is(Blocks.TALL_SEAGRASS)) {
                level.destroyBlock(other, false);
            }
        }

        level.destroyBlock(pos, false);

        if (!player.isCreative()) {
            Block.popResource(level, pos, new ItemStack(Blocks.SEAGRASS, count));
            tool.hurtAndBreak(1, player, slot);
        }
    }

    public static float bonus(LivingEntity target) {
        return target instanceof net.minecraft.world.entity.animal.WaterAnimal
                        || target.getType().is(AQUATIC)
                ? 3
                : 0;
    }

    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof Player player
                && player.getMainHandItem().is(DeepDiving.KNIFE.get())) {
            event.setAmount(event.getAmount() + bonus(event.getEntity()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void mine(BlockEvent.BreakEvent event) {
        if (RIGHT_CLICK_PERMISSION.get()) return;
        Player player = event.getPlayer();

        if (player.isEyeInFluid(FluidTags.WATER)
                && player.getMainHandItem().is(DeepDiving.KNIFE.get())
                && grass(event.getState())
                && event.getLevel() instanceof Level level) {
            shear(level, event.getPos(), player, player.getMainHandItem(), EquipmentSlot.MAINHAND);
            event.setCanceled(true);
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("+3 damage against aquatic mobs"));
        lines.add(Component.literal("Cuts guide lines; shears seagrass underwater"));
    }
}
