package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.gas.GasMix;
import com.diggydwarff.deepdiving.network.DivingPackets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Create casing around an emissive-looking black readout; shared by the screen and HUD. */
final class OledDisplay {
    static final int WHITE = 16317695;

    static final int LABEL = 11517382;

    static final int ALERT = 16766071;

    static final int HUD_WIDTH = 192;

    static final int HUD_HEIGHT = 156;

    static final int COMPACT_WIDTH = 168;

    static final int COMPACT_HEIGHT = 112;

    record Placement(int x, int y, int width, int height, float scale) {}

    static Placement placement(int guiWidth, int guiHeight, boolean compact, float requestedScale,
            int offsetX, int offsetY) {
        if (guiWidth < 1 || guiHeight < 1) return new Placement(0, 0, 0, 0, 0);
        int width = (compact ? COMPACT_WIDTH : HUD_WIDTH) + CreatePanel.HEADER_OVERHANG * 2;
        int height = compact ? COMPACT_HEIGHT : HUD_HEIGHT;
        float scale = Math.min(requestedScale,
            Math.min(guiWidth / (float) width, guiHeight / (float) height));
        int scaledWidth = Math.min(guiWidth, (int) Math.ceil(width * scale));
        int scaledHeight = Math.min(guiHeight, (int) Math.ceil(height * scale));
        return new Placement(Math.clamp(offsetX, 0, guiWidth - scaledWidth),
            Math.clamp(offsetY, 0, guiHeight - scaledHeight), scaledWidth, scaledHeight, scale);
    }

    static void hud(GuiGraphics g, DivingPackets.Snapshot s) {
        boolean compact = DivingConfig.COMPACT_HUD.get();
        var p = placement(g.guiWidth(), g.guiHeight(), compact, DivingConfig.HUD_SIZE.get().scale,
            DivingConfig.HUD_X.get(), DivingConfig.HUD_Y.get());
        if (p.scale() <= 0) return;
        g.pose().pushPose();
        g.pose().translate(p.x() + CreatePanel.HEADER_OVERHANG * p.scale(), p.y(), 0);
        g.pose().scale(p.scale(), p.scale(), 1);
        if (compact) compactHud(g, s);
        else hud(g, 0, 0, s);
        g.pose().popPose();
    }

    private static void compactHud(GuiGraphics g, DivingPackets.Snapshot s) {
        frame(g, 0, 0, COMPACT_WIDTH, COMPACT_HEIGHT);
        text(g, "DEPTH / BLOCKS", 12, 9, 96, LABEL);
        text(g, "ATM", 118, 9, 38, LABEL);
        fittedValue(g, DiveReadout.format("%.1f", s.depth()), 12, 22, 1.45f, 96, WHITE);
        fittedValue(g, DiveReadout.format("%.1f", s.pressure()), 118, 24, 1.1f, 38, WHITE);
        text(g, "GAS " + DiveReadout.gasTime(s), 12, 42, 88, WHITE);
        fittedValue(g, s.ceiling() > 0 ? DiveReadout.format("STOP %.0fb", s.ceiling())
                : "NDL " + (s.ndl() >= 3600 ? "60m+" : s.ndl() / 60 + "m"),
            101, 42, 1, 55, s.ceiling() > 0 ? ALERT : WHITE);
        text(g, DiveReadout.hudSecondary(s),
            12, 55, 88, s.ceiling() > 0 ? ALERT : LABEL);
        fittedValue(g, DiveReadout.format("%+.1f b/s", s.ascent()), 105, 55, 1, 51, WHITE);
        text(g, DiveReadout.compactAscentStatus(s), 12, 68, 144,
            (s.warnings() & com.diggydwarff.deepdiving.diving.DiverService.ASCENT) != 0
                || s.ceiling() > 0 ? ALERT : LABEL);
        ascentBar(g, 12, 81, 144, s);
        rule(g, 12, 87, 144);
        text(g, DiveReadout.hudWarning(s), 12, 92, 144,
            s.warnings() == 0 ? LABEL : ALERT);
    }

    private static void fittedValue(GuiGraphics g, String text, int x, int y,
            float maximumScale, int width, int color) {
        var font = Minecraft.getInstance().font;
        float scale = Math.min(maximumScale, width / (float) Math.max(1, font.width(text)));
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    static void frame(
        GuiGraphics g,
        int x,
        int y,
        int w,
        int h
    ) {
        CreatePanel.draw(g, x, y, w, h);
        g.fill(x + 5, y + 3, x + w - 5, y + h - 8, 0xFF050708);
        g.fill(x + 8, y + 6, x + w - 8, y + h - 11, 0xFF000000);
    }

    static void text(
        GuiGraphics g,
        String text,
        int x,
        int y,
        int width,
        int color
    ) {
        var font = Minecraft.getInstance().font;

        g.drawString(font, font.plainSubstrByWidth(text, width), x, y, color, false);
    }

    static void value(
        GuiGraphics g,
        String text,
        int x,
        int y,
        float scale,
        int width
    ) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        text(g, text, 0, 0, (int) (width / scale), WHITE);
        g.pose().popPose();
    }

    static void rule(
        GuiGraphics g,
        int x,
        int y,
        int width
    ) {
        g.fill(x, y, x + width, y + 1, 0xFF28343C);
    }

    static void ascentBar(
        GuiGraphics g,
        int x,
        int y,
        int width,
        DivingPackets.Snapshot s
    ) {
        g.fill(x, y, x + width, y + 5, 0xFF26313A);

        double fraction =
            com.diggydwarff.deepdiving.physiology.DiveComputerTools.ascentFraction(s.ascent(), s.safeAscent());

        g.fill(
            x,
            y,
            x + (int) (width * fraction / 2),
            y + 5,
            (s.warnings() & com.diggydwarff.deepdiving.diving.DiverService.ASCENT) != 0
                    ? 0xFFFFD477 : 0xFF99C9C6
        );
        g.fill(x + width / 2, y - 2, x + width / 2 + 1, y + 7, 0xFFF8FCFF);
    }

    static void hud(
        GuiGraphics g,
        int x,
        int y,
        DivingPackets.Snapshot s
    ) {
        frame(g, x, y, HUD_WIDTH, HUD_HEIGHT);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        text(g, "DIVE COMPUTER", 12, 9, 170, WHITE);
        rule(g, 12, 23, 168);
        text(g, "DEPTH / BLOCKS", 12, 29, 105, LABEL);
        text(g, "ATM", 137, 29, 40, LABEL);
        value(g, DiveReadout.format("%.1f", s.depth()), 12, 42, 1.6f, 112);
        value(g, DiveReadout.format("%.1f", s.pressure()), 137, 44, 1.2f, 45);
        text(g, "GAS TIME " + DiveReadout.gasTime(s), 12, 62, 168, WHITE);
        text(
            g,
            s.ceiling() > 0
                    ? "NEXT " + DiveReadout.stopTime(s)
                    : new GasMix(s.oxygen(), s.helium()).name(),
            12,
            77,
            168,
            s.ceiling() > 0 ? ALERT : LABEL
        );
        text(
            g,
            s.ceiling() > 0
                    ? DiveReadout.format("STOP %.0f blocks", s.ceiling())
                    : "NDL " + (s.ndl() >= 3600 ? "60m+" : s.ndl() / 60 + "m"),
            12,
            92,
            95,
            WHITE
        );
        text(g, DiveReadout.format("%+.1f b/s", s.ascent()), 118, 92, 65, WHITE);
        ascentBar(g, 12, 106, 168, s);
        text(
            g,
            DiveReadout.ascentStatus(s),
            12,
            118,
            168,
            (s.warnings() & com.diggydwarff.deepdiving.diving.DiverService.ASCENT) != 0
                    || s.ceiling() > 0 ? ALERT : LABEL
        );

        var warnings = DiveReadout.warnings(s);

        rule(g, 12, 130, 168);
        text(
            g,
            warnings.isEmpty() ? "STATUS NORMAL" : warnings.getFirst(),
            12,
            136,
            168,
            warnings.isEmpty() ? LABEL : ALERT
        );
        g.pose().popPose();
    }

    private OledDisplay() {}
}
