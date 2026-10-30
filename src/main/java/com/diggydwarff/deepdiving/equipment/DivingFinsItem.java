package com.diggydwarff.deepdiving.equipment;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import java.util.List;

public final class DivingFinsItem extends Item implements Equipable {
    public final DyeColor color;

    public DivingFinsItem(Properties properties) {
        this(properties, DyeColor.CYAN);
    }

    public DivingFinsItem(Properties properties, DyeColor color) {
        super(properties.stacksTo(1));
        this.color = color;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.FEET;
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
        return swapWithEquipmentSlot(this, level, player, hand);
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
