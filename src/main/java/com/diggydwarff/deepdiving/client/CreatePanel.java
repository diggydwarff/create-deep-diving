package com.diggydwarff.deepdiving.client;

import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.minecraft.client.gui.GuiGraphics;

/** Draw directly from Create's stock-keeper texture; no copied or substitute GUI assets. */
final class CreatePanel {
    static final int HEADER_OVERHANG = 8;

    static void draw(
        GuiGraphics g,
        int x,
        int y,
        int width,
        int height
    ) {
        piece(g, x - HEADER_OVERHANG, y, width + HEADER_OVERHANG * 2, 18, 16, 0, 224, 18);

        for (int offset = 18; offset < height - 10; offset += 20) {
            int h = Math.min(20, height - 10 - offset);

            piece(g, x, y + offset, width, h, 24, 48, 208, h);
        }

        piece(g, x, y + height - 10, width, 10, 24, 150, 208, 10);
        // Keep Create's casing; a solid inset stops the texture stripes competing with small text.
        g.fill(x + 7, y + 19, x + width - 7, y + height - 12, 0xFF25282B);
    }

    static void slot(GuiGraphics g, int x, int y) {
        AllGuiTextures.STOCK_KEEPER_REQUEST_SLOT.render(g, x, y);
    }

    private static void piece(
        GuiGraphics g,
        int x,
        int y,
        int w,
        int h,
        int u,
        int v,
        int sw,
        int sh
    ) {
        g.blit(
            AllGuiTextures.STOCK_KEEPER_REQUEST_BODY.location,
            x,
            y,
            w,
            h,
            u,
            v,
            sw,
            sh,
            256,
            256
        );
    }

    private CreatePanel() {}
}
