package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.client.lighting.LampVanillaUniforms;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderInstance.class)
public abstract class LampCoreUniformsMixin {
    @Shadow
    public abstract int getId();

    @Inject(method = "apply", at = @At("TAIL"))
    private void deepdiving$materialUniforms(CallbackInfo callback) {
        LampVanillaUniforms.bind(getId());
    }

    @Inject(method = "clear", at = @At("TAIL"))
    private void deepdiving$restoreTextures(CallbackInfo callback) {
        LampVanillaUniforms.releaseTextures();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void deepdiving$forgetProgram(CallbackInfo callback) {
        LampVanillaUniforms.forgetPrograms();
    }
}
