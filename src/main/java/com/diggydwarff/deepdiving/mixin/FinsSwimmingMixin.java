package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.diving.FinsService;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class FinsSwimmingMixin {
    @Inject(
        method = "updateSwimming",
        at = @At("TAIL")
    )
    private void deepdiving$keepSwimmingPose(CallbackInfo callback) {
        if ((Object) this instanceof Player player) {
            Boolean swimming = FinsService.swimmingOverride(player);

            if (swimming != null) {
                player.setSwimming(swimming);
            }
        }
    }
}
