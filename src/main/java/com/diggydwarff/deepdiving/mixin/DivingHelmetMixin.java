package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.diving.DiverService;
import com.simibubi.create.content.equipment.armor.DivingHelmetItem;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    value = DivingHelmetItem.class,
    remap = false
)
public abstract class DivingHelmetMixin {
    @Inject(
        method = "breatheUnderwater",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void deepdiving$breathe(LivingBreatheEvent event, CallbackInfo ci) {
        if (event.getEntity() instanceof Player player && DiverService.manages(player)) {
            DiverService.breathe(event);
            ci.cancel();
        }
    }
}
