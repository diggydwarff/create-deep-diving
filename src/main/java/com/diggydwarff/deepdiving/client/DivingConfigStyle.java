package com.diggydwarff.deepdiving.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Original nautical trim around Create's installed casing texture. */
final class DivingConfigStyle {
    static final int INK = 0xFF102830;
    static final int PANEL = 0xFF17343D;
    static final int BRASS = 0xFFBDA477;
    static final int TEXT = 0xFFE0EBE6;
    static final int MUTED = 0xFF99B5B7;
    static final int ACCENT = 0xFF6DAEA5;

    static void backdrop(GuiGraphics g, int width, int height) {
        g.fillGradient(0, 0, width, height, 0xFF142932, 0xFF08151D);
        for (int y = 16; y < height; y += 24) g.fill(0, y, width, y + 1, 0x0B71A4A7);
    }

    static void frame(GuiGraphics g, int x, int y, int width, int height) {
        CreatePanel.draw(g, x, y, width, height);
        g.fill(x + 3, y + 20, x + width - 3, y + height - 10, INK);
        g.fill(x + 3, y + 20, x + width - 3, y + 21, BRASS);
        for (int xx : new int[] {x + 7, x + width - 9})
            for (int yy : new int[] {y + 24, y + height - 17}) {
                g.fill(xx, yy, xx + 3, yy + 3, 0xFF524C3D);
                g.fill(xx, yy, xx + 2, yy + 2, BRASS);
            }
    }

    static void title(GuiGraphics g, Component title, int centerX, int y, int maxWidth, int color) {
        var font = Minecraft.getInstance().font;
        String label = ScreenText.fit(font, title.getString(), maxWidth);
        g.drawString(font, label, centerX - font.width(label) / 2, y, color, false);
    }

    static void depthMarks(GuiGraphics g, int x, int y, int height) {
        g.fill(x, y, x + 1, y + height, 0xFF34505A);
        for (int offset = 0; offset <= height; offset += 8) {
            int length = offset % 32 == 0 ? 7 : 3;
            g.fill(
                    x - length,
                    y + offset,
                    x,
                    y + offset + 1,
                    offset % 32 == 0 ? BRASS : 0xFF52777C);
        }
    }

    static void editor(GuiGraphics g, int width, int height, Component section) {
        backdrop(g, width, height);
        int panelWidth = Math.min(436, width);
        frame(g, (width - panelWidth) / 2, 5, panelWidth, height - 10);
        title(
                g,
                Component.translatable("gui.deepdiving.config.title"),
                width / 2,
                11,
                panelWidth - 24,
                0xFF281C11);
        title(g, section, width / 2, 34, panelWidth - 24, TEXT);
        if (width >= 400)
            depthMarks(g, (width - panelWidth) / 2 + 18, 59, Math.max(32, height - 114));
    }

    private DivingConfigStyle() {}
}
