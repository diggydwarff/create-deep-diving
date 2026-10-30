package com.diggydwarff.deepdiving.equipment;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import java.util.List;

/** Lightweight swimwear occupies the leg slot, without granting pressure or armor protection. */
public final class SwimsuitTrousersItem extends Item implements Equipable {
    public final DyeColor color;

    public SwimsuitTrousersItem(Properties properties, DyeColor color) {
        super(properties.stacksTo(1));
        this.color = color;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.LEGS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.deepdiving.swimsuit_trousers"));
    }
}
