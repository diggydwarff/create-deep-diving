package com.diggydwarff.deepdiving.physiology;

import com.diggydwarff.deepdiving.DeepDiving;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Named conditions are the real active effects; vanilla symptom queries are derived, never added.
 */
public final class DivingEffects {
    public static final DeferredRegister<MobEffect> REGISTRY =
        DeferredRegister.create(Registries.MOB_EFFECT, DeepDiving.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> HYPOXIA = register("hypoxia", 0x7FBADE);

    public static final DeferredHolder<MobEffect, MobEffect> NARCOSIS = register("narcosis", 0xADA6E3);

    public static final DeferredHolder<MobEffect, MobEffect> TOXICITY = register("oxygen_toxicity", 0xDAAE71);

    public static final DeferredHolder<MobEffect, MobEffect> DECOMPRESSION =
        register("decompression_sickness", 0xCC9564);

    public static final DeferredHolder<MobEffect, MobEffect> PRESSURE = register("pressure_injury", 0xB04B55);

    public static final DeferredHolder<MobEffect, MobEffect> BLACKOUT = register("blackout", 0x707782);

    public static final List<Holder<MobEffect>> ALL =
        List.of(HYPOXIA, NARCOSIS, TOXICITY, DECOMPRESSION, PRESSURE, BLACKOUT);

    /** Installed only by the client; dedicated servers keep ordinary symptom queries. */
    public static java.util.function.Predicate<LivingEntity> customNauseaVisual = entity -> false;

    private static final ResourceLocation MOVEMENT = DeepDiving.id("diving_movement");

    private static final ResourceLocation ATTACK = DeepDiving.id("diving_weakness");

    private static DeferredHolder<MobEffect, MobEffect> register(String name, int color) {
        return REGISTRY.register(name, () -> new Status(color));
    }

    private static final class Status extends MobEffect {
        Status(int color) {
            super(MobEffectCategory.HARMFUL, color);
        }
    }

    public static void physiology(Player player, DiverState state) {
        set(
            player,
            HYPOXIA,
            state.oxygenation < 70
                    ? Math.clamp((int) ((70 - state.oxygenation) / 20), 0, 3)
                    : -1
        );
        set(player, NARCOSIS, conditionAmplifier(state.narcosis, .2, .8, 1.8));
        set(player, TOXICITY, conditionAmplifier(state.toxicity, 15, 30, 100));
        set(player, DECOMPRESSION, conditionAmplifier(state.decompressionStress, 10, 20, 50));
        set(player, BLACKOUT, state.blackedOut ? 0 : -1);
    }

    public static void pressure(Player player, double injury) {
        set(player, PRESSURE, conditionAmplifier(injury, 10, 30, 65));
    }

    /** Minecraft uses zero-based amplifiers; -1 means the condition should be removed. */
    private static int conditionAmplifier(
        double value,
        double mild,
        double moderate,
        double severe
    ) {
        // Require the entry threshold first so an invalid value (NaN) cannot create an effect.
        if (!(value > mild)) {
            return -1;
        }

        if (value > severe) {
            return 2;
        }

        if (value > moderate) {
            return 1;
        }

        return 0;
    }

    public static void clearPhysiology(Player player) {
        for (var effect : ALL) {
            if (!effect.equals(PRESSURE)) {
                player.removeEffect(effect);
            }
        }
    }

    public static void clear(Player player) {
        for (var effect : ALL) {
            player.removeEffect(effect);
        }

        updateAttributes(player);
    }

    private static void set(Player player, Holder<MobEffect> type, int amplifier) {
        MobEffectInstance current = player.getActiveEffectsMap().get(type);

        if (amplifier < 0) {
            if (current != null) {
                player.removeEffect(type);
            }

            return;
        }

        if (current != null
            && current.getAmplifier() == amplifier
            && current.isInfiniteDuration()) {
            return;
        }

        if (current != null) {
            // Recovery can lower severity as well as increase it.
            player.removeEffect(type);
        }

        player.addEffect(new MobEffectInstance(type, -1, amplifier, false, false, true));
    }

    private static int level(LivingEntity entity, Holder<MobEffect> effect) {
        var current = entity.getActiveEffectsMap().get(effect);

        return current == null ? -1 : current.getAmplifier();
    }

    /** Read only the actual map to avoid recursing through the symptom-query mixin. */
    public static MobEffectInstance symptom(LivingEntity entity, Holder<MobEffect> requested) {
        if (!(entity instanceof Player) || entity.getActiveEffectsMap().isEmpty()) {
            return null;
        }

        if (!requested.equals(MobEffects.MOVEMENT_SLOWDOWN)
            && !requested.equals(MobEffects.DIG_SLOWDOWN)
            && !requested.equals(MobEffects.CONFUSION)
            && !requested.equals(MobEffects.WEAKNESS)
            && !requested.equals(MobEffects.BLINDNESS)) {
            return null;
        }

        int hypoxiaLevel = level(entity, HYPOXIA);

        int narcosisLevel = level(entity, NARCOSIS);

        int toxicityLevel = level(entity, TOXICITY);

        int decompressionLevel = level(entity, DECOMPRESSION);

        int pressureLevel = level(entity, PRESSURE);

        int blackoutLevel = level(entity, BLACKOUT);

        int amplifier = -1;

        if (requested.equals(MobEffects.MOVEMENT_SLOWDOWN)) {
            amplifier = Math.max(hypoxiaLevel < 0 ? -1 : Math.min(2, hypoxiaLevel), narcosisLevel);

            if (decompressionLevel >= 0) {
                amplifier = Math.max(amplifier, 0);
            }

            if (pressureLevel >= 1) {
                amplifier = Math.max(amplifier, 1);
            }

            if (blackoutLevel >= 0) {
                amplifier = 4;
            }
        } else if (requested.equals(MobEffects.DIG_SLOWDOWN)) {
            if (hypoxiaLevel >= 0 || narcosisLevel >= 0 || decompressionLevel >= 0) {
                amplifier = 0;
            }
        } else if (requested.equals(MobEffects.CONFUSION)) {
            if ((narcosisLevel >= 1 || toxicityLevel >= 1 || decompressionLevel >= 1)
                && !customNauseaVisual.test(entity)) {
                amplifier = 0;
            }
        } else if (requested.equals(MobEffects.WEAKNESS)) {
            if (decompressionLevel >= 0) {
                amplifier = 0;
            }
        } else if (blackoutLevel >= 0) {
            amplifier = 0;
        }

        return amplifier < 0
                ? null
                : new MobEffectInstance(requested, -1, amplifier, false, false, false);
    }

    /**
     * Reproduce vanilla attribute penalties once, accounting for an independently applied potion.
     */
    public static void updateAttributes(Player player) {
        var slow = symptom(player, MobEffects.MOVEMENT_SLOWDOWN);

        var realSlow = player.getActiveEffectsMap().get(MobEffects.MOVEMENT_SLOWDOWN);

        double multiplier = 0;

        if (slow != null && (realSlow == null || slow.getAmplifier() > realSlow.getAmplifier())) {
            double existing = realSlow == null ? 1 : 1 - .15 * (realSlow.getAmplifier() + 1);

            if (existing > 0) {
                multiplier = (1 - .15 * (slow.getAmplifier() + 1)) / existing - 1;
            }
        }

        modifier(
            player,
            Attributes.MOVEMENT_SPEED,
            MOVEMENT,
            multiplier,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        var weak = symptom(player, MobEffects.WEAKNESS);

        var realWeak = player.getActiveEffectsMap().get(MobEffects.WEAKNESS);

        double attack = weak == null ? 0 : -4 * (weak.getAmplifier() + 1);

        if (realWeak != null) {
            attack = Math.min(0, attack + 4 * (realWeak.getAmplifier() + 1));
        }

        modifier(
            player,
            Attributes.ATTACK_DAMAGE,
            ATTACK,
            attack,
            AttributeModifier.Operation.ADD_VALUE
        );
    }

    private static void modifier(
        Player player,
        Holder<Attribute> type,
        ResourceLocation id,
        double amount,
        AttributeModifier.Operation operation
    ) {
        var attribute = player.getAttribute(type);

        if (attribute == null) {
            return;
        }

        var existing = attribute.getModifier(id);

        if (amount == 0) {
            if (existing != null) {
                attribute.removeModifier(id);
            }

            return;
        }

        if (existing != null && Math.abs(existing.amount() - amount) < 1e-9) {
            return;
        }

        attribute.removeModifier(id);
        attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
    }

    private DivingEffects() {}
}
