package com.diggydwarff.deepdiving.client.ambience;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.config.DivingConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Controller-owned loops and finite distant cues share the same immersion fade. */
final class AmbientSound extends AbstractTickableSoundInstance {
    private final ClientLevel world;

    private final int layer;

    private final int maximumAge;

    private int age;

    AmbientSound(ClientLevel world, String name, int layer, Vec3 origin) {
        super(SoundEvent.createVariableRangeEvent(DeepDiving.id(name)), SoundSource.AMBIENT, RandomSource.create());
        this.world = world;
        this.layer = layer;
        maximumAge = layer < 2 ? Integer.MAX_VALUE : 200;
        looping = layer < 2;
        relative = looping;
        attenuation = looping ? Attenuation.NONE : Attenuation.LINEAR;
        volume = .0001f;
        x = origin.x;
        y = origin.y;
        z = origin.z;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        float gain = DeepWaterAmbience.soundGain(layer);

        if (Minecraft.getInstance().level != world || age++ >= maximumAge || gain <= 0) {
            stop();
            return;
        }

        // This ceiling is deliberately below equipment breathing and alarm volumes.
        float ceiling = layer < 2 ? .35f : .18f;

        float envelope = looping ? 1 : Math.min(1, age / 12f);

        volume = Math.min(ceiling, DivingConfig.AMBIENT_VOLUME.get().floatValue()) * gain * envelope;
    }
}
