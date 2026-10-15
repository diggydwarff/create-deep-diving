package com.diggydwarff.deepdiving.client.ambience;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.client.ClientRegistration;
import com.diggydwarff.deepdiving.compat.DeepSeasCompatibility;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.presentation.AmbienceMath;
import com.diggydwarff.deepdiving.presentation.SoundTransmission;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

/** Local atmosphere only. No cloud simulation, gas changes, world edits or extra packets. */
@EventBusSubscriber(modid = DeepDiving.MOD_ID, value = Dist.CLIENT)
public final class DeepWaterAmbience {
    private static final RandomSource random = RandomSource.create();

    private static final List<MarineSnowParticle> particles = new ArrayList<>();

    private static ClientLevel previousLevel;

    private static Player previousPlayer;

    private static AmbienceDepth depthSampler = new AmbienceDepth();

    private static AmbientSound shallowLoop;

    private static AmbientSound deepLoop;

    private static AmbientSound distantCue;

    private static float immersion;

    private static float audioPresence;

    private static float deepBlend;

    private static float snowBlend;

    private static float warningDuck = 1;

    private static double spawnCredit;

    private static int cueDelay;

    private static int soundRetry;

    public static void reset() {
        var manager = Minecraft.getInstance().getSoundManager();

        for (AmbientSound sound : new AmbientSound[]{shallowLoop, deepLoop, distantCue}) {
            if (sound != null) {
                manager.stop(sound);
            }
        }

        particles.forEach(MarineSnowParticle::remove);
        particles.clear();
        shallowLoop = null;
        deepLoop = null;
        distantCue = null;
        depthSampler = new AmbienceDepth();
        immersion = 0;
        audioPresence = 0;
        deepBlend = 0;
        snowBlend = 0;
        warningDuck = 1;
        spawnCredit = 0;
        cueDelay = 0;
        soundRetry = 0;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();

        if (mc.level != previousLevel || mc.player != previousPlayer) {
            reset();
            previousLevel = mc.level;
            previousPlayer = mc.player;
        }

        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }

        boolean wet = DivingConfig.ENABLED.get() && mc.player.isAlive() && !mc.player.isSpectator()
            && mc.player.isEyeInFluid(FluidTags.WATER) && !DeepSeasCompatibility.inDryCompartment(mc.player);

        double depth = wet ? depthSampler.sample(mc.player) : 0;

        double transition = DivingConfig.AMBIENCE_TRANSITION.get();

        immersion = SoundTransmission.approach(immersion, wet ? 1 : 0, transition);
        audioPresence = SoundTransmission.approach(audioPresence,
            wet && DivingConfig.DEEP_WATER_AMBIENCE.get() && DivingConfig.AMBIENT_VOLUME.get() > 0 ? 1 : 0, transition);

        // Keep the current depth mix during the exit fade, avoiding a last-second shallow-water swell.
        if (wet) {
            deepBlend = SoundTransmission.approach(deepBlend, AmbienceMath.depthBlend(depth,
                DivingConfig.SHALLOW_AMBIENT_DEPTH.get(), DivingConfig.DEEP_AMBIENT_DEPTH.get()), transition);
            snowBlend = SoundTransmission.approach(snowBlend, AmbienceMath.depthBlend(depth,
                DivingConfig.MARINE_SNOW_DEPTH.get(), DivingConfig.DEEP_AMBIENT_DEPTH.get()), transition);
        }

        var snapshot = ClientRegistration.snapshot;

        warningDuck = SoundTransmission.approach(warningDuck,
            snapshot != null && snapshot.warnings() != 0 ? .35f : 1, .4);

        updateSounds(mc, wet);
        updateParticles(mc, wet);
    }

    static float soundGain(int layer) {
        float mix = switch (layer) {
            case 0 -> Math.max(.001f, 1 - deepBlend) * .65f;
            case 1 -> Math.max(.001f, deepBlend) * .55f;
            default -> .45f;
        };

        return audioPresence * mix * warningDuck;
    }

    static float particleVisibility(boolean luminous) {
        if (previousLevel == null) {
            return 0;
        }

        if (luminous) {
            return DivingConfig.BIOLUMINESCENCE.get() ? immersion * deepBlend : 0;
        }

        return DivingConfig.MARINE_SNOW.get() && DivingConfig.MARINE_SNOW_DENSITY.get() > 0
            ? immersion * snowBlend : 0;
    }

    private static void updateSounds(Minecraft mc, boolean wet) {
        var manager = mc.getSoundManager();

        if (audioPresence <= 0 || mc.options.getSoundSourceVolume(SoundSource.AMBIENT) <= 0) {
            stopSounds();
            cueDelay = 0;
            return;
        }

        if (soundRetry-- <= 0) {
            if (shallowLoop == null || shallowLoop.isStopped() || !manager.isActive(shallowLoop)) {
                shallowLoop = new AmbientSound(mc.level, "shallow_water_wash", 0, Vec3.ZERO);
                manager.play(shallowLoop);
            }

            if (deepLoop == null || deepLoop.isStopped() || !manager.isActive(deepLoop)) {
                deepLoop = new AmbientSound(mc.level, "deep_water_wash", 1, Vec3.ZERO);
                manager.play(deepLoop);
            }

            soundRetry = 40;
        }

        if (!wet || DivingConfig.AMBIENT_FREQUENCY.get() <= 0) {
            cueDelay = 0;
            return;
        }

        if (distantCue != null && manager.isActive(distantCue)) {
            return;
        }

        if (cueDelay <= 0) {
            cueDelay = AmbienceMath.cueDelay(deepBlend > .5f, DivingConfig.AMBIENT_FREQUENCY.get(), random.nextDouble());
            return;
        }

        if (--cueDelay == 0) {
            playDistantCue(mc);
        }
    }

    private static void stopSounds() {
        var manager = Minecraft.getInstance().getSoundManager();

        for (AmbientSound sound : new AmbientSound[]{shallowLoop, deepLoop, distantCue}) {
            if (sound != null) {
                manager.stop(sound);
            }
        }

        shallowLoop = null;
        deepLoop = null;
        distantCue = null;
    }

    private static void playDistantCue(Minecraft mc) {
        boolean deep = deepBlend > .5f;

        if (deep && !ocean(mc)) {
            return;
        }

        Vec3 eye = mc.player.getEyePosition();

        double angle = random.nextDouble() * Math.PI * 2;

        Vec3 origin = eye.add(Math.cos(angle) * 18, deep ? -3 : 0, Math.sin(angle) * 18);

        if (!AmbienceWater.clearWater(mc.level, origin) || !AmbienceWater.connected(mc.level, mc.player, origin)) {
            return;
        }

        distantCue = new AmbientSound(mc.level, deep ? "distant_ocean" : "surface_activity", 2, origin);
        mc.getSoundManager().play(distantCue);
    }

    private static boolean ocean(Minecraft mc) {
        var biome = mc.level.getBiome(BlockPos.containing(mc.player.getEyePosition()));

        return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN);
    }

    private static void updateParticles(Minecraft mc, boolean wet) {
        particles.removeIf(particle -> !particle.isAlive());

        int budget = DivingConfig.AMBIENCE_PARTICLE_LIMIT.get();

        ParticleStatus setting = mc.options.particles().get();

        if (setting == ParticleStatus.MINIMAL) {
            budget /= 4;
        } else if (setting == ParticleStatus.DECREASED) {
            budget /= 2;
        }

        while (particles.size() > budget) {
            particles.removeLast().remove();
        }

        if (!wet || budget == 0) {
            spawnCredit = 0;
            return;
        }

        // Both kinds share the same hard budget; a luminous speck never reserves extra slots.
        if (DivingConfig.BIOLUMINESCENCE.get() && deepBlend > .1f && ocean(mc)
            && particles.size() < budget && random.nextFloat() < deepBlend / 500
            && particles.stream().filter(MarineSnowParticle::luminous).count() < 2) {
            spawnParticle(mc, true);
        }

        int target = DivingConfig.MARINE_SNOW.get()
            ? AmbienceMath.particleTarget(budget, DivingConfig.MARINE_SNOW_DENSITY.get(), snowBlend) : 0;

        // Up to two snow attempts each tick, including failed positions. Density never raises this limit.
        spawnCredit = Math.min(2, spawnCredit + target / 180.0);

        int attempts = Math.min(2, (int) spawnCredit);

        for (int index = 0; index < attempts && particles.size() < Math.min(target, budget); index++) {
            spawnCredit--;
            spawnParticle(mc, false);
        }
    }

    private static void spawnParticle(Minecraft mc, boolean luminous) {
        Vec3 eye = mc.player.getEyePosition();

        Vec3 origin = eye.add((random.nextDouble() - .5) * 16,
            (random.nextDouble() - .5) * 8, (random.nextDouble() - .5) * 16);

        if (eye.distanceToSqr(origin) < 1 || !AmbienceWater.clearWater(mc.level, origin)
            || !AmbienceWater.connected(mc.level, mc.player, origin)) {
            return;
        }

        var texture = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);

        if (!(texture instanceof TextureAtlas atlas)) {
            return;
        }

        MarineSnowParticle particle = new MarineSnowParticle(mc.level, origin, atlas, luminous);

        particles.add(particle);
        mc.particleEngine.add(particle);
    }

    private DeepWaterAmbience() {}
}
