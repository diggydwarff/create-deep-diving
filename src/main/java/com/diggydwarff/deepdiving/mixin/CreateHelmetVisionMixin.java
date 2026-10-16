package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.config.DivingConfig;
import com.simibubi.create.content.equipment.armor.DivingHelmetItem;
import com.simibubi.create.foundation.events.ClientEvents;

import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ViewportEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gate only Create's underwater helmet fog boost, preserving lava vision and other fog handlers.
 */
@Mixin(
    value = ClientEvents.class,
    remap = false
)
abstract class CreateHelmetVisionMixin {
    @Inject(
        method = "getFogDensity",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void deepdiving$helmetVision(ViewportEvent.RenderFog event, CallbackInfo ci) {
        if (DivingConfig.allowCreateHelmetVision()) {
            return;
        }

        var camera = event.getCamera();

        if (camera.getFluidInCamera() == FogType.WATER
            && !camera.getEntity().isSpectator()
            && DivingHelmetItem.isWornBy(camera.getEntity())) {
            ci.cancel();
        }
    }
}
