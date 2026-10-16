package com.diggydwarff.deepdiving.mixin.sodium;

import com.diggydwarff.deepdiving.client.lighting.LampVanillaShader;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader", remap = false)
public abstract class LampSodiumSourceMixin {
    @Inject(method = "getShaderSource", at = @At("RETURN"), cancellable = true)
    private static void deepdiving$materialSource(ResourceLocation name, CallbackInfoReturnable<String> callback) {
        if (name.getNamespace().equals("sodium")) {
            callback.setReturnValue(LampVanillaShader.sodium(name.getPath(), callback.getReturnValue()));
        }
    }
}
