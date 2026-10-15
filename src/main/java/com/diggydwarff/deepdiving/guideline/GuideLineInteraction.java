package com.diggydwarff.deepdiving.guideline;

import com.diggydwarff.deepdiving.DeepDiving;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class GuideLineInteraction {
    private static boolean interact(PlayerInteractEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.mayBuild()) {
            return false;
        }

        var stack = event.getItemStack();

        boolean glow = stack.is(Items.GLOW_INK_SAC);

        boolean cut = stack.is(DeepDiving.KNIFE.get()) || stack.is(Items.SHEARS);

        if (!glow && !cut) {
            return false;
        }

        var graph = GuideNetwork.get(player.serverLevel());

        GuideLine line = graph.trace(player);

        if (line == null) {
            return false;
        }

        if (glow) {
            if (graph.glow(player.serverLevel(), line.id()) && !player.isCreative()) {
                stack.shrink(1);
            }
        } else {
            GuideLine removed = graph.remove(player.serverLevel(), line.id());

            if (removed != null) {
                Block.popResource(player.level(), player.blockPosition(), GuideLineItem.refund(removed));

                if (!player.isCreative()) {
                    stack.hurtAndBreak(
                        1,
                        player,
                        event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                                ? EquipmentSlot.MAINHAND
                                : EquipmentSlot.OFFHAND
                    );
                }
            }
        }

        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void block(PlayerInteractEvent.RightClickBlock event) {
        if (interact(event)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void air(PlayerInteractEvent.RightClickItem event) {
        if (interact(event)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private GuideLineInteraction() {}
}
