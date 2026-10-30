package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.common.ModConfigSpec;

/** A small themed entrance; NeoForge still owns config editing, saving and restart handling. */
public final class DivingConfigScreen extends Screen {
    private final Screen parent;
    private final ModContainer mod;
    private final ConfigurationScreen editorOwner;
    private int left, top, panelWidth, panelHeight, firstY, secondY, cardHeight;
    private boolean compact;
    private CreateButton client, world;

    public DivingConfigScreen(ModContainer mod, Screen parent) {
        super(Component.translatable("gui.deepdiving.config.title"));
        this.mod = mod;
        this.parent = parent;
        editorOwner = new ConfigurationScreen(mod, this, DivingConfigSectionScreen::new);
    }

    @Override
    protected void init() {
        compact = width < 400 || height < 300;
        panelWidth = Math.min(420, width - 24);
        panelHeight = Math.min(280, height - 20);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        firstY = compact ? 68 : 82;
        cardHeight = compact ? 48 : 62;
        secondY = firstY + cardHeight + 7;
        client = openButton(ModConfig.Type.CLIENT, firstY);
        world = openButton(ModConfig.Type.SERVER, secondY);
        addRenderableWidget(
                new CreateButton(
                        left + panelWidth - 88,
                        top + panelHeight - 27,
                        72,
                        Component.translatable("gui.done").getString(),
                        Component.translatable("gui.done").getString(),
                        this::onClose));
        updateAvailability();
    }

    private CreateButton openButton(ModConfig.Type type, int y) {
        CreateButton button =
                addRenderableWidget(
                        new CreateButton(
                                left + panelWidth - 80,
                                top + y + (cardHeight - 18) / 2,
                                64,
                                Component.translatable("gui.deepdiving.config.open").getString(),
                                label(type).getString(),
                                () -> open(type)));
        button.setMessage(label(type));
        return button;
    }

    private ModConfig config(ModConfig.Type type) {
        return ModConfigs.getConfigSet(type).stream()
                .filter(c -> c.getModId().equals(mod.getModId()))
                .findFirst()
                .orElse(null);
    }

    /** Match the generic NeoForge screen's local-world and LAN editing restrictions. */
    Component unavailable(ModConfig.Type type) {
        ModConfig config = config(type);
        if (config == null || !((ModConfigSpec) config.getSpec()).isLoaded())
            return Component.translatable(
                    type == ModConfig.Type.SERVER
                            ? "gui.deepdiving.config.world_unloaded"
                            : "gui.deepdiving.config.unloaded");
        if (type == ModConfig.Type.SERVER
                && minecraft.getCurrentServer() != null
                && !minecraft.isSingleplayer())
            return ConfigurationScreen.TOOLTIP_CANNOT_EDIT_THIS_WHILE_ONLINE;
        if (type == ModConfig.Type.SERVER
                && minecraft.hasSingleplayerServer()
                && minecraft.getSingleplayerServer().isPublished())
            return ConfigurationScreen.TOOLTIP_CANNOT_EDIT_THIS_WHILE_OPEN_TO_LAN;
        return null;
    }

    private void updateAvailability() {
        for (ModConfig.Type type :
                new ModConfig.Type[] {ModConfig.Type.CLIENT, ModConfig.Type.SERVER}) {
            CreateButton button = type == ModConfig.Type.CLIENT ? client : world;
            Component reason = unavailable(type);
            button.active = reason == null;
            button.setToolTip(reason == null ? label(type) : reason);
        }
    }

    private void open(ModConfig.Type type) {
        if (unavailable(type) != null) return;
        minecraft.setScreen(
                new DivingConfigSectionScreen(editorOwner, type, config(type), label(type)));
    }

    static Component label(ModConfig.Type type) {
        return Component.translatable(
                type == ModConfig.Type.CLIENT
                        ? "gui.deepdiving.config.client"
                        : "gui.deepdiving.config.world");
    }

    @Override
    public void tick() {
        updateAvailability();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        DivingConfigStyle.backdrop(g, width, height);
        DivingConfigStyle.frame(g, left, top, panelWidth, panelHeight);
        DivingConfigStyle.title(g, title, width / 2, top + 6, panelWidth - 24, 0xFF281C11);
        g.drawString(
                font,
                Component.translatable("gui.deepdiving.config.subtitle"),
                left + 18,
                top + 34,
                DivingConfigStyle.TEXT,
                false);
        g.drawString(
                font,
                Component.translatable("gui.deepdiving.config.description"),
                left + 18,
                top + 49,
                DivingConfigStyle.MUTED,
                false);
        card(g, client, firstY, ModConfig.Type.CLIENT, new ItemStack(DeepDiving.COMPUTER.get()));
        card(
                g,
                world,
                secondY,
                ModConfig.Type.SERVER,
                new ItemStack(DeepDiving.COMPRESSOR.get().asItem()));
        if (!compact) DivingConfigStyle.depthMarks(g, left + panelWidth - 14, top + 30, 34);
        int statusY = top + panelHeight - 45;
        g.fill(left + 18, statusY + 2, left + 21, statusY + 5, DivingConfigStyle.ACCENT);
        g.drawString(font, "Create", left + 26, statusY, DivingConfigStyle.MUTED, false);
        boolean deepSeas = ModList.get().isLoaded("create_submarine");
        g.fill(
                left + 77,
                statusY + 2,
                left + 80,
                statusY + 5,
                deepSeas ? DivingConfigStyle.ACCENT : 0xFF52686D);
        g.drawString(
                font,
                Component.translatable(
                        deepSeas
                                ? "gui.deepdiving.config.deep_seas_installed"
                                : "gui.deepdiving.config.deep_seas_optional"),
                left + 85,
                statusY,
                DivingConfigStyle.MUTED,
                false);
    }

    private void card(
            GuiGraphics g, CreateButton button, int y, ModConfig.Type type, ItemStack icon) {
        int yy = top + y;
        g.fill(left + 14, yy, left + panelWidth - 14, yy + cardHeight, DivingConfigStyle.PANEL);
        int border = button.isHoveredOrFocused() ? DivingConfigStyle.ACCENT : 0xFF49636A;
        g.fill(left + 14, yy, left + panelWidth - 14, yy + 1, border);
        g.fill(left + 14, yy + cardHeight - 1, left + panelWidth - 14, yy + cardHeight, 0xFF25434C);
        g.fill(left + 14, yy + 5, left + 16, yy + cardHeight - 5, DivingConfigStyle.BRASS);
        int scale = compact ? 1 : 2;
        g.pose().pushPose();
        g.pose().translate(left + 23, yy + (cardHeight - 16 * scale) / 2f, 0);
        g.pose().scale(scale, scale, 1);
        g.renderItem(icon, 0, 0);
        g.pose().popPose();
        int x = compact ? 48 : 65;
        int labelWidth = panelWidth - x - 91;
        g.drawString(
                font,
                ScreenText.fit(font, label(type).getString(), labelWidth),
                left + x,
                yy + 10,
                button.active ? DivingConfigStyle.TEXT : DivingConfigStyle.MUTED,
                false);
        Component description =
                Component.translatable(
                        type == ModConfig.Type.CLIENT
                                ? "gui.deepdiving.config.client_description"
                                : "gui.deepdiving.config.world_description");
        if (!button.active && type == ModConfig.Type.SERVER)
            description = Component.translatable("gui.deepdiving.config.world_locked");
        g.drawString(
                font,
                ScreenText.fit(font, description.getString(), labelWidth),
                left + x,
                yy + 25,
                DivingConfigStyle.MUTED,
                false);
    }
}
