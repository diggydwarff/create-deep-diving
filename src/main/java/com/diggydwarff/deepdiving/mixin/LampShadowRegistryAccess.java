package com.diggydwarff.deepdiving.mixin;

import com.BreadRes.illuminationapi.client.LightShadowMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** The pinned API lacks per-light disposal; remove only Deep Diving's own keys. */
@Mixin(value = LightShadowMap.class, remap = false)
public interface LampShadowRegistryAccess {
    @Accessor("CACHE")
    static Map<Object, Object> deepdiving$shadowCache() {
        throw new AssertionError("Mixin not applied");
    }
}
