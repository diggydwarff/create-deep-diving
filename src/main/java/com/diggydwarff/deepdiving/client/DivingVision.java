package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.diving.DiverService;
import com.diggydwarff.deepdiving.physiology.DiveDynamics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Eye masks and injury overlays also work when world post processing is unavailable. */
public final class DivingVision {
    private static double oxygen = 100;

    private static double injury;

    private static double shock;

    private static double clock;

    public static void reset() {
        oxygen = 100;
        clock = 0;
        shock = 0;
        injury = 0;
    }

    public static void tick() {
        var mc = Minecraft.getInstance();

        if (mc.player == null || !mc.player.isAlive()) {
            reset();
            return;
        }

        if (mc.isPaused()) {
            return;
        }

        var snapshot = DivingPostProcess.visualSnapshot();

        if (snapshot == null) {
            reset();
            return;
        }

        oxygen += (snapshot.oxygenation() - oxygen) * .18;
        injury += (snapshot.pressureInjury() - injury) * .18;
        shock = Math.max(snapshot.pressureShock() / 100, shock * .92);
        clock += .05;
    }

    public static double severity() {
        return DiveDynamics.hypoxia(oxygen);
    }

    public static double pressureSeverity() {
        return Math.clamp(injury / 100 + shock * .6, 0, 1);
    }

    public static double eyeClosure(float partial) {
        var snapshot = DivingPostProcess.visualSnapshot();

        if (snapshot != null && DivingConfig.REDUCED_MOTION.get()) {
            return (snapshot.warnings() & DiverService.BLACKOUT) != 0
                    ? 1
                    : Math.max(0, severity() - .25);
        }

        return snapshot == null
                ? 0
                : DiveDynamics.eyelidClosure(
                    clock + partial / 20,
                    severity(),
                    (snapshot.warnings() & DiverService.BLACKOUT) != 0
                );
    }

    public static void render(GuiGraphics graphics, float partial) {
        var snapshot = DivingPostProcess.visualSnapshot();

        var mc = Minecraft.getInstance();

        if (snapshot == null
            || mc.player == null
            || !mc.player.isAlive()
            || !DivingConfig.VISUAL_EFFECTS.get()) {
            return;
        }

        if (!DiverService.active(mc.player)
            && !Boolean.getBoolean("deepdiving.visualQa")
            && !Boolean.getBoolean("deepdiving.shaderQa")
            && snapshot == ClientRegistration.snapshot) {
            return;
        }

        double intensity = DivingConfig.EFFECT_INTENSITY.get();

        if (intensity == 0) {
            return;
        }

        double hypoxia = severity();

        double pressure = pressureSeverity();

        int width = graphics.guiWidth();

        int height = graphics.guiHeight();

        double time = clock + partial / 20;

        renderHypoxia(graphics, width, height, hypoxia, intensity);

        renderPressureInjury(graphics, width, height, pressure, intensity, time);

        renderFallbackConditions(graphics, width, height, partial, time);

        renderEyelids(graphics, width, height, partial, intensity);
    }

    /** Dim the edges gradually as oxygen falls; the centre remains usable until severe hypoxia. */
    private static void renderHypoxia(
        GuiGraphics graphics,
        int width,
        int height,
        double hypoxia,
        double intensity
    ) {
        // Layered elliptical edges narrow smoothly instead of covering everything with a flat
        // filter.
        if (hypoxia > .01) {
            for (int layer = 0; layer < 5; layer++) {
                double radius = 1.36 - hypoxia * .88 + layer * .045;

                ellipseOutside(
                    graphics,
                    width,
                    height,
                    radius,
                    radius,
                    color(0, .09 * intensity * hypoxia)
                );
            }

            graphics.fill(0, 0, width, height, color(0, hypoxia * .10 * intensity));
        }
    }

    /** Keep pressure injury separate from hypoxia so both overlays can contribute in draw order. */
    private static void renderPressureInjury(
        GuiGraphics graphics,
        int width,
        int height,
        double pressure,
        double intensity,
        double time
    ) {
        if (pressure > .01 && DivingConfig.BLOOD_EFFECTS.get()) {
            double pulse = DivingConfig.REDUCED_MOTION.get() ? .85 : .78 + .22 * Math.sin(time * 5.2);

            for (int layer = 0; layer < 5; layer++) {
                ellipseOutside(
                    graphics,
                    width,
                    height,
                    .84 + layer * .05,
                    .88 + layer * .05,
                    color(0x650812, pressure * .13 * pulse * intensity)
                );
            }

            // Irregular droplets and small trails cling to the edges of the mask.
            for (int i = 0; i < 18; i++) {
                double seed = Math.sin(i * 83.17) * .5 + .5;

                double secondary = Math.cos(i * 2.399963 + .75) * .5 + .5;

                int x = (int) (width * (i % 2 == 0 ? .02 + seed * .17 : .81 + seed * .17));

                int y = (int) (height * secondary);

                int radius = 2 + i % 5;

                for (int dy = -radius; dy <= radius; dy++) {
                    int span = (int) Math.sqrt(radius * radius - dy * dy);

                    graphics.fill(
                        x - span,
                        y + dy,
                        x + span + 1,
                        y + dy + 1,
                        color(0x790511, pressure * .82 * intensity)
                    );
                }

                graphics.fill(
                    x,
                    y + radius,
                    x + 1 + i % 2,
                    y + radius + (int) (seed * 18 * pressure),
                    color(0x55050D, pressure * .65 * intensity)
                );
            }

            if (shock > .02) {
                graphics.fill(
                    0,
                    0,
                    width,
                    height,
                    color(0xA31520, Math.min(.45, shock * .45) * intensity)
                );
            }
        }
    }

    /** Draw simple condition tints when the optional post-processing shader cannot handle them. */
    private static void renderFallbackConditions(
        GuiGraphics graphics,
        int width,
        int height,
        float partial,
        double time
    ) {
        var fallback = DivingPostProcess.fallbackLevels(partial);

        if (fallback.narcosis() > .01
            || fallback.toxicity() > .01
            || fallback.decompression() > .01) {
            double pulse = DivingConfig.REDUCED_MOTION.get() ? 1 : .85 + .15 * Math.sin(time * 1.3);

            for (int layer = 0; layer < 4; layer++) {
                double radius = .94 + layer * .06;

                ellipseOutside(
                    graphics,
                    width,
                    height,
                    radius,
                    radius,
                    color(0x273B58, fallback.narcosis() * .05 * pulse)
                );
                ellipseOutside(
                    graphics,
                    width,
                    height,
                    radius,
                    radius,
                    color(0x55401F, fallback.toxicity() * .035 * pulse)
                );
                ellipseOutside(
                    graphics,
                    width,
                    height,
                    radius,
                    radius,
                    color(0x170F17, fallback.decompression() * .05 * pulse)
                );
            }
        }
    }

    /** Draw the blink last so closed eyelids cover the other distress layers. */
    private static void renderEyelids(
        GuiGraphics graphics,
        int width,
        int height,
        float partial,
        double intensity
    ) {
        if (DivingConfig.EYELID_EFFECTS.get()) {
            double closure = eyeClosure(partial) * intensity;

            if (closure > 0) {
                // Asymmetric curved lids meet across the centre during a complete blink.
                double open = 1 - closure;

                for (int x = 0; x < width; x += 4) {
                    double normalizedX = (x + 2.0 - width / 2.0) / (width * .65);

                    double curve = Math.sqrt(Math.max(0, 1 - normalizedX * normalizedX));

                    int top = (int) (height * .5 - height * .60 * open * curve);

                    int bottom = (int) (height * .5 + height * .56 * open * curve);

                    if (top > 0) {
                        graphics.fill(x, 0, Math.min(width, x + 4), top, 0xFF000000);
                        graphics.fill(x, top, Math.min(width, x + 4), top + 2, 0x80000000);
                    }

                    if (bottom < height) {
                        graphics.fill(x, bottom, Math.min(width, x + 4), height, 0xFF000000);
                        graphics.fill(x, bottom - 2, Math.min(width, x + 4), bottom, 0x80000000);
                    }
                }
            }
        }
    }

    private static void ellipseOutside(
        GuiGraphics graphics,
        int width,
        int height,
        double radiusX,
        double radiusY,
        int color
    ) {
        for (int y = 0; y < height; y += 6) {
            double normalizedY = (y + 3.0 - height * .5) / (height * .5 * radiusY);

            double half = width * .5 * radiusX * Math.sqrt(Math.max(0, 1 - normalizedY * normalizedY));

            int left = (int) Math.max(0, width * 0.5 - half);

            int right = (int) Math.min(width, width * 0.5 + half);

            if (left > 0) {
                graphics.fill(0, y, left, Math.min(height, y + 6), color);
            }

            if (right < width) {
                graphics.fill(right, y, width, Math.min(height, y + 6), color);
            }
        }
    }

    private static int color(int rgb, double alpha) {
        return ((int) (Math.clamp(alpha, 0, 1) * 255) << 24) | rgb;
    }

    private DivingVision() {}
}
