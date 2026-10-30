package com.diggydwarff.deepdiving.chart;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Normal sign placement with a short explanation of its map interaction. */
public final class DiveSiteSignItem extends SignItem {
    public DiveSiteSignItem(Properties properties, Block standing, Block wall) {
        super(properties, standing, wall);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("tooltip.deepdiving.site_sign"));
    }
}
