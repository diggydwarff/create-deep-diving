package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.config.DivingConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

/** One admission point for sediment, marine snow and luminous specks. Client thread only. */
@EventBusSubscriber(modid = DeepDiving.MOD_ID, value = Dist.CLIENT)
public final class UnderwaterParticleBudget {
    private static final List<Particle> particles = new ArrayList<>();

    private static ClientLevel previousLevel;

    public static int scaledLimit(int configured) {
        ParticleStatus setting = Minecraft.getInstance().options.particles().get();

        return switch (setting) {
            case DECREASED -> configured / 2;
            case MINIMAL -> configured / 4;
            default -> configured;
        };
    }

    private static void prune() {
        var mc = Minecraft.getInstance();

        if (mc.level != previousLevel) {
            reset();
            previousLevel = mc.level;
        }

        particles.removeIf(particle -> !particle.isAlive());

        int limit = mc.level == null ? 0 : scaledLimit(DivingConfig.UNDERWATER_PARTICLE_LIMIT.get());

        while (particles.size() > limit) {
            particles.removeFirst().remove();
        }
    }

    public static boolean add(Particle particle) {
        prune();

        if (Minecraft.getInstance().level == null
            || particles.size() >= scaledLimit(DivingConfig.UNDERWATER_PARTICLE_LIMIT.get())) {
            particle.remove();
            return false;
        }

        particles.add(particle);
        Minecraft.getInstance().particleEngine.add(particle);
        return true;
    }

    public static int remaining() {
        prune();

        return Minecraft.getInstance().level == null ? 0
            : Math.max(0, scaledLimit(DivingConfig.UNDERWATER_PARTICLE_LIMIT.get()) - particles.size());
    }

    public static void reset() {
        particles.forEach(Particle::remove);
        particles.clear();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        prune();
    }

    private UnderwaterParticleBudget() {}
}
