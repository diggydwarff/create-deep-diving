package com.diggydwarff.deepdiving.chart;

import net.minecraft.world.level.material.MapColor;

/** The chart's vanilla-compatible color bytes; kept separate from world sampling and glyph math. */
public final class ChartPalette {
    public static byte color(int depth, int left, int above) {
        if (depth == 0) return MapColor.STONE.getPackedId(MapColor.Brightness.NORMAL);
        if (ChartMath.contour(depth, left) || ChartMath.contour(depth, above)) {
            return MapColor.COLOR_CYAN.getPackedId(MapColor.Brightness.LOWEST);
        }
        return switch (ChartMath.band(depth)) {
            case 0 -> MapColor.COLOR_LIGHT_BLUE.getPackedId(MapColor.Brightness.HIGH);
            case 1 -> MapColor.COLOR_LIGHT_BLUE.getPackedId(MapColor.Brightness.LOW);
            case 2 -> MapColor.WATER.getPackedId(MapColor.Brightness.HIGH);
            case 3 -> MapColor.WATER.getPackedId(MapColor.Brightness.NORMAL);
            case 4 -> MapColor.WATER.getPackedId(MapColor.Brightness.LOW);
            default -> MapColor.WATER.getPackedId(MapColor.Brightness.LOWEST);
        };
    }

    public static byte label(int underlyingDepth) {
        return underlyingDepth <= 20
                ? MapColor.COLOR_BLACK.getPackedId(MapColor.Brightness.HIGH)
                : MapColor.SNOW.getPackedId(MapColor.Brightness.HIGH);
    }

    private ChartPalette() {}
}
