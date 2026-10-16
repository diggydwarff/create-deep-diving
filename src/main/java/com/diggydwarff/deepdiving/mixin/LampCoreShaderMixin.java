package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.client.lighting.LampVanillaShader;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.Program;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.InputStream;

/** Patches only recognized world shaders, before Mojang resolves their imports. */
@Mixin(Program.class)
public abstract class LampCoreShaderMixin {
    @ModifyVariable(method = "compileShaderInternal", at = @At("STORE"), ordinal = 2)
    private static String deepdiving$materialSource(String source, Program.Type type, String name,
        InputStream input, String pack, GlslPreprocessor preprocessor) {
        return LampVanillaShader.vanilla(name, type == Program.Type.VERTEX, source);
    }
}
