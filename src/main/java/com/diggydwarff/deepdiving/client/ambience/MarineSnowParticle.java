package com.diggydwarff.deepdiving.client.ambience;

import com.diggydwarff.deepdiving.presentation.AmbienceMath;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Small, lit translucent flakes; the optional luminous variant uses only a faint light-map floor. */
final class MarineSnowParticle extends TextureSheetParticle {
    private final boolean luminous;

    private final float opacity;

    private final double driftPhase;

    MarineSnowParticle(ClientLevel level, Vec3 origin, TextureAtlas atlas, boolean luminous) {
        super(level, origin.x, origin.y, origin.z);
        this.luminous = luminous;
        opacity = luminous ? .12f + random.nextFloat() * .06f : .10f + random.nextFloat() * .16f;
        driftPhase = random.nextDouble() * Math.PI * 2;
        lifetime = luminous ? 100 + random.nextInt(100) : 180 + random.nextInt(140);
        quadSize = luminous ? .018f + random.nextFloat() * .012f : .018f + random.nextFloat() * .025f;
        xd = (random.nextDouble() - .5) * .002;
        yd = -.004 - random.nextDouble() * .006;
        zd = (random.nextDouble() - .5) * .002;
        hasPhysics = true;
        friction = 1;
        gravity = 0;
        alpha = 0;
        setSize(.025f, .025f);
        setSprite(atlas.getSprite(ResourceLocation.withDefaultNamespace("generic_0")));

        if (luminous) {
            setColor(.18f, .55f, .46f);
        } else {
            setColor(.70f, .74f, .71f);
        }
    }

    boolean luminous() {
        return luminous;
    }

    @Override
    public void tick() {
        var player = Minecraft.getInstance().player;

        float visibility = DeepWaterAmbience.particleVisibility(luminous);

        if (Minecraft.getInstance().level != level || player == null || player.distanceToSqr(x, y, z) > 14 * 14
            || visibility <= 0
            || !AmbienceWater.clearWater(level, new Vec3(x, y, z))) {
            remove();
            return;
        }

        // Slow settling with a tiny lateral drift. No sediment cloud or terrain interaction.
        xd = Math.sin(driftPhase + age * .017) * .0018;
        zd = Math.cos(driftPhase + age * .013) * .0018;
        super.tick();

        if (onGround || !AmbienceWater.clearWater(level, new Vec3(x, y, z))) {
            remove();
            return;
        }

        float lifeFade = Math.min(1, Math.min(age / 20f, (lifetime - age) / 30f));

        alpha = opacity * visibility * Math.max(0, lifeFade);
    }

    @Override
    protected int getLightColor(float partialTick) {
        // The lamp is rendered light, so reveal nearby flakes explicitly without
        // changing terrain light or making particles beyond the beam emissive.
        int light = com.diggydwarff.deepdiving.client.lighting.DivingLampLights.particleLight(
            x, y, z, super.getLightColor(partialTick));

        return AmbienceMath.particleLight(light, luminous);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
