package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.sign.DivePanelItem;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.StonecutterMenu;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla caches by item type; swapping same-type panels must refresh caption/ink components. */
@Mixin(StonecutterMenu.class)
public abstract class DiveSignStonecutterMixin {
    @Shadow @Final public Container container;

    @Shadow
    abstract void setupResultSlot();

    @Inject(method = "slotsChanged", at = @At("RETURN"))
    private void deepdiving$refreshPanelComponents(Container changed, CallbackInfo callback) {
        if (container.getItem(0).getItem() instanceof DivePanelItem) setupResultSlot();
    }
}
