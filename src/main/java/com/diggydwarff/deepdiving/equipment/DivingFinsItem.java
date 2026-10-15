package com.diggydwarff.deepdiving.equipment;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import java.util.List;

public final class DivingFinsItem extends Item {
    public DivingFinsItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.FEET;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
        Level level,
        Player player,
        InteractionHand hand
    ) {
        if (!level.isClientSide) {
            ItemStack old = player.getItemBySlot(EquipmentSlot.FEET);

            player.setItemSlot(EquipmentSlot.FEET, player.getItemInHand(hand).copy());
            player.setItemInHand(hand, old);
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        List<Component> lines,
        TooltipFlag flag
    ) {
        lines.add(Component.literal("Use: wear | Slight swim boost; awkward walking"));
        lines.add(Component.literal("Stop swimming: float gently and keep the swimming pose"));
        lines.add(Component.literal("Space: rise and release the pose | Sneak: descend"));
    }
}
