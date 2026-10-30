package com.diggydwarff.deepdiving.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BubbleParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;

/** Cosmetic exhaust stays behind the diver and rises until it surfaces or reaches its time cap. */
public final class DiverBubbleParticle extends BubbleParticle {
    private final float originalSize;
    private final double riseSpeed, acceleration, driftPhase, surfaceDissolve, surfaceFadeRange;
    private final int fadeTicks;
    private double surfaceY = Double.POSITIVE_INFINITY;

    DiverBubbleParticle(
            ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
        super(level, x, y, z, dx, dy, dz);
        lifetime = 160 + random.nextInt(141);
        fadeTicks = 40 + random.nextInt(41);
        float size = random.nextFloat();
        quadSize = .025f + size * .035f;
        originalSize = quadSize;
        riseSpeed = .026 + .044 * size + random.nextDouble() * .004;
        acceleration = .0003 + random.nextDouble() * .00045;
        driftPhase = random.nextDouble() * Math.PI * 2;
        surfaceDissolve = .08 + random.nextDouble() * .42;
        surfaceFadeRange = 1.4 + random.nextDouble() * .8;
        xd = dx + (random.nextDouble() - .5) * .012;
        yd = Math.min(riseSpeed, Math.max(.010, dy * .45 + random.nextDouble() * .010));
        zd = dz + (random.nextDouble() - .5) * .012;
        alpha = .85f;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime
                || !level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER)) {
            remove();
            return;
        }
        yd = Math.min(riseSpeed, yd + acceleration);
        move(xd, yd, zd);
        xd = xd * .95 + Math.sin(driftPhase + age * .10) * .00035;
        zd = zd * .95 + Math.cos(driftPhase + age * .085) * .00035;
        quadSize = originalSize * (1 + .12f * age / lifetime);
        if (age == 1 || age % 4 == 0) surfaceY = nearbySurface();
        float lifeFade = Math.clamp((lifetime - age) / (float) fadeTicks, 0, 1);
        float surfaceFade =
                (float) Math.clamp((surfaceY - y - surfaceDissolve) / surfaceFadeRange, 0, 1);
        // Each bubble dissolves at its own distance from the surface, without fading back in.
        alpha = Math.min(alpha, .85f * Math.min(lifeFade, surfaceFade));
        if (y == yo
                || onGround
                || alpha <= .015f
                || !level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER)) remove();
    }

    /** Short client-only scan; it never asks for distant chunks or searches the full ocean. */
    private double nearbySurface() {
        BlockPos.MutableBlockPos pos = BlockPos.containing(x, y, z).mutable();
        for (int i = 0; i < 4; i++, pos.move(net.minecraft.core.Direction.UP)) {
            if (!level.hasChunkAt(pos)) return Double.POSITIVE_INFINITY;
            var water = level.getFluidState(pos);
            if (!water.is(FluidTags.WATER)) return pos.getY();
            double top = pos.getY() + water.getHeight(level, pos);
            var above = pos.above();
            if (top < pos.getY() + 1 || !level.getFluidState(above).is(FluidTags.WATER)) return top;
        }
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double dx,
                double dy,
                double dz) {
            var bubble = new DiverBubbleParticle(level, x, y, z, dx, dy, dz);
            bubble.pickSprite(sprites);
            return bubble;
        }
    }
}
