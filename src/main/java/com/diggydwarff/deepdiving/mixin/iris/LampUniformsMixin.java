package com.diggydwarff.deepdiving.mixin.iris;

import com.diggydwarff.deepdiving.client.lighting.LampIrisHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.irisshaders.iris.uniforms.CommonUniforms", remap = false)
abstract class LampUniformsMixin {
    // Register on each real program, including Sodium terrain. The shared
    // custom-input cache updates before AFTER_SKY and would use last frame's lamp.
    @Inject(method = "addDynamicUniforms", at = @At("TAIL"))
    private static void deepdiving$materialUniforms(@Coerce Object uniforms, @Coerce Object fog,
        CallbackInfo callback) {
        LampIrisHooks.uniforms(uniforms);
    }
}
