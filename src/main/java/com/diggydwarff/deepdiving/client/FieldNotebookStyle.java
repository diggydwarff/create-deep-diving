package com.diggydwarff.deepdiving.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Quiet stitched covers and flush metal fittings for the chart case and waterproof dive journal.
 */
final class FieldNotebookStyle {
    static final int COVER = 0xFF193B43, EDGE = 0xFF0D252E, BRASS = 0xFFB49969;

    static void cover(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x60000000);
        g.fill(x, y, x + width, y + height, EDGE);
        g.fill(x + 2, y + 2, x + width - 2, y + height - 2, COVER);
        g.fill(x + 3, y + 3, x + width - 3, y + 4, 0xFF3B6063);
        for (int xx = x + 8; xx < x + width - 8; xx += 6) {
            g.fill(xx, y + 5, xx + 2, y + 6, 0xFF68817A);
            g.fill(xx, y + height - 6, xx + 2, y + height - 5, 0xFF68817A);
        }
        for (int yy = y + 9; yy < y + height - 8; yy += 6)
            for (int xx : new int[] {x + 5, x + width - 6})
                g.fill(xx, yy, xx + 1, yy + 2, 0xFF68817A);
        for (int xx : new int[] {x + 3, x + width - 9})
            for (int yy : new int[] {y + 3, y + height - 6}) g.fill(xx, yy, xx + 6, yy + 3, BRASS);
    }

    static void slot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF0F262C);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF29474D);
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFF57706D);
    }

    private FieldNotebookStyle() {}
}
