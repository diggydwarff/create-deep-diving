package com.diggydwarff.deepdiving.sign;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class DivePanelItem extends BlockItem {
    public DivePanelItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("tooltip.deepdiving.dive_sign.place"));
        lines.add(Component.translatable("tooltip.deepdiving.dive_sign.edit"));
        lines.add(Component.translatable("tooltip.deepdiving.dive_sign.ink"));
    }
}
