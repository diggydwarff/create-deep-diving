package com.diggydwarff.deepdiving.mixin.sodium;

import com.diggydwarff.deepdiving.client.lighting.LampVanillaUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gl.shader.GlProgram", remap = false)
public abstract class LampSodiumProgramMixin {
    @Inject(method = "bind", at = @At("TAIL"))
    private void deepdiving$materialUniforms(CallbackInfo callback) {
        LampVanillaUniforms.bindCurrent();
    }

    @Inject(method = "unbind", at = @At("HEAD"))
    private void deepdiving$restoreTextures(CallbackInfo callback) {
        LampVanillaUniforms.releaseTextures();
    }

    @Inject(method = "delete", at = @At("HEAD"))
    private void deepdiving$forgetProgram(CallbackInfo callback) {
        LampVanillaUniforms.forgetPrograms();
    }
}
