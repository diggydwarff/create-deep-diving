package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.client.DivingPostProcess;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Process the completed world after vanilla post effects, before any HUD or screen is drawn. */
@Mixin(GameRenderer.class)
abstract class DivingPostProcessMixin {
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;bindWrite(Z)V",
            shift = At.Shift.AFTER
        )
    )
    private void deepdiving$worldVision(DeltaTracker delta, boolean renderWorld, CallbackInfo ci) {
        DivingPostProcess.render(delta);
    }
}
