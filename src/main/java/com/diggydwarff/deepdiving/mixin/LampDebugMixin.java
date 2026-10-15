package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.client.lighting.LampRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Upstream 1.0 hard-enables verbose profiling; omit that work only during our shadow pass. */
@Mixin(targets = "com.BreadRes.illuminationapi.client.IlluminationDebug", remap = false)
abstract class LampDebugMixin {
    @Inject(method = {"stage", "shadowStage", "frameFinished", "glCheck", "info", "warn"},
        at = @At("HEAD"), cancellable = true)
    private static void deepdiving$quietLampProfiling(CallbackInfo callback) {
        if (LampRenderer.castingOwner() != null) {
            callback.cancel();
        }
    }
}
