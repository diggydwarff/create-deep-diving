package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.chart.DiveSiteMarkers;

import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Frame map limits apply equally to depth charts; NeoForge already renders custom MapItems. */
@Mixin(ItemFrame.class)
public abstract class DepthMapFrameMixin {
    @Redirect(
            method = "interact",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean deepdiving$frameChart(ItemStack stack, Item item) {
        return stack.is(item)
                || item == Items.FILLED_MAP && DiveSiteMarkers.isFilledDepthMap(stack);
    }
}
