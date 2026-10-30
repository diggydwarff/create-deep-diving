package com.diggydwarff.deepdiving.mixin;

import com.diggydwarff.deepdiving.chart.DepthMapItem;
import com.diggydwarff.deepdiving.chart.MapBundleItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/** Vanilla map tracking must recognize charts carried inside a bundle. */
@Mixin(MapItemSavedData.class)
abstract class MapBundleTrackingMixin {
    @Inject(method = "mapMatcher", at = @At("RETURN"), cancellable = true)
    private static void deepdiving$bundleMaps(
            ItemStack map, CallbackInfoReturnable<Predicate<ItemStack>> callback) {
        if (map.getItem() instanceof DepthMapItem) {
            var id = map.get(DataComponents.MAP_ID);
            callback.setReturnValue(
                    callback.getReturnValue().or(stack -> MapBundleItem.containsMap(stack, id)));
        }
    }
}
