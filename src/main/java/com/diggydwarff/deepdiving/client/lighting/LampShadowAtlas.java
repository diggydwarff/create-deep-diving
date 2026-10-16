package com.diggydwarff.deepdiving.client.lighting;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import java.util.Arrays;
import java.util.List;

/** Shares up to eight API shadow maps through one Iris sampler. */
final class LampShadowAtlas {
    private int texture;

    private int reader;

    private final Object[] keys = new Object[8];

    private final long[] versions = new long[8];

    int texture() {
        return texture;
    }

    void update(List<LampMaterialBridge.Light> lights) {
        if (texture == 0) {
            texture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24,
                2048, 1024, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, 0L);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL14.GL_COMPARE_R_TO_TEXTURE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_FUNC, GL11.GL_LEQUAL);
            reader = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, reader);
            GL11.glDrawBuffer(GL11.GL_NONE);
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, reader);
        GL11.glReadBuffer(GL11.GL_NONE);

        for (int index = 0; index < lights.size(); index++) {
            var light = lights.get(index);

            if (keys[index] == light.source().cacheKey() && versions[index] == light.revision()) {
                continue;
            }

            GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                GL11.GL_TEXTURE_2D, light.depth(), 0);

            if (GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
                throw new IllegalStateException("Deep Diving: shadow atlas source is incomplete");
            }

            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, (index % 4) * 512,
                (index / 4) * 512, 0, 0, 512, 512);
            keys[index] = light.source().cacheKey();
            versions[index] = light.revision();
        }

        // Do not keep an attachment reference to an API texture after this copy.
        GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
            GL11.GL_TEXTURE_2D, 0, 0);
    }

    void close() {
        if (texture != 0) GL11.glDeleteTextures(texture);
        if (reader != 0) GL30.glDeleteFramebuffers(reader);
        texture = 0;
        reader = 0;
        Arrays.fill(keys, null);
        Arrays.fill(versions, 0);
    }
}
