package com.diggydwarff.deepdiving.client.audio;

import com.diggydwarff.deepdiving.compat.DeepSeasCompatibility;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.presentation.SoundTransmission;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/** World queries stay on the client thread; only a filter amount is sent to the audio thread. */
public final class UnderwaterAcoustics {
    private static final Map<SoundInstance, SourceState> sources = new WeakHashMap<>();

    private static final class SourceState {
        float water;

        float barrier;

        SourceState(float water) {
            this.water = water;
        }
    }

    private static ClientLevel previousLevel;

    private static float listenerWater;

    private static Vec3 listener = Vec3.ZERO;

    public static boolean enabled() {
        var mc = Minecraft.getInstance();

        return mc.level != null && mc.player != null
            && DivingConfig.ENABLED.get() && DivingConfig.SOUND_FILTERING.get()
            && DivingConfig.FILTERING_STRENGTH.get() > 0;
    }

    public static void tick(boolean paused) {
        var mc = Minecraft.getInstance();

        if (mc.level != previousLevel || !enabled() || mc.player == null) {
            sources.clear();
            listenerWater = 0;
            previousLevel = mc.level;
        }

        if (mc.level == null || mc.player == null || !enabled() || paused) {
            return;
        }

        var camera = mc.getCameraEntity();

        listener = camera == null ? mc.player.getEyePosition() : camera.getEyePosition();

        boolean wet = waterAt(listener);

        if (camera instanceof Player player && DeepSeasCompatibility.inDryCompartment(player)) {
            wet = false;
        }

        listenerWater = SoundTransmission.approach(
            listenerWater,
            wet ? 1 : 0,
            DivingConfig.ACOUSTIC_TRANSITION.get()
        );
    }

    public static float amount(SoundInstance sound, boolean starting) {
        if (!enabled() || previousLevel == null || Minecraft.getInstance().player == null
            || sound.isRelative() || sound.getAttenuation() == SoundInstance.Attenuation.NONE
            || sound.getSource() == SoundSource.MUSIC
            || sound.getSource() == SoundSource.MASTER) {
            return 0;
        }

        Vec3 origin = new Vec3(sound.getX(), sound.getY(), sound.getZ());

        float target = sourceInWater(origin) ? 1 : 0;

        SourceState state = sources.computeIfAbsent(sound, ignored -> new SourceState(target));

        state.water = starting ? target : SoundTransmission.approach(
            state.water,
            target,
            DivingConfig.ACOUSTIC_TRANSITION.get()
        );

        float crossing = SoundTransmission.crossing(listenerWater, state.water);

        // Two air spaces can still be separated by water. Probe a bounded straight path,
        // without loading chunks or trying to simulate reflections through walls.
        float barrier = listenerWater == 0 && state.water == 0 && waterBetween(origin) ? 1 : 0;

        state.barrier = SoundTransmission.approach(state.barrier, barrier, DivingConfig.ACOUSTIC_TRANSITION.get());
        crossing = Math.max(crossing, state.barrier);

        return crossing * DivingConfig.FILTERING_STRENGTH.get().floatValue();
    }

    private static boolean waterAt(Vec3 point) {
        BlockPos position = BlockPos.containing(point);

        if (previousLevel == null || !previousLevel.hasChunkAt(position)) {
            return false;
        }

        var fluid = previousLevel.getFluidState(position);

        return fluid.is(FluidTags.WATER)
            && point.y < position.getY() + fluid.getHeight(previousLevel, position)
            && !DeepSeasCompatibility.inDryCompartment(previousLevel, point);
    }

    private static boolean sourceInWater(Vec3 origin) {
        if (waterAt(origin)) {
            return true;
        }

        BlockPos position = BlockPos.containing(origin);

        if (!previousLevel.hasChunkAt(position)
            || previousLevel.getBlockState(position).getCollisionShape(previousLevel, position).isEmpty()
            || DeepSeasCompatibility.inDryCompartment(previousLevel, origin)) {
            return false;
        }

        // A solid machine has no water inside its block. Its surrounding fluid describes
        // the sound's medium better than classifying every submerged machine as air.
        int wetFaces = 0;

        for (Direction direction : Direction.values()) {
            if (waterAt(Vec3.atCenterOf(position.relative(direction)))) {
                wetFaces++;
            }
        }

        return wetFaces >= 3;
    }

    private static boolean waterBetween(Vec3 origin) {
        double distance = listener.distanceTo(origin);

        int samples = Math.min(12, (int) distance);

        for (int index = 1; index < samples; index++) {
            if (waterAt(listener.lerp(origin, index / (double) samples))) {
                return true;
            }
        }

        return false;
    }

    private UnderwaterAcoustics() {}
}
