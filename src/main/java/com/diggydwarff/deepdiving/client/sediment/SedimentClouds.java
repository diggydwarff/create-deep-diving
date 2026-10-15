package com.diggydwarff.deepdiving.client.sediment;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.client.UnderwaterParticleBudget;
import com.diggydwarff.deepdiving.client.ambience.AmbienceWater;
import com.diggydwarff.deepdiving.compat.DeepSeasCompatibility;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.network.SedimentPulse;
import com.diggydwarff.deepdiving.sediment.SedimentCloud;
import com.diggydwarff.deepdiving.sediment.SedimentMath;
import com.diggydwarff.deepdiving.sediment.SedimentRules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

/** Particle plumes only. This feature does not modify biome clarity, fog or shader programs. */
@EventBusSubscriber(modid = DeepDiving.MOD_ID, value = Dist.CLIENT)
public final class SedimentClouds {
    private static final List<SedimentCloud> clouds = new ArrayList<>();

    private static final List<SedimentParticle> particles = new ArrayList<>();

    private static ClientLevel previousLevel;

    private static int cloudCursor;

    public static boolean enabled() {
        return Minecraft.getInstance().level != null && DivingConfig.CLIENT.isLoaded()
            && SedimentRules.enabled() && DivingConfig.SEDIMENT_PARTICLES.get();
    }

    private static void updateLevel() {
        ClientLevel current = Minecraft.getInstance().level;

        if (current != previousLevel) {
            reset();
            previousLevel = current;
        }
    }

    public static void receive(SedimentPulse pulse) {
        updateLevel();

        var mc = Minecraft.getInstance();

        if (!enabled() || mc.player == null || !pulse.dimension().equals(mc.level.dimension().location())
            || !SedimentRules.water(mc.level, pulse.position())) {
            return;
        }

        if (pulse.movement() ? !DivingConfig.SEDIMENT_MOVEMENT.get() : !DivingConfig.SEDIMENT_MINING.get()) {
            return;
        }

        Vec3 center = pulse.position().getCenter();

        double range = DivingConfig.SEDIMENT_RANGE.get();

        if (center.distanceToSqr(mc.player.getEyePosition()) > range * range) {
            return;
        }

        double radius = DivingConfig.SEDIMENT_RADIUS.get() * (pulse.movement() ? .6 : 1);

        double lifetime = DivingConfig.SEDIMENT_LIFETIME.get() * (pulse.movement() ? .7 : 1);

        double density = (pulse.movement() ? DivingConfig.SEDIMENT_MOVEMENT_DENSITY.get()
            : DivingConfig.SEDIMENT_MINING_DENSITY.get()) * pulse.intensity();

        if (density <= 0) {
            return;
        }

        while (clouds.size() >= DivingConfig.SEDIMENT_CLOUD_LIMIT.get()) {
            clouds.removeFirst();
        }

        clouds.add(new SedimentCloud(center, radius, lifetime, density));
    }

    public static boolean viewingWater() {
        var mc = Minecraft.getInstance();

        return enabled() && mc.player != null && mc.player.isAlive() && !mc.player.isSpectator()
            && mc.gameRenderer.getMainCamera().getFluidInCamera() == FogType.WATER
            && !DeepSeasCompatibility.inDryCompartment(mc.player)
            && AmbienceWater.contains(mc.level, mc.gameRenderer.getMainCamera().getPosition());
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        updateLevel();

        var mc = Minecraft.getInstance();

        if (!enabled() || mc.player == null) {
            reset();
            return;
        }

        if (mc.isPaused()) {
            return;
        }

        Vec3 eye = mc.player.getEyePosition();

        double range = DivingConfig.SEDIMENT_RANGE.get();

        clouds.forEach(SedimentCloud::tick);
        clouds.removeIf(cloud -> cloud.expired() || cloud.center().distanceToSqr(eye) > range * range);

        while (clouds.size() > DivingConfig.SEDIMENT_CLOUD_LIMIT.get()) {
            clouds.removeFirst();
        }

        particles.removeIf(particle -> !particle.isAlive());

        int limit = UnderwaterParticleBudget.scaledLimit(DivingConfig.SEDIMENT_PARTICLE_LIMIT.get());

        while (particles.size() > limit) {
            particles.removeFirst().remove();
        }

        if (!viewingWater()) {
            // Plumes may finish settling outside the pocket, but no particles render inside it.
            particles.forEach(SedimentParticle::remove);
            particles.clear();
            return;
        }

        int attempts = Math.min(DivingConfig.SEDIMENT_SPAWN_BUDGET.get(),
            Math.min(limit - particles.size(), UnderwaterParticleBudget.remaining()));

        for (int index = 0; index < attempts && !clouds.isEmpty(); index++) {
            SedimentCloud cloud = clouds.get(Math.floorMod(cloudCursor++, clouds.size()));

            spawn(mc, cloud);
        }
    }

    public static double densityAt(Vec3 position) {
        double total = 0;

        for (SedimentCloud cloud : clouds) {
            total += cloud.densityAt(position);
        }

        return SedimentMath.cappedDensity(total, DivingConfig.SEDIMENT_MAXIMUM_DENSITY.get());
    }

    private static void spawn(Minecraft mc, SedimentCloud cloud) {
        var random = mc.level.random;

        double radius = cloud.radius() * .65;

        Vec3 origin = cloud.center().add((random.nextDouble() * 2 - 1) * radius,
            (random.nextDouble() * 2 - 1) * radius, (random.nextDouble() * 2 - 1) * radius);

        // Low-density bottom walking changes particle count rather than pushing every
        // wisp below vanilla's alpha cutoff, where a small puff would become invisible.
        double chance = SedimentMath.spawnChance(densityAt(origin));

        if (random.nextDouble() >= chance || !AmbienceWater.clearWater(mc.level, origin)
            || !AmbienceWater.connected(mc.level, mc.player, origin)) {
            return;
        }

        var texture = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);

        if (texture instanceof TextureAtlas atlas) {
            SedimentParticle particle = new SedimentParticle(mc.level, origin, atlas,
                DivingConfig.SEDIMENT_COLOR.get());

            if (UnderwaterParticleBudget.add(particle)) {
                particles.add(particle);
            }
        }
    }

    public static void reset() {
        particles.forEach(SedimentParticle::remove);
        particles.clear();
        clouds.clear();
        cloudCursor = 0;
    }

    private SedimentClouds() {}
}
