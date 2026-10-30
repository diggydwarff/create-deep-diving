package com.diggydwarff.deepdiving.chart;

import net.minecraft.world.level.saveddata.maps.MapBanner;

/** Identifies depth-map saved data so zooming can retain its surveyed site markers. */
public interface DiveChartData {
    boolean deepdiving$isDepthChart();

    void deepdiving$markDepthChart();

    void deepdiving$putMarker(MapBanner marker);

    /** Keeps site identity separate from ordinary orange banners, including across save/load. */
    void deepdiving$markSite(net.minecraft.core.BlockPos pos);
}
