package com.diggydwarff.deepdiving.equipment;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Toggle a small local light while this lamp is carried. */
public final class DivingLampItem extends Item {
    public DivingLampItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
        Level level,
        Player player,
        InteractionHand hand
    ) {
        if (!level.isClientSide) {
            boolean enabled = !player.getPersistentData().getBoolean("deepdiving.lamp");

            player.getPersistentData().putBoolean("deepdiving.lamp", enabled);

            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                DivingLampService.update(serverPlayer);
            }

            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                    enabled ? "message.deepdiving.lamp_on" : "message.deepdiving.lamp_off"
                ),
                true
            );
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
