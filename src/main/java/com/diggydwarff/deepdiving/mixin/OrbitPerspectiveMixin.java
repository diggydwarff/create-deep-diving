package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.client.OrbitCamera;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
abstract class OrbitPerspectiveMixin {
    @ModifyExpressionValue(
        method = "handleKeybinds",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/CameraType;cycle()Lnet/minecraft/client/CameraType;"
        )
    )
    private CameraType deepdiving$perspective(CameraType ordinaryNext) {
        return OrbitCamera.nextPerspective(ordinaryNext);
    }
}
