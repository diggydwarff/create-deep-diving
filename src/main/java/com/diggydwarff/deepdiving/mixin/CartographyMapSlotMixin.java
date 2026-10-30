package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.chart.DiveSiteMarkers;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends the vanilla input slot without replacing its menu or result handling. */
@Mixin(targets = "net.minecraft.world.inventory.CartographyTableMenu$3")
public abstract class CartographyMapSlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void deepdiving$acceptChart(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (DiveSiteMarkers.isFilledDepthMap(stack)) callback.setReturnValue(true);
    }
}
