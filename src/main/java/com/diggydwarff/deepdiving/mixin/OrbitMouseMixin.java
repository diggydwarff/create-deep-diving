package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.client.OrbitCamera;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
abstract class OrbitMouseMixin {
    @WrapOperation(
        method = "turnPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
        )
    )
    private void deepdiving$orbitMouse(
        LocalPlayer player,
        double horizontal,
        double vertical,
        Operation<Void> ordinaryTurn
    ) {
        if (!OrbitCamera.turn(horizontal, vertical)) {
            ordinaryTurn.call(player, horizontal, vertical);
        }
    }
}
