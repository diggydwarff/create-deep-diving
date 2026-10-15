package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.config.LampConfig;
import com.diggydwarff.deepdiving.client.lighting.LampRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Do not let a diver's own head/body occlude a lamp originating on their equipment. */
@Mixin(targets = "com.BreadRes.illuminationapi.client.EntityShadowCaster", remap = false)
abstract class LampEntityShadowMixin {
    @Inject(method = "emitEntity", at = @At("HEAD"), cancellable = true)
    private static void deepdiving$excludeEmitter(EntityRenderDispatcher dispatcher, Entity entity,
        Vec3 origin, float partialTick, @Coerce Object geometry, @Coerce Object statistics,
        CallbackInfoReturnable<Boolean> callback) {
        Entity owner = LampRenderer.castingOwner();

        if (owner != null && (entity == owner || !LampConfig.ENTITY_SHADOWS.get())) {
            callback.setReturnValue(false);
        }
    }
}
