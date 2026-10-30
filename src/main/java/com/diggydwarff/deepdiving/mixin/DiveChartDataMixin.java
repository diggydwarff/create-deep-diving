package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.chart.DiveChartData;
import com.diggydwarff.deepdiving.chart.DiveSiteMarkers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Keeps depth charts identifiable after save/load, zooming and locking. */
@Mixin(MapItemSavedData.class)
public abstract class DiveChartDataMixin implements DiveChartData {
    @Shadow @Final private Map<String, MapBanner> bannerMarkers;

    @Shadow
    private void removeDecoration(String id) {
        throw new AssertionError();
    }

    @Unique private boolean deepdiving$depthChart;
    @Unique private long deepdiving$lastMarkerCheck = Long.MIN_VALUE;
    @Unique private final Set<BlockPos> deepdiving$sites = new HashSet<>();

    @Override
    public void deepdiving$markSite(BlockPos pos) {
        MapBanner marker =
                bannerMarkers.get("banner-" + pos.getX() + "," + pos.getY() + "," + pos.getZ());
        if (marker == null) deepdiving$sites.remove(pos);
        else {
            deepdiving$sites.add(pos.immutable());
            deepdiving$putMarker(marker);
        }
        ((MapItemSavedData) (Object) this).setDirty();
    }

    @Override
    public boolean deepdiving$isDepthChart() {
        return deepdiving$depthChart;
    }

    @Override
    public void deepdiving$markDepthChart() {
        if (!deepdiving$depthChart) {
            deepdiving$depthChart = true;
            ((MapItemSavedData) (Object) this).setDirty();
        }
    }

    @Override
    public void deepdiving$putMarker(MapBanner marker) {
        MapItemSavedData data = (MapItemSavedData) (Object) this;
        bannerMarkers.put(marker.getId(), marker);
        data.addDecoration(
                deepdiving$sites.contains(marker.pos())
                        ? DeepDiving.SITE_MARKER
                        : marker.getDecoration(),
                null,
                marker.getId(),
                marker.pos().getX() + .5,
                marker.pos().getZ() + .5,
                deepdiving$sites.contains(marker.pos()) ? 0 : 180,
                marker.name().orElse(null));
        data.setDirty();
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void deepdiving$saveKind(
            CompoundTag tag,
            HolderLookup.Provider registries,
            CallbackInfoReturnable<CompoundTag> callback) {
        if (deepdiving$depthChart)
            callback.getReturnValue().putBoolean("deepdiving_depth_chart", true);
        deepdiving$sites.removeIf(
                pos ->
                        !bannerMarkers.containsKey(
                                "banner-" + pos.getX() + "," + pos.getY() + "," + pos.getZ()));
        callback.getReturnValue()
                .putLongArray(
                        "deepdiving_site_markers",
                        deepdiving$sites.stream().mapToLong(BlockPos::asLong).toArray());
    }

    @Inject(method = "load", at = @At("RETURN"))
    private static void deepdiving$loadKind(
            CompoundTag tag,
            HolderLookup.Provider registries,
            CallbackInfoReturnable<MapItemSavedData> callback) {
        if (tag.getBoolean("deepdiving_depth_chart"))
            ((DiveChartData) callback.getReturnValue()).deepdiving$markDepthChart();
        var sites = tag.getLongArray("deepdiving_site_markers");
        for (int i = 0; i < Math.min(256, sites.length); i++)
            ((DiveChartData) callback.getReturnValue()).deepdiving$markSite(BlockPos.of(sites[i]));
    }

    @Inject(method = "scaled", at = @At("RETURN"))
    private void deepdiving$scaleMarkers(CallbackInfoReturnable<MapItemSavedData> callback) {
        if (!deepdiving$depthChart) return;
        MapItemSavedData result = callback.getReturnValue();
        ((DiveChartData) result).deepdiving$markDepthChart();
        for (MapBanner marker : bannerMarkers.values()) {
            double x = marker.pos().getX() + .5, z = marker.pos().getZ() + .5;
            int size = 1 << result.scale;
            if (Math.abs(x - result.centerX) <= 63 * size
                    && Math.abs(z - result.centerZ) <= 63 * size) {
                ((DiveChartData) result).deepdiving$putMarker(marker);
                if (deepdiving$sites.contains(marker.pos()))
                    ((DiveChartData) result).deepdiving$markSite(marker.pos());
            }
        }
    }

    @Inject(method = "locked", at = @At("RETURN"))
    private void deepdiving$lockKind(CallbackInfoReturnable<MapItemSavedData> callback) {
        if (deepdiving$depthChart)
            ((DiveChartData) callback.getReturnValue()).deepdiving$markDepthChart();
        for (BlockPos pos : deepdiving$sites)
            ((DiveChartData) callback.getReturnValue()).deepdiving$markSite(pos);
    }

    @Inject(method = "tickCarriedBy", at = @At("HEAD"))
    private void deepdiving$refreshMarkers(Player player, ItemStack stack, CallbackInfo callback) {
        var world = player.level();
        MapItemSavedData data = (MapItemSavedData) (Object) this;
        if (world.isClientSide || !data.dimension.equals(world.dimension())) return;
        long now = world.getGameTime();
        if (deepdiving$lastMarkerCheck != Long.MIN_VALUE
                && now >= deepdiving$lastMarkerCheck
                && now - deepdiving$lastMarkerCheck < 20) return;
        deepdiving$lastMarkerCheck = now;
        // The vanilla 256-marker limit bounds this loop. Never load a marker's chunk to check it.
        for (MapBanner marker : List.copyOf(bannerMarkers.values())) {
            if (!world.hasChunkAt(marker.pos())) continue;
            if (data.locked) {
                // Upgrade old icons while preserving the saved names on locked snapshots.
                if (!deepdiving$sites.contains(marker.pos())
                        && DiveSiteMarkers.isSite(world.getBlockEntity(marker.pos())))
                    deepdiving$markSite(marker.pos());
                continue;
            }
            MapBanner current = MapBanner.fromWorld(world, marker.pos());
            boolean site = DiveSiteMarkers.isSite(world.getBlockEntity(marker.pos()));
            if (marker.equals(current) && site == deepdiving$sites.contains(marker.pos())) continue;
            if (current != null && site) {
                deepdiving$putMarker(current);
                deepdiving$markSite(current.pos());
            } else {
                bannerMarkers.remove(marker.getId());
                deepdiving$sites.remove(marker.pos());
                removeDecoration(marker.getId());
            }
        }
    }

    @Inject(method = "checkBanners", at = @At("HEAD"))
    private void deepdiving$renameSite(BlockGetter world, int x, int z, CallbackInfo callback) {
        for (MapBanner previous : List.copyOf(bannerMarkers.values())) {
            if (previous.pos().getX() != x
                    || previous.pos().getZ() != z
                    || !DiveSiteMarkers.isSite(world.getBlockEntity(previous.pos()))) continue;
            MapBanner current = MapBanner.fromWorld(world, previous.pos());
            if (current != null
                    && (!current.equals(previous) || !deepdiving$sites.contains(current.pos()))) {
                deepdiving$putMarker(current);
                deepdiving$markSite(current.pos());
            }
        }
    }

    @Inject(method = "toggleBanner", at = @At("HEAD"))
    private void deepdiving$ordinaryBanner(
            net.minecraft.world.level.LevelAccessor world,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> callback) {
        if (!DiveSiteMarkers.isSite(world.getBlockEntity(pos))) deepdiving$sites.remove(pos);
    }
}
