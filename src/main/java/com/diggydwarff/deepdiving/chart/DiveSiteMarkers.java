package com.diggydwarff.deepdiving.chart;

import com.diggydwarff.deepdiving.sign.DivePanelBlockEntity;
import com.diggydwarff.deepdiving.sign.DiveSignDesign;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Maps use signs before the ordinary sign editor, just as maps use banners. */
public final class DiveSiteMarkers {
    public static boolean isFilledDepthMap(ItemStack stack) {
        return stack.getItem() instanceof DepthMapItem && stack.has(DataComponents.MAP_ID);
    }

    @SubscribeEvent
    public static void useSign(PlayerInteractEvent.RightClickBlock event) {
        ItemStack map = event.getItemStack();
        // An empty main hand would otherwise open the sign editor before the offhand gets a turn.
        if (map.isEmpty() && event.getHand() == InteractionHand.MAIN_HAND) {
            map = event.getEntity().getOffhandItem();
        }
        map = MapBundleItem.resolveMap(map);
        BlockEntity sign = event.getLevel().getBlockEntity(event.getPos());
        if (!(map.getItem() instanceof MapItem) || !isSite(sign)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (event.getLevel().isClientSide) return;

        var data = MapItem.getSavedData(map, event.getLevel());
        String message = null;
        if (data == null) message = "message.deepdiving.site.open_map";
        else if (!data.dimension.equals(event.getLevel().dimension()))
            message = "message.deepdiving.site.dimension";
        else if (data.locked) message = "message.deepdiving.site.locked";
        else if (siteName(sign).getString().isBlank())
            message = "message.deepdiving.site.name_required";
        else if (!data.toggleBanner(event.getLevel(), event.getPos()))
            message = "message.deepdiving.site.outside";
        else ((DiveChartData) data).deepdiving$markSite(event.getPos());
        if (message != null)
            event.getEntity().displayClientMessage(Component.translatable(message), true);
    }

    public static boolean isSite(BlockEntity sign) {
        return sign instanceof DiveSiteSignBlockEntity
                || sign instanceof DivePanelBlockEntity panel
                        && panel.design() == DiveSignDesign.DIVE_SITE;
    }

    public static Component siteName(BlockEntity sign) {
        if (sign instanceof DiveSiteSignBlockEntity textSign) return textSign.siteName();
        if (sign instanceof DivePanelBlockEntity panel
                && panel.design() == DiveSignDesign.DIVE_SITE)
            return Component.literal(panel.customCaption());
        return Component.empty();
    }

    private DiveSiteMarkers() {}
}
