package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.client.audio.AcousticChannel;
import com.diggydwarff.deepdiving.client.audio.DirectSoundFilter;
import com.mojang.blaze3d.audio.Channel;

import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public abstract class AcousticChannelMixin implements AcousticChannel {
    @Shadow
    @Final
    private int source;

    @Unique
    private final DirectSoundFilter deepdiving$directFilter = new DirectSoundFilter();

    @Unique
    private float deepdiving$baseVolume = 1;

    @Unique
    private float deepdiving$fallbackGain = 1;

    @ModifyVariable(method = "setVolume", at = @At("HEAD"), argsOnly = true)
    private float deepdiving$volume(float volume) {
        deepdiving$baseVolume = volume;

        return volume * deepdiving$fallbackGain;
    }

    @Override
    public void deepdiving$filter(float amount) {
        float gain = deepdiving$directFilter.apply(source, amount);

        if (gain != deepdiving$fallbackGain) {
            deepdiving$fallbackGain = gain;
            AL10.alSourcef(source, AL10.AL_GAIN, deepdiving$baseVolume * gain);
        }
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void deepdiving$releaseFilter(CallbackInfo callback) {
        deepdiving$directFilter.clear(source);
    }
}
