package com.diggydwarff.deepdiving.client.lighting;

import com.diggydwarff.deepdiving.config.LampConfig;
import com.BreadRes.illuminationapi.client.LightShadowMap;
import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.client.ambience.AmbienceWater;
import com.diggydwarff.deepdiving.mixin.LampShadowRegistryAccess;
import com.diggydwarff.deepdiving.mixin.LampShadowResourceAccess;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** One terrain/volumetric pass, after Iris composition and before vanilla hands/HUD. */
@EventBusSubscriber(modid = DeepDiving.MOD_ID, value = Dist.CLIENT)
public final class LampRenderer {
    private static final LampMedium MEDIUM = new LampMedium();

    private static final Map<UUID, Shadow> SHADOWS = new HashMap<>();

    private static final Map<String, Integer> UNIFORMS = new HashMap<>();

    private static List<DivingLampLights.Frame> frames = List.of();

    private static Player castingOwner;

    private static int program;

    private static int vao;

    private static int opaqueDepth;

    private static int depthWidth;

    private static int depthHeight;

    private static float fogStart;

    private static float fogEnd = 96;

    private static boolean ready;

    private static boolean failed;

    private static boolean shaderUnavailable;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void stage(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (event.getCamera() != minecraft.gameRenderer.getMainCamera()) {
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            frames = DivingLampLights.collect(event.getPartialTick().getGameTimeDeltaPartialTick(false));
            ready = true;
            fogStart = RenderSystem.getShaderFogStart();
            fogEnd = RenderSystem.getShaderFogEnd();

            if (!frames.isEmpty() && !LampShaderPipeline.active()) {
                try (LampGlState state = new LampGlState()) {
                    copyOpaqueDepth();
                }
            }
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL && ready) {
            ready = false;

            if (frames.isEmpty()) {
                releaseShadows();
                return;
            }

            if (failed || minecraft.level == null) {
                return;
            }

            try (LampGlState state = new LampGlState()) {
                render(event);
            } catch (RuntimeException | LinkageError failure) {
                failed = true;
                LogUtils.getLogger().error("Deep Diving: lamp renderer failed; reload resources to retry.", failure);
            }
        }
    }

    private static void copyOpaqueDepth() {
        var target = Minecraft.getInstance().getMainRenderTarget();

        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        if (opaqueDepth == 0) {
            opaqueDepth = GL11.glGenTextures();
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, opaqueDepth);

        if (depthWidth != target.width || depthHeight != target.height) {
            depthWidth = target.width;
            depthHeight = target.height;
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT32F,
                depthWidth, depthHeight, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, 0L);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        }

        // Copy conversion handles vanilla depth/stencil formats without requiring
        // matching depth attachments, unlike a cross-format framebuffer blit.
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, depthWidth, depthHeight);
    }

    private static void render(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();

        var input = LampShaderPipeline.inputs(opaqueDepth, event.getModelViewMatrix(), event.getProjectionMatrix());

        shaderUnavailable = input == null;

        if (input == null || input.depth() <= 0) {
            return;
        }

        ensureProgram();

        Vec3 camera = event.getCamera().getPosition();

        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 2);
        MEDIUM.update(minecraft.level, camera);

        // Keys are private to this renderer, so other API consumers remain untouched.
        SHADOWS.entrySet().removeIf(entry -> {
            if (frames.stream().noneMatch(frame -> frame.owner().getUUID().equals(entry.getKey()))) {
                dispose(entry.getValue());
                return true;
            }
            return false;
        });

        Matrix4f inverse = new Matrix4f(input.projection()).mul(input.view()).invert();

        int updates = 0;

        long now = System.nanoTime();

        int budget = LampConfig.REDUCED.get() ? 1 : LampConfig.SHADOW_UPDATES.get();

        for (DivingLampLights.Frame frame : frames) {
            var source = frame.source();

            Shadow shadow = SHADOWS.computeIfAbsent(frame.owner().getUUID(), ignored -> new Shadow(source.cacheKey()));

            if (now >= shadow.nextUpdate && updates < budget) {
                castingOwner = frame.owner();

                try {
                    shadow.data = LightShadowMap.get(source, source.lightPos(), source.forward(), source.up(),
                        source.range() + source.beamStart(), source.outerAngle(), source.beamStart());
                    shadow.origin = source.lightPos();
                    shadow.nextUpdate = now + 1_000_000_000L / LampConfig.SHADOW_RATE.get();
                    updates++;
                } finally {
                    castingOwner = null;
                }
            }

            // A newly admitted lamp waits for a real shadow map. We never draw an
            // unshadowed substitute that can shine through a wall under load.
            if (shadow.data == null) {
                continue;
            }

            draw(frame, shadow, input, inverse, camera);
        }
    }

    private static void draw(DivingLampLights.Frame frame, Shadow shadow, LampShaderPipeline.Inputs input,
        Matrix4f inverse, Vec3 camera) {
        var target = Minecraft.getInstance().getMainRenderTarget();

        var source = frame.source();

        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
        GL11.glViewport(0, 0, target.width, target.height);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
        GL14.glBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
        RenderSystem.depthMask(false);
        GL20.glUseProgram(program);
        GL30.glBindVertexArray(vao);

        texture(0, input.depth(), "DepthTex");
        texture(1, shadow.data.textureId(), "ShadowTex");
        texture(3, input.frontDepth(), "FrontDepthTex");
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + 2);
        GL33.glBindSampler(2, 0);
        GL11.glBindTexture(GL12.GL_TEXTURE_3D, MEDIUM.texture());
        GL20.glUniform1i(uniform("MediumTex"), 2);

        matrix("InvViewProj", inverse);
        matrix("LightViewProj", shadow.data.lightViewProj());
        vector("CameraPos", Vec3.ZERO);
        vector("LightPos", source.lightPos().subtract(camera));
        vector("EmitterPos", source.emitterPos().subtract(camera));
        vector("AperturePos", source.aperturePos().subtract(camera));
        vector("ShadowOrigin", shadow.origin.subtract(camera));
        vector("MediumOrigin", MEDIUM.origin().subtract(camera));
        GL20.glUniform3f(uniform("MediumSize"), LampMedium.X, LampMedium.Y, LampMedium.Z);
        vector("LightForward", source.forward());
        vector("LightRight", source.right());
        vector("LightUp", source.up());

        int color = source.color();

        GL20.glUniform3f(uniform("LightColor"), (color >> 16 & 255) / 255f,
            (color >> 8 & 255) / 255f, (color & 255) / 255f);
        scalar("Range", source.range());
        scalar("BeamStart", source.beamStart());
        scalar("ApertureRadius", source.apertureRadius());
        scalar("TanOuter", (float) Math.tan(Math.toRadians(source.outerAngle())));
        scalar("TanInner", (float) Math.tan(Math.toRadians(source.innerAngle())));
        scalar("SurfaceIntensity", source.surfaceIntensity());
        scalar("VolumeIntensity", source.volumeIntensity());
        float depthFog = input.shaders() ? LampShaderPipeline.depthFogDensity() : 1;

        scalar("WaterExtinction", .025f * depthFog);

        boolean wet = AmbienceWater.contains(Minecraft.getInstance().level, camera);

        float end = wet && Float.isFinite(fogEnd) && fogEnd > 1 ? Math.min(96, fogEnd) : 96;

        scalar("WaterFogStart", wet && Float.isFinite(fogStart) ? Math.max(0, fogStart) : 0);
        scalar("WaterFogEnd", end / depthFog);
        scalar("OutsideWater", wet || AmbienceWater.contains(Minecraft.getInstance().level, source.lightPos()) ? 1 : 0);
        scalar("MaskHand", input.shaders() ? 1 : 0);
        GL20.glUniform2f(uniform("ShadowTexel"), 1f / LightShadowMap.SIZE, 1f / LightShadowMap.SIZE);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
    }

    private static void texture(int unit, int id, String name) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL33.glBindSampler(unit, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL20.glUniform1i(uniform(name), unit);
    }

    private static int uniform(String name) {
        return UNIFORMS.computeIfAbsent(name, key -> GL20.glGetUniformLocation(program, key));
    }

    private static void scalar(String name, float value) {
        GL20.glUniform1f(uniform(name), value);
    }

    private static void vector(String name, Vec3 value) {
        GL20.glUniform3f(uniform(name), (float) value.x, (float) value.y, (float) value.z);
    }

    private static void vector(String name, Vector3f value) {
        GL20.glUniform3f(uniform(name), value.x, value.y, value.z);
    }

    private static void matrix(String name, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL20.glUniformMatrix4fv(uniform(name), false, value.get(stack.mallocFloat(16)));
        }
    }

    private static void ensureProgram() {
        if (program != 0) {
            return;
        }

        String vertex = resource(LightShadowMap.class, "/assets/illuminationapi/shaders/post/light_volume.vsh");

        String upstream = resource(LightShadowMap.class, "/assets/illuminationapi/shaders/post/light_volume.fsh");

        String medium = resource(LampRenderer.class, "/assets/deepdiving/shaders/lighting/medium.glsl");

        int vertexShader = compile(GL20.GL_VERTEX_SHADER, vertex);

        int fragmentShader = 0;

        int candidate = 0;

        try {
            fragmentShader = compile(GL20.GL_FRAGMENT_SHADER, LampShaderSource.adapt(upstream, medium));
            candidate = GL20.glCreateProgram();
            GL20.glAttachShader(candidate, vertexShader);
            GL20.glAttachShader(candidate, fragmentShader);
            GL20.glLinkProgram(candidate);

            if (GL20.glGetProgrami(candidate, GL20.GL_LINK_STATUS) == 0) {
                throw new IllegalStateException(GL20.glGetProgramInfoLog(candidate));
            }

            program = candidate;
            candidate = 0;
            vao = GL30.glGenVertexArrays();
        } finally {
            GL20.glDeleteShader(vertexShader);
            if (fragmentShader != 0) {
                GL20.glDeleteShader(fragmentShader);
            }
            if (candidate != 0) {
                GL20.glDeleteProgram(candidate);
            }
        }
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);

        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            String message = GL20.glGetShaderInfoLog(shader);

            GL20.glDeleteShader(shader);
            throw new IllegalStateException(message);
        }
        return shader;
    }

    private static String resource(Class<?> owner, String path) {
        try (var stream = owner.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing shader " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    public static Player castingOwner() {
        return castingOwner;
    }

    public static boolean available() {
        return !failed && !shaderUnavailable;
    }

    private static void dispose(Shadow shadow) {
        Object entry = LampShadowRegistryAccess.deepdiving$shadowCache().remove(shadow.key);

        if (entry instanceof LampShadowResourceAccess resource) {
            GL30.glDeleteFramebuffers(resource.deepdiving$framebuffer());
            GL11.glDeleteTextures(resource.deepdiving$depthTexture());
        }
    }

    private static void releaseShadows() {
        SHADOWS.values().forEach(LampRenderer::dispose);
        SHADOWS.clear();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        DivingLampLights.reset();
    }

    public static void reset() {
        releaseShadows();
        MEDIUM.close();
        if (program != 0) {
            GL20.glDeleteProgram(program);
        }
        if (vao != 0) {
            GL30.glDeleteVertexArrays(vao);
        }
        if (opaqueDepth != 0) {
            GL11.glDeleteTextures(opaqueDepth);
        }
        program = 0;
        vao = 0;
        opaqueDepth = 0;
        depthWidth = 0;
        depthHeight = 0;
        frames = List.of();
        UNIFORMS.clear();
        ready = false;
        failed = false;
        shaderUnavailable = false;
    }

    private static final class Shadow {
        private final Object key;

        private LightShadowMap.ShadowData data;

        private Vec3 origin;

        private long nextUpdate;

        private Shadow(Object key) {
            this.key = key;
        }
    }

    private LampRenderer() {}
}
