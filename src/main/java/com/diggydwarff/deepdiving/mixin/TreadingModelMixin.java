package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.client.TreadingAnimation;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Run before PlayerModel copies limbs into its sleeves and pants, keeping all layers aligned. */
@Mixin(HumanoidModel.class)
abstract class TreadingModelMixin {
    @Inject(
        method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
        at = @At("TAIL")
    )
    private void deepdiving$tread(
        LivingEntity entity,
        float limb,
        float amount,
        float age,
        float yaw,
        float pitch,
        CallbackInfo ci
    ) {
        if (entity instanceof AbstractClientPlayer player) {
            TreadingAnimation.apply((HumanoidModel<?>) (Object) this, player, age);
        }
    }
}
