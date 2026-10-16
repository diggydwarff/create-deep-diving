package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.physiology.DivingEffects;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla mining/fog/nausea behavior sees symptoms; the inventory sees only actual named
 * conditions.
 */
@Mixin(LivingEntity.class)
public abstract class DivingEffectMixin {
    @Inject(
        method = "hasEffect",
        at = @At("RETURN"),
        cancellable = true
    )
    private void deepdiving$hasSymptom(
        Holder<MobEffect> effect,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValue()
            && DivingEffects.symptom((LivingEntity) (Object) this, effect) != null) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method = "getEffect",
        at = @At("RETURN"),
        cancellable = true
    )
    private void deepdiving$getSymptom(
        Holder<MobEffect> effect,
        CallbackInfoReturnable<MobEffectInstance> cir
    ) {
        var symptom = DivingEffects.symptom((LivingEntity) (Object) this, effect);

        var actual = cir.getReturnValue();

        if (symptom != null && (actual == null || symptom.getAmplifier() > actual.getAmplifier())) {
            cir.setReturnValue(symptom);
        }
    }
}
