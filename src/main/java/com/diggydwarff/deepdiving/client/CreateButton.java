package com.diggydwarff.deepdiving.client;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Create's real button chrome, sized to a short label. */
final class CreateButton extends IconButton {
    private String label;
    private boolean chosen;

    CreateButton(int x, int y, int width, String label, String tooltip, Runnable callback) {
        super(x, y, width, 18, AllIcons.I_CONFIRM);
        this.label = label;
        setMessage(Component.literal(label));
        withCallback(callback);
        setToolTip(Component.literal(tooltip));
    }

    void setLabel(String text) {
        label = text;
        setMessage(Component.literal(text));
    }

    void setChosen(boolean value) {
        chosen = value;
    }

    @Override
    public void doRender(GuiGraphics g, int mx, int my, float partial) {
        AllGuiTextures bg =
                !active
                        ? AllGuiTextures.BUTTON_DISABLED
                        : isHoveredOrFocused()
                                ? AllGuiTextures.BUTTON_HOVER
                                : AllGuiTextures.BUTTON;

        drawBg(g, bg);

        var font = Minecraft.getInstance().font;

        String visible = ScreenText.fit(font, label, getWidth() - 6);

        g.drawString(
                font,
                visible,
                getX() + (getWidth() - font.width(visible)) / 2,
                getY() + 5,
                active ? 0x181B1D : 0x525659,
                false);
        if (chosen) {
            g.fill(
                    getX() + 3,
                    getY() + 15,
                    getX() + getWidth() - 3,
                    getY() + 17,
                    active ? 0xFF377C73 : 0xFF626D69);
        }
    }

    @Override
    protected void drawBg(GuiGraphics g, AllGuiTextures t) {
        g.blit(t.location, getX(), getY(), t.getStartX(), t.getStartY(), 2, 18);

        for (int x = 2; x < getWidth() - 2; x++) {
            g.blit(t.location, getX() + x, getY(), t.getStartX() + 8, t.getStartY(), 1, 18);
        }

        g.blit(
                t.location,
                getX() + getWidth() - 2,
                getY(),
                t.getStartX() + 16,
                t.getStartY(),
                2,
                18);
    }
}
