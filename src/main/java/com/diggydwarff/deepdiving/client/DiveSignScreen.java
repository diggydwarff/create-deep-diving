package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.sign.DiveSignPackets;
import com.diggydwarff.deepdiving.sign.DiveSignText;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.PacketDistributor;

/** Compact caption editor with the actual panel texture as its live preview. */
public final class DiveSignScreen extends Screen {
    private final DiveSignPackets.View view;
    private EditBox caption;
    private String draft;
    private int color,
            arrow,
            left,
            top,
            panelWidth,
            panelHeight,
            previewSize,
            formLeft,
            formWidth,
            cell;

    public DiveSignScreen(DiveSignPackets.View view) {
        super(Component.translatable("gui.deepdiving.dive_sign.title"));
        this.view = view;
        draft = view.caption();
        color = view.color();
        arrow = view.arrow();
    }

    @Override
    protected void init() {
        if (caption != null) draft = caption.getValue();
        panelWidth = Math.min(360, width - 16);
        panelHeight = Math.min(244, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        previewSize = Math.min(112, (panelWidth - 36) / 3);
        formLeft = left + previewSize + 30;
        formWidth = panelWidth - previewSize - 44;
        caption =
                new EditBox(
                        font,
                        formLeft,
                        top + 52,
                        formWidth,
                        20,
                        Component.translatable("gui.deepdiving.dive_sign.caption"));
        caption.setMaxLength(DiveSignText.MAX_LENGTH);
        caption.setValue(draft);
        addRenderableWidget(caption);
        cell = Math.max(16, Math.min(23, formWidth / 8));
        for (int index = 0; index < 16; index++) {
            int selected = index;
            CreateButton button =
                    new CreateButton(
                            formLeft + (index % 8) * cell,
                            top + 94 + (index / 8) * 22,
                            cell - 2,
                            "",
                            DyeColor.byId(index).getName().replace('_', ' '),
                            () -> color = selected);
            button.setMessage(Component.literal(DyeColor.byId(index).getName().replace('_', ' ')));
            addRenderableWidget(button);
        }
        CreateButton direction =
                new CreateButton(
                        formLeft,
                        top + 143,
                        formWidth,
                        arrowTitle().getString(),
                        Component.translatable("gui.deepdiving.dive_sign.arrow_help").getString(),
                        () -> {});
        direction.withCallback(
                () -> {
                    arrow = (arrow + 1) % 5;
                    direction.setLabel(arrowTitle().getString());
                });
        addRenderableWidget(direction);
        addRenderableWidget(
                new CreateButton(
                        left + panelWidth - 218,
                        top + panelHeight - 28,
                        98,
                        Component.translatable("gui.cancel").getString(),
                        Component.translatable("gui.cancel").getString(),
                        this::onClose));
        addRenderableWidget(
                new CreateButton(
                        left + panelWidth - 112,
                        top + panelHeight - 28,
                        98,
                        Component.translatable("gui.deepdiving.dive_sign.save").getString(),
                        Component.translatable("gui.deepdiving.dive_sign.save_help").getString(),
                        () -> {
                            PacketDistributor.sendToServer(
                                    new DiveSignPackets.Action(
                                            view.pos(),
                                            view.token(),
                                            caption.getValue(),
                                            color,
                                            arrow));
                            onClose();
                        }));
        setInitialFocus(caption);
    }

    private Component arrowTitle() {
        return Component.translatable(
                "gui.deepdiving.dive_sign.arrow",
                Component.translatable("gui.deepdiving.dive_sign.arrow." + arrow));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.renderBackground(graphics, mouseX, mouseY, partial);
        graphics.fill(left - 2, top - 2, left + panelWidth + 2, top + panelHeight + 2, 0xffb7a57c);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xff152d37);
        graphics.fill(left + 2, top + 2, left + panelWidth - 2, top + 27, 0xff254651);
        graphics.drawString(
                font,
                ScreenText.fit(font, title.getString(), panelWidth - 28),
                left + 14,
                top + 10,
                0xffedf0e4,
                false);
        graphics.drawString(
                font,
                Component.translatable("gui.deepdiving.dive_sign.caption"),
                formLeft,
                top + 38,
                0xffceded9,
                false);
        graphics.drawString(
                font,
                Component.translatable("gui.deepdiving.dive_sign.color"),
                formLeft,
                top + 81,
                0xffceded9,
                false);
        graphics.drawWordWrap(
                font,
                Component.translatable("gui.deepdiving.dive_sign.default"),
                formLeft,
                top + 170,
                formWidth,
                0xff9bb5b4);
        String count = caption.getValue().length() + " / " + DiveSignText.MAX_LENGTH;
        graphics.drawString(
                font, count, formLeft + formWidth - font.width(count), top + 81, 0xff9bb5b4, false);
        ResourceLocation texture =
                DeepDiving.id(
                        "textures/block/dive_sign/" + view.design().getSerializedName() + ".png");
        graphics.blit(
                texture,
                left + 14,
                top + 43,
                0,
                0,
                previewSize,
                previewSize,
                previewSize,
                previewSize);
        graphics.pose().pushPose();
        graphics.pose().translate(left + 14 + previewSize / 2f, top + 43 + previewSize / 2f, 1);
        graphics.pose().scale(previewSize / 128f, previewSize / 128f, 1);
        Component shown = Component.literal(caption.getValue());
        DivePanelRenderer.drawCaption(
                font,
                shown,
                color,
                arrow,
                graphics.pose(),
                graphics.bufferSource(),
                LightTexture.FULL_BRIGHT);
        graphics.pose().popPose();
        graphics.flush();
        graphics.drawString(
                font,
                Component.translatable(
                        view.glowing()
                                ? "gui.deepdiving.dive_sign.glowing"
                                : "gui.deepdiving.dive_sign.ink_off"),
                left + 14,
                top + 166,
                0xffc6d9d1,
                false);
        if (!view.glowing())
            graphics.drawString(
                    font,
                    Component.translatable("gui.deepdiving.dive_sign.ink_tip"),
                    left + 14,
                    top + 179,
                    0xff93b1af,
                    false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        for (int index = 0; index < 16; index++) {
            int x = formLeft + (index % 8) * cell, y = top + 94 + (index / 8) * 22;
            graphics.fill(
                    x + 4,
                    y + 4,
                    x + cell - 6,
                    y + 16,
                    0xff000000 | DyeColor.byId(index).getTextureDiffuseColor());
            if (index == color) graphics.renderOutline(x + 2, y + 2, cell - 6, 16, 0xffefe5c8);
        }
    }
}
