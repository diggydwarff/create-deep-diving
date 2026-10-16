package com.diggydwarff.deepdiving.client.lighting;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryStack;

import java.util.HashMap;
import java.util.Map;

/** Uniforms for ordinary Minecraft and Sodium programs; Iris retains its own bridge. */
public final class LampVanillaUniforms {
    // Both renderers use the low units for their block atlas and light map.
    // OpenGL 3.2 guarantees at least sixteen fragment texture units.
    private static final int SHADOW_UNIT = 12;

    private static final int MEDIUM_UNIT = 13;

    private static final Map<Integer, Map<String, Integer>> LOCATIONS = new HashMap<>();

    private static final Map<Integer, Long> UPLOADED_FRAMES = new HashMap<>();

    private static final Matrix4f inverseView = new Matrix4f();

    private static boolean drawingWorld;

    private static long frameNumber;

    private static boolean texturesBound;

    private static int previousShadow;

    private static int previousMedium;

    private static int previousShadowSampler;

    private static int previousMediumSampler;

    static void beginFrame(Matrix4f view) {
        inverseView.set(view).invert();
        drawingWorld = true;
        frameNumber++;
    }

    static void endFrame() {
        drawingWorld = false;
        releaseTextures();
    }

    public static void bindCurrent() {
        bind(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM));
    }

    public static void bind(int program) {
        if (program <= 0) {
            return;
        }

        Map<String, Integer> locations = LOCATIONS.computeIfAbsent(program, ignored -> new HashMap<>());

        // Complementary also has ddLampCount. Its sampler assignments and
        // uniform uploads belong to Iris, so identify our native vertex stage.
        if (location(program, locations, "ddInverseView") < 0) {
            return;
        }

        int countLocation = location(program, locations, "ddLampCount");

        if (countLocation < 0) {
            return;
        }

        int count = drawingWorld && LampMaterialBridge.vanillaActive() ? LampMaterialBridge.count() : 0;

        GL20.glUniform1i(countLocation, count);

        // Different sampler types must use distinct units even in a draw with
        // zero lights; otherwise OpenGL can reject the whole terrain draw.
        integer(program, locations, "ddLampShadow", SHADOW_UNIT);
        integer(program, locations, "ddLampMedium", MEDIUM_UNIT);

        if (count == 0) {
            return;
        }

        bindTextures();

        // Vanilla can apply the same terrain program once per chunk. Texture
        // bindings must be restored for each draw, but the lights do not change.
        if (Long.valueOf(frameNumber).equals(UPLOADED_FRAMES.get(program))) {
            return;
        }

        matrix(program, locations, "ddInverseView", inverseView);
        vector(program, locations, "ddMediumOrigin", LampMaterialBridge.mediumOrigin());
        vector(program, locations, "ddMediumSize", LampMaterialBridge.mediumSize());
        scalar(program, locations, "ddOutsideWater", LampMaterialBridge.outsideWater());
        scalar(program, locations, "ddShadowSoftness", (float) LampMaterialBridge.shadowSoftness());

        for (int index = 0; index < count; index++) {
            for (String field : new String[]{"Position", "Forward", "Right", "Color", "ShadowOrigin"}) {
                Vector4f value = LampMaterialBridge.vector(index, field);

                GL20.glUniform4f(location(program, locations, "ddLamp" + field + index),
                    value.x, value.y, value.z, value.w);
            }

            matrix(program, locations, "ddLampMatrix" + index, LampMaterialBridge.matrix(index));
        }

        UPLOADED_FRAMES.put(program, frameNumber);
    }

    private static void bindTextures() {
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        GL13.glActiveTexture(GL13.GL_TEXTURE0 + SHADOW_UNIT);

        if (!texturesBound) {
            previousShadow = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            previousShadowSampler = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
        }

        GL33.glBindSampler(SHADOW_UNIT, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, LampMaterialBridge.shadowTexture());
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + MEDIUM_UNIT);

        if (!texturesBound) {
            previousMedium = GL11.glGetInteger(GL12.GL_TEXTURE_BINDING_3D);
            previousMediumSampler = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
        }

        GL33.glBindSampler(MEDIUM_UNIT, 0);
        GL11.glBindTexture(GL12.GL_TEXTURE_3D, LampMaterialBridge.mediumTexture());
        GL13.glActiveTexture(active);
        texturesBound = true;
    }

    public static void releaseTextures() {
        if (!texturesBound) {
            return;
        }

        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        GL13.glActiveTexture(GL13.GL_TEXTURE0 + SHADOW_UNIT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousShadow);
        GL33.glBindSampler(SHADOW_UNIT, previousShadowSampler);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + MEDIUM_UNIT);
        GL11.glBindTexture(GL12.GL_TEXTURE_3D, previousMedium);
        GL33.glBindSampler(MEDIUM_UNIT, previousMediumSampler);
        GL13.glActiveTexture(active);
        texturesBound = false;
    }

    private static int location(int program, Map<String, Integer> locations, String name) {
        return locations.computeIfAbsent(name, key -> GL20.glGetUniformLocation(program, key));
    }

    private static void integer(int program, Map<String, Integer> locations, String name, int value) {
        GL20.glUniform1i(location(program, locations, name), value);
    }

    private static void scalar(int program, Map<String, Integer> locations, String name, float value) {
        GL20.glUniform1f(location(program, locations, name), value);
    }

    private static void vector(int program, Map<String, Integer> locations, String name, Vector3f value) {
        GL20.glUniform3f(location(program, locations, name), value.x, value.y, value.z);
    }

    private static void matrix(int program, Map<String, Integer> locations, String name, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL20.glUniformMatrix4fv(location(program, locations, name), false, value.get(stack.mallocFloat(16)));
        }
    }

    public static void forgetPrograms() {
        LOCATIONS.clear();
        UPLOADED_FRAMES.clear();
    }

    static void reset() {
        endFrame();
        forgetPrograms();
    }

    private LampVanillaUniforms() {}
}
