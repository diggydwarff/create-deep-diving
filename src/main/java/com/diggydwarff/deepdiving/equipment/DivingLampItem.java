package com.diggydwarff.deepdiving.equipment;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Toggle a small local light while this lamp is carried. */
public final class DivingLampItem extends Item {
    public final LampProfile profile;

    public DivingLampItem(Properties properties) {
        this(properties, LampProfile.DIVING);
    }

    public DivingLampItem(Properties properties, LampProfile profile) {
        super(properties.stacksTo(1));
        this.profile = profile;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
        java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.deepdiving.lamp." + profile.name().toLowerCase(java.util.Locale.ROOT)));
        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.deepdiving.lamp.toggle"));
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
