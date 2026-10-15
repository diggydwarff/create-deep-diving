package com.diggydwarff.deepdiving.client.audio;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.EXTEfx;

/** A channel owns its filter and releases it before its OpenAL source is destroyed. */
public final class DirectSoundFilter {
    private int filter;

    private float previousAmount = -1;

    private boolean unavailable;

    public float apply(int source, float amount) {
        amount = Math.clamp(amount, 0, 1);

        if (amount <= .001f) {
            clear(source);
            return 1;
        }

        // Some audio drivers do not expose EFX. Keep attenuation available on those devices.
        if (unavailable || AL.getCapabilities().alGenFilters == 0) {
            return gain(amount);
        }

        if (filter == 0) {
            filter = EXTEfx.alGenFilters();

            if (filter == 0) {
                unavailable = true;
                return gain(amount);
            }
            EXTEfx.alFilteri(filter, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
        }

        if (amount != previousAmount) {
            EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAIN, gain(amount));
            EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAINHF, 1 - .97f * amount);
            // OpenAL copies filter parameters when binding; rebind after each adjustment.
            AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, filter);
            previousAmount = amount;
        }

        return 1;
    }

    private static float gain(float amount) {
        return 1 - .70f * amount;
    }

    public void clear(int source) {
        if (filter != 0) {
            AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);
            EXTEfx.alDeleteFilters(filter);
            filter = 0;
        }

        previousAmount = -1;
    }
}
