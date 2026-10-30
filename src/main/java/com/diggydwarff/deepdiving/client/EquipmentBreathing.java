package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.compat.DeepSeasCompatibility;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.equipment.BreathingEquipment;
import com.diggydwarff.deepdiving.network.BreathingPulse;
import com.diggydwarff.deepdiving.presentation.BreathingCycle;
import com.diggydwarff.deepdiving.presentation.ExhaleBubbles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One short, non-looping sound per phase. Bubbles use that same phase on each client. */
@EventBusSubscriber(modid = DeepDiving.MOD_ID, value = Dist.CLIENT)
public final class EquipmentBreathing {
    private static final double AUDIBLE_DISTANCE_SQUARED = 16 * 16;

    private static final Map<UUID, Breath> breaths = new HashMap<>();

    private static ClientLevel previousLevel;

    private static final class Breath {
        final BreathingPulse pulse;

        final BreathingSound sound;

        final int bubbleVariation;

        int age;

        Breath(BreathingPulse pulse, BreathingSound sound, int bubbleVariation) {
            this.pulse = pulse;
            this.sound = sound;
            this.bubbleVariation = bubbleVariation;
        }

        void finish() {
            if (sound != null) {
                sound.fadeOut();
            }
        }
    }

    private static void updateLevel() {
        ClientLevel level = Minecraft.getInstance().level;

        if (level != previousLevel) {
            for (Breath breath : breaths.values()) {
                if (breath.sound != null) {
                    Minecraft.getInstance().getSoundManager().stop(breath.sound);
                }
            }

            breaths.clear();
            previousLevel = level;
        }
    }

    public static void receive(BreathingPulse pulse) {
        updateLevel();

        Breath previous = breaths.remove(pulse.diverId());

        if (previous != null) {
            previous.finish();
        }

        if (previousLevel == null || pulse.phase() == BreathingCycle.STOPPED
            || pulse.ticks() < 1 || pulse.ticks() > 40
            || (pulse.profile() != BreathingPulse.REGULATOR && pulse.profile() != BreathingPulse.CREATE_HELMET)
            || (pulse.phase() != BreathingCycle.INHALE && pulse.phase() != BreathingCycle.EXHALE)) {
            return;
        }

        var entity = previousLevel.getEntity(pulse.entityId());

        if (!(entity instanceof Player diver) || !diver.getUUID().equals(pulse.diverId())
            || !visible(diver) || !submerged(diver)) {
            return;
        }

        BreathingSound sound = null;

        if (DivingConfig.EQUIPMENT_BREATHING.get() && DivingConfig.BREATHING_VOLUME.get() > 0) {
            sound = new BreathingSound(diver, pulse);
            Minecraft.getInstance().getSoundManager().play(sound);
        }

        breaths.put(pulse.diverId(), new Breath(pulse, sound, diver.getRandom().nextInt()));
    }

    private static boolean visible(Player diver) {
        var mc = Minecraft.getInstance();

        return mc.player != null && (diver == mc.player
            || (DivingConfig.NEARBY_DIVERS.get() && diver.distanceToSqr(mc.player) <= AUDIBLE_DISTANCE_SQUARED));
    }

    private static boolean submerged(Player diver) {
        if (!DivingConfig.ENABLED.get() || !diver.isAlive() || diver.isSpectator()
            || !diver.isEyeInFluid(FluidTags.WATER) || DeepSeasCompatibility.inDryCompartment(diver)) {
            return false;
        }

        // Remote inventory is private. Its authoritative supply state comes from the pulse.
        if (diver != Minecraft.getInstance().player) {
            return true;
        }

        if (DivingConfig.WATER_BREATHING_BYPASS.get()
            && (diver.hasEffect(MobEffects.WATER_BREATHING) || diver.hasEffect(MobEffects.CONDUIT_POWER))) {
            return false;
        }

        var supply = BreathingEquipment.select(diver);

        return supply != null && supply.available();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        updateLevel();

        if (previousLevel == null || Minecraft.getInstance().isPaused()) {
            return;
        }

        var iterator = breaths.values().iterator();

        while (iterator.hasNext()) {
            Breath breath = iterator.next();

            var entity = previousLevel.getEntity(breath.pulse.entityId());

            if (!(entity instanceof Player diver) || !diver.getUUID().equals(breath.pulse.diverId())
                || !visible(diver) || !submerged(diver) || breath.age >= breath.pulse.ticks()) {
                breath.finish();
                iterator.remove();
                continue;
            }

            if (breath.pulse.phase() == BreathingCycle.EXHALE && breath.pulse.bubbles()
                && DivingConfig.EXHAUST_BUBBLES.get()) {
                int count = ExhaleBubbles.count(breath.age, breath.pulse.ticks(), breath.bubbleVariation);
                if (count > 0) emitBubbles(diver, count, breath.age >= 6);
            }

            breath.age++;
        }
    }

    private static void emitBubbles(Player diver, int count, boolean trail) {
        Vec3 eye = diver.getEyePosition();

        Vec3 origin = eye.add(diver.getLookAngle().scale(.20)).add(0, -.10, 0);

        if (!previousLevel.getFluidState(BlockPos.containing(origin)).is(FluidTags.WATER)) {
            origin = eye.add(0, -.06, 0);
        }

        if (!previousLevel.getFluidState(BlockPos.containing(origin)).is(FluidTags.WATER)) {
            return;
        }

        var random = diver.getRandom();

        for (int index = 0; index < count; index++) {
            // A short mouth-directed puff opens the exhale, then drag lets each bubble rise.
            Vec3 puff = diver.getLookAngle().scale(trail ? 0 : .030 + random.nextDouble() * .020);
            previousLevel.addParticle(
                DeepDiving.DIVER_BUBBLE.get(),
                origin.x + random.nextGaussian() * .035,
                origin.y + random.nextGaussian() * .02,
                origin.z + random.nextGaussian() * .035,
                diver.getDeltaMovement().x * .15 + puff.x,
                trail ? .015 + random.nextDouble() * .012 : .025 + random.nextDouble() * .020,
                diver.getDeltaMovement().z * .15 + puff.z
            );
        }
    }

    private static final class BreathingSound extends AbstractTickableSoundInstance {
        private final Player diver;

        private final int duration;

        private int age;

        private int fadeTicks = -1;

        BreathingSound(Player diver, BreathingPulse pulse) {
            super(sound(pulse), SoundSource.PLAYERS, RandomSource.create());
            this.diver = diver;
            duration = pulse.ticks();
            relative = diver == Minecraft.getInstance().player;
            attenuation = relative ? Attenuation.NONE : Attenuation.LINEAR;
            volume = .001f;
            pitch = Math.clamp(20f / duration, .7f, 1.7f);
            updatePosition();
        }

        private static SoundEvent sound(BreathingPulse pulse) {
            String system = pulse.profile() == BreathingPulse.CREATE_HELMET ? "helmet" : "regulator";

            String phase = pulse.phase() == BreathingCycle.INHALE ? "inhale" : "exhale";

            return SoundEvent.createVariableRangeEvent(DeepDiving.id(system + "_" + phase));
        }

        void fadeOut() {
            if (fadeTicks < 0) {
                fadeTicks = 3;
            }
        }

        private void updatePosition() {
            if (!relative) {
                Vec3 eye = diver.getEyePosition();

                x = eye.x;
                y = eye.y;
                z = eye.z;
            }
        }

        @Override
        public void tick() {
            if (!visible(diver) || !submerged(diver) || !DivingConfig.EQUIPMENT_BREATHING.get()
                || diver.level() != Minecraft.getInstance().level || age >= duration) {
                fadeOut();
            }

            if (fadeTicks == 0 || diver.isRemoved()) {
                stop();
                return;
            }

            float envelope = Math.min(1, Math.min((age + 1) / 3f, (duration - age) / 3f));

            if (fadeTicks > 0) {
                envelope = Math.min(envelope, fadeTicks-- / 3f);
            }

            float nearby = relative ? 1 : DivingConfig.NEARBY_BREATHING_VOLUME.get().floatValue();

            volume = DivingConfig.BREATHING_VOLUME.get().floatValue() * nearby * Math.max(0, envelope);
            updatePosition();
            age++;
        }
    }

    private EquipmentBreathing() {}
}
