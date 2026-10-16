package com.diggydwarff.deepdiving.equipment;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A mask and attached snorkel, worn without a tank or regulator. */
public final class SnorkelMaskItem extends Item implements Equipable {
    public SnorkelMaskItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Vanilla's swap also respects occupied slots and binding enchantments.
        return swapWithEquipmentSlot(this, level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.deepdiving.snorkeling_mask.wear"));
        lines.add(Component.translatable("tooltip.deepdiving.snorkeling_mask.breathing"));
        lines.add(Component.translatable("tooltip.deepdiving.snorkeling_mask.limits"));
    }
}
