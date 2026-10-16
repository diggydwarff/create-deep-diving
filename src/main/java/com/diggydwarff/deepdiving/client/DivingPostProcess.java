package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.diving.DiverService;
import com.diggydwarff.deepdiving.network.DivingPackets;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.neoforged.fml.ModList;

import org.slf4j.Logger;

import java.lang.reflect.Method;

/** Owns only our two-pass chain; never replaces GameRenderer's or another mod's post effect. */
public final class DivingPostProcess {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ConditionVisualState state = new ConditionVisualState();

    private static PostChain chain;

    private static EffectInstance effect;

    private static RenderTarget target;

    private static Object sessionLevel;

    private static Object sessionPlayer;

    private static DivingPackets.Snapshot ignoredSnapshot;

    private static DivingPackets.Snapshot preview;

    private static int width;

    private static int height;

    private static int previewTicks;

    private static int packCheckTicks;

    private static boolean failed;

    private static boolean processed;

    private static boolean shaderPackActive;

    private static boolean irisChecked;

    private static Method irisInstance;

    private static Method irisInUse;

    private static long processedFrames;

    public static void tick() {
        var mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null || !mc.player.isAlive()) {
            reset();
            return;
        }

        if (sessionLevel != mc.level || sessionPlayer != mc.player) {
            state.reset();
            DivingVision.reset();
            close();
            preview = null;
            ignoredSnapshot = ClientRegistration.snapshot;
            sessionLevel = mc.level;
            sessionPlayer = mc.player;
        }

        if (mc.isPaused()) {
            return;
        }

        if (previewTicks > 0 && --previewTicks == 0) {
            preview = null;
        }

        if (--packCheckTicks <= 0) {
            shaderPackActive = shaderPackInUse();
            packCheckTicks = 20;
        }

        var s = visualSnapshot();

        state.tick(
            s == null || !allowedPlayer()
                    ? ConditionVisualState.Levels.NONE
                    : ConditionVisualState.fromPhysiology(
                        s.oxygenation(),
                        s.narcosis(),
                        s.toxicity(),
                        s.stress(),
                        s.pressureInjury(),
                        s.pressureShock()
                    )
        );
    }

    public static DivingPackets.Snapshot visualSnapshot() {
        if (preview != null) {
            return preview;
        }

        var p = Minecraft.getInstance().player;

        if (p == null
            || !DiverService.active(p)
                && !Boolean.getBoolean("deepdiving.visualQa")
                && !Boolean.getBoolean("deepdiving.shaderQa")) {
            return null;
        }

        var s = ClientRegistration.snapshot;

        return s == ignoredSnapshot ? null : s;
    }

    private static boolean allowedPlayer() {
        var p = Minecraft.getInstance().player;

        return p != null
            && p.isAlive()
            && (DiverService.active(p)
                || preview != null
                || Boolean.getBoolean("deepdiving.visualQa")
                || Boolean.getBoolean("deepdiving.shaderQa"));
    }

    public static void render(DeltaTracker delta) {
        processed = false;

        var mc = Minecraft.getInstance();

        if (mc.level == null || !allowedPlayer()) {
            close();
            return;
        }

        float partial = delta.getGameTimeDeltaPartialTick(false);

        var levels = levels(partial);

        if (levels.maximum() <= .002
            || !DivingConfig.SHADER_EFFECTS.get()
            || shaderPackActive && DivingConfig.SHADER_PACK_FALLBACK.get()) {
            close();
            return;
        }

        if (failed) {
            return;
        }

        var main = mc.getMainRenderTarget();

        int filter = main.filterMode;

        try {
            if (target != main || chain == null) {
                close();
                chain = new PostChain(
                    mc.getTextureManager(),
                    mc.getResourceManager(),
                    main,
                    DeepDiving.id("shaders/post/diving.json")
                );

                var swap = chain.getTempTarget("diving_swap");

                effect = chain.addPass("deepdiving:diving", main, swap, true).getEffect();
                chain.addPass("deepdiving:copy", swap, main, false);
                target = main;
                height = 0;
                width = 0;
            }

            if (width != main.width || height != main.height) {
                width = main.width;
                height = main.height;
                chain.resize(width, height);
            }

            uniform("Hypoxia", levels.hypoxia());
            uniform("Narcosis", levels.narcosis());
            uniform("Toxicity", levels.toxicity());
            uniform("Decompression", levels.decompression());
            uniform("Pressure", levels.pressure());
            uniform("Shock", levels.shock());
            uniform("Clock", state.time(partial));
            uniform("Motion", DivingConfig.REDUCED_MOTION.get() ? 0 : mc.options.screenEffectScale().get());
            uniform("CenterCalm", DivingConfig.SHADER_CENTER_CALM.get());
            uniform("Quality", DivingConfig.SHADER_QUALITY.get() == DivingConfig.ShaderQuality.HIGH ? 1 : 0);
            uniform("Blood", DivingConfig.BLOOD_EFFECTS.get() ? 1 : 0);
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            chain.process(0); // Our tick clock also stops when the integrated game is paused.
            processed = true;
            processedFrames++;
        } catch (Exception | LinkageError e) {
            failed = true;
            close();
            LOGGER.warn(
                "Deep Diving world shader unavailable; retaining condition overlays. Reload resources to"
                        + " retry.",
                e
            );
        } finally {
            main.setFilterMode(filter);
            main.bindWrite(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    private static void uniform(String name, double value) {
        effect.safeGetUniform(name).set((float) value);
    }

    public static ConditionVisualState.Levels levels(float partial) {
        if (!DivingConfig.VISUAL_EFFECTS.get() || !allowedPlayer()) {
            return ConditionVisualState.Levels.NONE;
        }

        var s = state.sample(partial);

        double intensity = DivingConfig.EFFECT_INTENSITY.get() * DivingConfig.SHADER_INTENSITY.get();

        return new ConditionVisualState.Levels(
            DivingConfig.HYPOXIA_SHADER.get() ? s.hypoxia() * intensity : 0,
            DivingConfig.NARCOSIS_SHADER.get() ? s.narcosis() * intensity : 0,
            DivingConfig.TOXICITY_SHADER.get() ? s.toxicity() * intensity : 0,
            DivingConfig.DECOMPRESSION_SHADER.get() ? s.decompression() * intensity : 0,
            DivingConfig.PRESSURE_SHADER.get() ? s.pressure() * intensity : 0,
            DivingConfig.PRESSURE_SHADER.get() ? s.shock() * intensity : 0
        );
    }

    public static ConditionVisualState.Levels fallbackLevels(float partial) {
        return !processed
            && DivingConfig.SHADER_EFFECTS.get()
            && DivingConfig.SHADER_OVERLAY_FALLBACK.get()
                ? levels(partial)
                : ConditionVisualState.Levels.NONE;
    }

    /**
     * Local visual demo only. It never changes the packet receiver, potion effects or server state.
     */
    public static void preview(String condition, int severity) {
        double strength = Math.clamp(severity, 1, 3) / 3.0;

        boolean all = condition.equals("combined");

        boolean blackout = condition.equals("blackout");

        preview = new DivingPackets.Snapshot(
            0,
            1,
            0,
            0,
            0,
            blackout
                    ? 10
                    : all || condition.equals("hypoxia") ? 70 - strength * 45 : 100,
            all || condition.equals("toxicity") ? 15 + strength * 100 : 0,
            all || condition.equals("narcosis") ? .2 + strength * 2.6 : 0,
            all || condition.equals("decompression") ? 10 + strength * 70 : 0,
            0,
            all || condition.equals("pressure") ? 10 + strength * 90 : 0,
            0,
            0,
            21,
            0,
            blackout ? DiverService.BLACKOUT | DiverService.HYPOXIA : 0,
            true,
            -1,
            .75
        );
        previewTicks = 1200;
    }

    public static void stopPreview() {
        preview = null;
        previewTicks = 0;
    }

    public static String status() {
        if (!DivingConfig.VISUAL_EFFECTS.get() || !DivingConfig.SHADER_EFFECTS.get()) {
            return "Shaders disabled";
        }

        if (failed) {
            return "Overlay fallback: shader resource error (reload resources to retry)";
        }

        if (shaderPackActive && DivingConfig.SHADER_PACK_FALLBACK.get()) {
            return "Overlay fallback: external shader pack";
        }

        return chain == null
                ? "Ready; no active condition"
                : "World shader active (" + width + " x " + height + ")";
    }

    public static boolean isLoaded() {
        return chain != null;
    }

    /** Independent vanilla nausea stays in the actual map and is never suppressed. */
    public static boolean handlesNausea() {
        if (!DivingConfig.VISUAL_EFFECTS.get() || DivingConfig.EFFECT_INTENSITY.get() == 0) {
            return false;
        }

        if (DivingConfig.REDUCED_MOTION.get()) {
            return true;
        }

        if (!processed || !DivingConfig.SHADER_EFFECTS.get()) {
            return false;
        }

        var strength = levels(1);

        return Math.max(strength.narcosis(), Math.max(strength.toxicity(), strength.decompression()))
                > .01;
    }

    public static long processedFrames() {
        return processedFrames;
    }

    public static void reload() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(DivingPostProcess::reload);
            return;
        }

        close();
        failed = false;
    }

    public static void reset() {
        state.reset();
        stopPreview();
        sessionPlayer = null;
        sessionLevel = null;
        ignoredSnapshot = null;
        processed = false;
        DivingVision.reset();
        close();
    }

    private static void close() {
        if (chain != null) {
            chain.close();
        }

        chain = null;
        effect = null;
        target = null;
        height = 0;
        width = 0;
    }

    private static boolean shaderPackInUse() {
        if (ModList.get().isLoaded("oculus")) {
            return true;
        }

        if (!ModList.get().isLoaded("iris")) {
            return false;
        }

        try {
            if (!irisChecked) {
                irisChecked = true;

                var api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");

                irisInstance = api.getMethod("getInstance");
                irisInUse = api.getMethod("isShaderPackInUse");
            }

            return irisInUse == null || (boolean) irisInUse.invoke(irisInstance.invoke(null));
        } catch (ReflectiveOperationException | LinkageError e) {
            return true; // An unknown optional API uses the safe overlay path.
        }
    }

    private DivingPostProcess() {}
}
