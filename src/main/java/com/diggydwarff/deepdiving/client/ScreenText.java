package com.diggydwarff.deepdiving.client;

import net.minecraft.client.gui.Font;

/** Keep clipped labels visibly incomplete instead of silently cutting off their last word. */
final class ScreenText {
    static String fit(Font font, String text, int width) {
        if (font.width(text) <= width) return text;
        String dots = "...";
        int available = width - font.width(dots);
        return available > 0
                ? font.plainSubstrByWidth(text, available) + dots
                : font.plainSubstrByWidth(text, Math.max(0, width));
    }

    private ScreenText() {}
}
