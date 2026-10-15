package com.diggydwarff.deepdiving.client.lighting;

import com.diggydwarff.deepdiving.config.LampConfig;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModList;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Iris is optional. No Iris types enter the server or vanilla client's class graph. */
final class LampShaderPipeline {
    record Inputs(int depth, int frontDepth, Matrix4f view, Matrix4f projection, boolean shaders) {}

    private static boolean discovered;

    private static boolean failed;

    private static boolean targetsDiscovered;

    private static Object api;

    private static Object captured;

    private static Method active;

    private static Method manager;

    private static Method pipeline;

    private static Method view;

    private static Method projection;

    private static Field targets;

    private static Method noHand;

    private static Method depth;

    private static Method texture;

    private static boolean depthDiscovered;

    private static Object depthState;

    private static Method depthFog;

    private static void discover() throws ReflectiveOperationException {
        discovered = true;

        if (!ModList.get().isLoaded("iris")) {
            return;
        }

        Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");

        api = irisApi.getMethod("getInstance").invoke(null);
        active = irisApi.getMethod("isShaderPackInUse");
    }

    private static void discoverTargets() throws ReflectiveOperationException {
        targetsDiscovered = true;

        Class<?> iris = Class.forName("net.irisshaders.iris.Iris");

        manager = iris.getMethod("getPipelineManager");
        pipeline = Class.forName("net.irisshaders.iris.pipeline.PipelineManager").getMethod("getPipelineNullable");

        Class<?> state = Class.forName("net.irisshaders.iris.uniforms.CapturedRenderingState");

        captured = state.getField("INSTANCE").get(null);
        view = state.getMethod("getGbufferModelView");
        projection = state.getMethod("getGbufferProjection");
        targets = Class.forName("net.irisshaders.iris.pipeline.IrisRenderingPipeline").getDeclaredField("renderTargets");
        targets.setAccessible(true);

        Class<?> renderTargets = Class.forName("net.irisshaders.iris.targets.RenderTargets");

        noHand = renderTargets.getMethod("getDepthTextureNoHand");
        depth = renderTargets.getMethod("getDepthTexture");
        texture = Class.forName("net.irisshaders.iris.targets.DepthTexture").getMethod("getTextureId");
    }

    static boolean active() {
        try {
            if (!discovered) {
                discover();
            }
            // An unknown installed Iris API must not accidentally use vanilla depth.
            return api == null ? failed : Boolean.TRUE.equals(active.invoke(api));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            disable(failure);
            return true;
        }
    }

    static Inputs inputs(int vanillaDepth, Matrix4f vanillaView, Matrix4f vanillaProjection) {
        if (!active()) {
            return new Inputs(vanillaDepth, vanillaDepth, vanillaView, vanillaProjection, false);
        }

        if (failed || !LampConfig.SHADERS.get()) {
            return null;
        }

        try {
            if (!targetsDiscovered) {
                discoverTargets();
            }

            Object current = pipeline.invoke(manager.invoke(null));

            if (current == null || !targets.getDeclaringClass().isInstance(current)) {
                return null;
            }

            Object buffers = targets.get(current);

            int terrain = (int) texture.invoke(noHand.invoke(buffers));

            int front = (int) depth.invoke(buffers);

            return new Inputs(terrain, front, new Matrix4f((Matrix4fc) view.invoke(captured)),
                new Matrix4f((Matrix4fc) projection.invoke(captured)), true);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            disable(failure);
            return null;
        }
    }

    private static void disable(Throwable failure) {
        if (!failed) {
            LogUtils.getLogger().warn("Deep Diving: Iris lamp bridge unavailable; skipping shader lamp rendering.", failure);
        }
        failed = true;
    }

    /** The shader path owns its fog, so vanilla's RenderFog values omit this factor. */
    static float depthFogDensity() {
        if (!ModList.get().isLoaded("fadingdepths")) {
            return 1;
        }

        try {
            if (!depthDiscovered) {
                depthDiscovered = true;

                Class<?> type = Class.forName("com.diggydwarff.fadingdepths.client.DepthState");

                depthState = type.getField("INSTANCE").get(null);
                depthFog = type.getMethod("fogDensity");
            }

            return depthFog == null ? 1 : Math.clamp(((Number) depthFog.invoke(depthState)).floatValue(), 1, 8);
        } catch (ReflectiveOperationException | LinkageError failure) {
            depthFog = null;
            return 1;
        }
    }

    private LampShaderPipeline() {}
}
