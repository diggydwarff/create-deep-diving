package com.diggydwarff.deepdiving.client.audio;

/** Implemented by the client-only Channel mixin. Calls run on Minecraft's sound executor. */
public interface AcousticChannel {
    void deepdiving$filter(float amount);
}
