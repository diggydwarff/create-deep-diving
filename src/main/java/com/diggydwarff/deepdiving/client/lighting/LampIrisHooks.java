package com.diggydwarff.deepdiving.client.lighting;

import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/** Optional Iris contracts are reflected, so vanilla/server loading needs no Iris classes. */
public final class LampIrisHooks {
    private static boolean uniformsReady;

    private static boolean samplersReady;

    private static volatile boolean failed;

    // Published before Iris starts its source-reader workers. Reading this flag
    // must never touch OpenGL: those workers have no graphics context.
    private static volatile boolean materialSamplers;

    private static Object reportedPack;

    private static Object reportedDimension;

    private static boolean packSupported;

    /** Runs at Iris's pack-load entry, before it submits parallel source parsing. */
    public static void beginPackLoad() {
        materialSamplers = false;
        uniformsReady = false;
        samplersReady = false;
        reportedPack = null;
        reportedDimension = null;
        packSupported = false;
        failed = false;

        // A thread check must precede every native query. Do not try to move or
        // acquire Minecraft's context here; an unexpected caller gets fallback.
        if (!RenderSystem.isOnRenderThread()) {
            LogUtils.getLogger().warn("Deep Diving: shader pack loaded outside the render thread; material lamps skipped.");
            return;
        }

        try {
            // LWJGL checks its thread-local capabilities in Java before any
            // native call. A missing context cannot reach glGetInteger.
            var capabilities = GL.getCapabilities();

            if (capabilities == null || capabilities.glGetIntegerv == 0L) {
                LogUtils.getLogger().warn("Deep Diving: graphics query unavailable; material lamps skipped.");
                return;
            }

            materialSamplers = GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS) >= 32;
        } catch (IllegalStateException failure) {
            LogUtils.getLogger().warn("Deep Diving: no graphics context during shader pack load; material lamps skipped.", failure);
        }
    }

    /** Safe on Iris worker threads, including during ProgramSource construction. */
    public static boolean canPatch() {
        return materialSamplers && !failed;
    }

    public static void uniforms(Object holder) {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.gl.uniform.UniformHolder");

            Class<?> frequency = Class.forName("net.irisshaders.iris.gl.uniform.UniformUpdateFrequency");

            Object frame = frequency.getField("PER_FRAME").get(null);

            Method integer = api.getMethod("uniform1i", frequency, String.class, IntSupplier.class);

            Method vector = api.getMethod("uniform4f", frequency, String.class, Supplier.class);

            Method matrix = api.getMethod("uniformMatrix", frequency, String.class, Supplier.class);

            Method vector3 = api.getMethod("uniform3f", frequency, String.class, Supplier.class);

            integer.invoke(holder, frame, "ddLampCount", (IntSupplier) LampMaterialBridge::count);
            vector3.invoke(holder, frame, "ddMediumOrigin", (Supplier<?>) LampMaterialBridge::mediumOrigin);
            vector3.invoke(holder, frame, "ddMediumSize", (Supplier<?>) LampMaterialBridge::mediumSize);

            api.getMethod("uniform1f", frequency, String.class, DoubleSupplier.class)
                .invoke(holder, frame, "ddOutsideWater", (DoubleSupplier) LampMaterialBridge::outsideWater);

            for (int index = 0; index < 8; index++) {
                final int slot = index;

                for (String field : new String[]{"Position", "Forward", "Right", "Color", "ShadowOrigin"}) {
                    vector.invoke(holder, frame, "ddLamp" + field + slot,
                        (Supplier<?>) () -> LampMaterialBridge.vector(slot, field));
                }

                matrix.invoke(holder, frame, "ddLampMatrix" + slot,
                    (Supplier<?>) () -> LampMaterialBridge.matrix(slot));
            }

            uniformsReady = true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            disable(failure);
        }
    }

    public static void samplers(Object holder) {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.gl.sampler.SamplerHolder");

            if (!Boolean.TRUE.equals(api.getMethod("hasSampler", String.class).invoke(holder, "ddLampShadow"))) {
                return;
            }

            Class<?> type = Class.forName("net.irisshaders.iris.gl.texture.TextureType");

            Class<?> sampler = Class.forName("net.irisshaders.iris.gl.sampler.GlSampler");

            Method add = api.getMethod("addDynamicSampler", type, IntSupplier.class, sampler, String[].class);

            Object shadows = add.invoke(holder, type.getField("TEXTURE_2D").get(null),
                (IntSupplier) LampMaterialBridge::shadowTexture, sampler.getField("LINEAR_HW").get(null),
                new String[]{"ddLampShadow"});

            Object medium = add.invoke(holder, type.getField("TEXTURE_3D").get(null),
                (IntSupplier) LampMaterialBridge::mediumTexture, sampler.getField("NEAREST").get(null),
                new String[]{"ddLampMedium"});

            if (!Boolean.TRUE.equals(shadows) || !Boolean.TRUE.equals(medium)) {
                throw new IllegalStateException("Iris could not reserve the lamp material samplers");
            }

            samplersReady = true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            disable(failure);
        }
    }

    public static boolean supportedPack() {
        if (failed || !uniformsReady || !samplersReady) {
            return false;
        }

        try {
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");

            Optional<?> current = (Optional<?>) iris.getMethod("getCurrentPack").invoke(null);

            Object dimension = iris.getMethod("getCurrentDimension").invoke(null);

            if (current.isEmpty() || dimension == null) {
                return false;
            }

            Object pack = current.get();

            if (reportedPack == pack && dimension.equals(reportedDimension)) {
                return packSupported;
            }

            Object programs = pack.getClass().getMethod("getProgramSet", iris.getMethod("getCurrentDimension").getReturnType()).invoke(pack, dimension);

            Class<?> id = Class.forName("net.irisshaders.iris.shaderpack.loading.ProgramId");

            Optional<?> terrain = (Optional<?>) programs.getClass().getMethod("get", id)
                .invoke(programs, id.getField("Terrain").get(null));

            boolean supported = false;

            if (terrain.isPresent()) {
                Optional<?> fragment = (Optional<?>) terrain.get().getClass().getMethod("getFragmentSource").invoke(terrain.get());

                supported = fragment.isPresent() && fragment.get().toString().contains(LampMaterialShader.MARKER);
            }

            reportedPack = pack;
            reportedDimension = dimension;
            packSupported = supported;
            LogUtils.getLogger().info("Deep Diving: shader material lamp path {} for the current pack.",
                supported ? "active" : "unavailable (using the post-process fallback)");

            return supported;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            disable(failure);
            return false;
        }
    }

    private static void disable(Throwable failure) {
        if (!failed) LogUtils.getLogger().warn("Deep Diving: Iris material lamp bridge unavailable.", failure);
        failed = true;
    }

    private LampIrisHooks() {}
}
