package com.diggydwarff.deepdiving.mixin.iris;

import com.diggydwarff.deepdiving.client.lighting.LampIrisHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(targets = "net.irisshaders.iris.pipeline.IrisRenderingPipeline", remap = false)
abstract class LampSamplersMixin {
    @Inject(method = "addGbufferOrShadowSamplers", at = @At("TAIL"))
    private void deepdiving$materialSamplers(@Coerce Object samplers, @Coerce Object images,
        Supplier<?> flipped, boolean shadow, boolean normals, boolean specular, boolean geometry,
        CallbackInfo callback) {
        LampIrisHooks.samplers(samplers);
    }
}
