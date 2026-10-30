package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.chart.MapBundleMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.MapItem;

/**
 * A real map page and a small chart pocket, framed in the same Create casing as the other tools.
 */
public final class MapBundleScreen extends AbstractContainerScreen<MapBundleMenu> {
    private CreateButton previous, next;

    public MapBundleScreen(MapBundleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 296;
        imageHeight = 238;
        inventoryLabelX = 66;
        inventoryLabelY = 146;
    }

    @Override
    protected void init() {
        super.init();
        previous =
                addRenderableWidget(
                        new CreateButton(
                                leftPos + 174,
                                topPos + 126,
                                24,
                                "<",
                                Component.translatable("gui.deepdiving.bundle.previous")
                                        .getString(),
                                () -> turn(MapBundleMenu.PREVIOUS)));
        next =
                addRenderableWidget(
                        new CreateButton(
                                leftPos + 254,
                                topPos + 126,
                                24,
                                ">",
                                Component.translatable("gui.deepdiving.bundle.next").getString(),
                                () -> turn(MapBundleMenu.NEXT)));
        updateButtons();
    }

    private void turn(int button) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    private void updateButtons() {
        previous.active = menu.pageNumber() > 1;
        next.active = menu.pageNumber() > 0 && menu.pageNumber() < menu.count();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 263 || key == 262) {
            turn(key == 263 ? MapBundleMenu.PREVIOUS : MapBundleMenu.NEXT);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        FieldNotebookStyle.cover(g, leftPos, topPos, imageWidth, imageHeight);
        g.fill(
                leftPos + 10,
                topPos + 19,
                leftPos + imageWidth - 10,
                topPos + 20,
                FieldNotebookStyle.BRASS);
        g.fill(leftPos + 10, topPos + 22, leftPos + 286, topPos + 145, 0xFFB7A383);
        g.fill(leftPos + 12, topPos + 24, leftPos + 160, topPos + 143, 0xFFF3ECD9);
        g.fill(leftPos + 166, topPos + 24, leftPos + 284, topPos + 143, 0xFFE5DCC5);
        g.fill(leftPos + 160, topPos + 24, leftPos + 166, topPos + 143, 0xFF86765E);
        for (var slot : menu.slots)
            FieldNotebookStyle.slot(g, leftPos + slot.x - 1, topPos + slot.y - 1);
        if (menu.count() > 0) {
            var slot = menu.slots.get(menu.selectedSlot());
            g.renderOutline(leftPos + slot.x - 2, topPos + slot.y - 2, 20, 20, 0xFF4DA3A7);
        }
        var map = menu.selectedMap();
        var id = map.get(DataComponents.MAP_ID);
        var data = MapItem.getSavedData(map, minecraft.level);
        if (id != null && data != null) {
            g.enableScissor(leftPos + 30, topPos + 26, leftPos + 142, topPos + 138);
            g.pose().pushPose();
            g.pose().translate(leftPos + 30, topPos + 26, 1);
            // Native map decorations sit at negative Z. Face them toward this GUI, above the map.
            g.pose().scale(.875f, .875f, -1);
            minecraft
                    .gameRenderer
                    .getMapRenderer()
                    .render(g.pose(), g.bufferSource(), id, data, false, LightTexture.FULL_BRIGHT);
            g.flush();
            g.pose().popPose();
            g.disableScissor();
        } else {
            int y = topPos + 68;
            for (var line :
                    font.split(
                            Component.translatable(
                                    menu.count() == 0
                                            ? "gui.deepdiving.bundle.empty"
                                            : "gui.deepdiving.bundle.loading"),
                            118)) {
                g.drawString(font, line, leftPos + 25, y, 0x425258, false);
                y += 12;
            }
        }
        g.drawString(
                font,
                Component.translatable("gui.deepdiving.bundle.maps", menu.count(), 16),
                leftPos + 174,
                topPos + 28,
                0x243F47,
                false);
        if (!map.isEmpty()) {
            String name =
                    map.has(DataComponents.CUSTOM_NAME)
                            ? map.getHoverName().getString()
                            : Component.translatable(
                                            "gui.deepdiving.bundle.map_id",
                                            id == null ? "?" : id.id())
                                    .getString();
            g.drawString(
                    font,
                    ScreenText.fit(font, name, 104),
                    leftPos + 174,
                    topPos + 115,
                    0x425258,
                    false);
        }
        String page =
                Component.translatable(
                                "gui.deepdiving.bundle.page", menu.pageNumber(), menu.count())
                        .getString();
        g.drawString(
                font, page, leftPos + 226 - font.width(page) / 2, topPos + 131, 0x243F47, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(
                font,
                ScreenText.fit(font, title.getString(), imageWidth - 20),
                10,
                6,
                0xE5ECE1,
                false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xC8D0CB, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        if (!menu.selectedMap().isEmpty()
                && mouseX >= leftPos + 170
                && mouseX < leftPos + 282
                && mouseY >= topPos + 113
                && mouseY < topPos + 125) {
            var map = menu.selectedMap();
            var details = new java.util.ArrayList<net.minecraft.util.FormattedCharSequence>();
            details.addAll(font.split(map.getHoverName(), 190));
            var id = map.get(DataComponents.MAP_ID);
            if (id != null)
                details.addAll(
                        font.split(
                                Component.translatable("gui.deepdiving.bundle.map_id", id.id())
                                        .withStyle(net.minecraft.ChatFormatting.GRAY),
                                190));
            var data = MapItem.getSavedData(map, minecraft.level);
            if (data != null)
                details.addAll(
                        font.split(
                                Component.translatable(
                                                "gui.deepdiving.bundle.chart_details",
                                                1 << data.scale,
                                                Component.translatable(
                                                        data.locked
                                                                ? "gui.deepdiving.bundle.locked"
                                                                : "gui.deepdiving.bundle.editable"))
                                        .withStyle(net.minecraft.ChatFormatting.GRAY),
                                190));
            g.renderTooltip(font, details, mouseX, mouseY);
        } else if (hoveredSlot != null && hoveredSlot.index < 16 && !hoveredSlot.hasItem()) {
            g.renderTooltip(
                    font,
                    font.split(Component.translatable("gui.deepdiving.bundle.insert_help"), 190),
                    mouseX,
                    mouseY);
        }
    }
}
