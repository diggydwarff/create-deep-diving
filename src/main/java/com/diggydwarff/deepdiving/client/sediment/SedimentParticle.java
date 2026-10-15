package com.diggydwarff.deepdiving.client.sediment;

import com.diggydwarff.deepdiving.client.ambience.AmbienceWater;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Lit brown wisps and grains remain distinct from the small descending marine-snow flakes. */
final class SedimentParticle extends TextureSheetParticle {
    private final float restingAlpha;

    SedimentParticle(ClientLevel level, Vec3 position, TextureAtlas atlas, int color) {
        super(level, position.x, position.y, position.z);

        boolean wisp = random.nextInt(4) != 0;

        setSprite(atlas.getSprite(ResourceLocation.withDefaultNamespace(wisp ? "generic_7" : "generic_0")));
        lifetime = 55 + random.nextInt(45);
        quadSize = wisp ? .35f + random.nextFloat() * .35f : .035f + random.nextFloat() * .045f;
        restingAlpha = wisp ? .28f : .55f;
        alpha = 0;
        setColor(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f);
        xd = (random.nextDouble() - .5) * .008;
        yd = -.001 - random.nextDouble() * .003;
        zd = (random.nextDouble() - .5) * .008;
        gravity = 0;
        friction = .99f;
        hasPhysics = true;
        setSize(.04f, .04f);
    }

    @Override
    public void tick() {
        if (Minecraft.getInstance().level != level || !SedimentClouds.viewingWater()
            || !AmbienceWater.clearWater(level, new Vec3(x, y, z))) {
            remove();
            return;
        }

        super.tick();

        if (onGround || !AmbienceWater.clearWater(level, new Vec3(x, y, z))) {
            remove();
            return;
        }

        float fadeIn = Math.min(1, age / 4f);

        float fadeOut = Math.min(1, (lifetime - age) / 20f);

        alpha = restingAlpha * fadeIn * Math.max(0, fadeOut);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
