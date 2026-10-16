package com.diggydwarff.deepdiving.mixin.iris;

import com.diggydwarff.deepdiving.client.lighting.LampIrisHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cache graphics limits before Iris creates its parallel source-reading tasks. */
@Mixin(targets = "net.irisshaders.iris.Iris", remap = false)
abstract class LampPackLoadMixin {
    @Inject(method = "loadShaderpack", at = @At("HEAD"))
    private static void deepdiving$prepareMaterialParsing(CallbackInfo callback) {
        LampIrisHooks.beginPackLoad();
    }
}
