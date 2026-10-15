package com.diggydwarff.deepdiving.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.BreadRes.illuminationapi.client.LightShadowMap$ShadowCache", remap = false)
public interface LampShadowResourceAccess {
    @Accessor("fbo")
    int deepdiving$framebuffer();

    @Accessor("depthTexture")
    int deepdiving$depthTexture();
}
