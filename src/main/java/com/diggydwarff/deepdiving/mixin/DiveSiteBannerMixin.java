package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.chart.DiveSiteMarkers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.saveddata.maps.MapBanner;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Reuses vanilla marker serialization, limits, network packets, labels and frame rendering. */
@Mixin(MapBanner.class)
public abstract class DiveSiteBannerMixin {
    @Inject(method = "fromWorld", at = @At("HEAD"), cancellable = true)
    private static void deepdiving$siteSign(
            BlockGetter world, BlockPos pos, CallbackInfoReturnable<MapBanner> callback) {
        if (DiveSiteMarkers.isSite(world.getBlockEntity(pos))) {
            var name = DiveSiteMarkers.siteName(world.getBlockEntity(pos));
            callback.setReturnValue(
                    name.getString().isBlank()
                            ? null
                            : new MapBanner(pos.immutable(), DyeColor.ORANGE, Optional.of(name)));
        }
    }
}
