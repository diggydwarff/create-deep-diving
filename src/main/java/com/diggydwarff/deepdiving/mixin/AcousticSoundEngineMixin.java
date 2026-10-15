package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.client.audio.AcousticChannel;
import com.diggydwarff.deepdiving.client.audio.UnderwaterAcoustics;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(SoundEngine.class)
public abstract class AcousticSoundEngineMixin {
    @Shadow
    @Final
    private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Inject(method = "tick", at = @At("HEAD"))
    private void deepdiving$updateTransmission(boolean paused, CallbackInfo callback) {
        UnderwaterAcoustics.tick(paused);

        if (paused) {
            return;
        }

        // Include static and streaming sounds, so a running machine also clears in an air pocket.
        instanceToChannel.forEach((sound, handle) -> deepdiving$apply(sound, handle, false));
    }

    @Inject(method = "play", at = @At("TAIL"))
    private void deepdiving$initialTransmission(SoundInstance sound, CallbackInfo callback) {
        ChannelAccess.ChannelHandle handle = instanceToChannel.get(sound);

        if (handle != null) {
            deepdiving$apply(sound, handle, true);
        }
    }

    private static void deepdiving$apply(SoundInstance sound, ChannelAccess.ChannelHandle handle, boolean starting) {
        float amount = UnderwaterAcoustics.amount(sound, starting);

        handle.execute(channel -> ((AcousticChannel) channel).deepdiving$filter(amount));
    }
}
