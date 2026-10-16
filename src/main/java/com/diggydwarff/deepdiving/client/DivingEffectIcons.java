package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.physiology.DivingEffects;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

final class DivingEffectIcons {
    static void register(RegisterClientExtensionsEvent event) {
        // Vanilla resolves each registered condition from textures/mob_effect/<id>.png. Keep the
        // recovery/severity text while allowing the atlas renderer to draw its icon.
        for (var effect : DivingEffects.ALL) {
            event.registerMobEffect(
                new IClientMobEffectExtensions() {
                    @Override
                    public boolean renderInventoryText(
                        MobEffectInstance instance,
                        EffectRenderingInventoryScreen<?> screen,
                        GuiGraphics g,
                        int x,
                        int y,
                        int z
                    ) {
                        var font = net.minecraft.client.Minecraft.getInstance().font;

                        DivingRecovery.track(instance, screen, x, y);

                        String key = instance.getEffect().value().getDescriptionId() + ".short";

                        String name = net.minecraft.network.chat.Component.translatable(key)
                            .getString();

                        boolean blackout = instance.getEffect().equals(DivingEffects.BLACKOUT);

                        String severity = blackout
                                ? "Unconscious"
                                : instance.getAmplifier() >= 3
                                        ? "Critical"
                                        : instance.getAmplifier() >= 2
                                                ? "Severe"
                                                : instance.getAmplifier() == 1
                                                        ? "Moderate"
                                                        : "Mild";

                        g.drawString(
                            font,
                            font.plainSubstrByWidth(name, 88),
                            x + 28,
                            y + 6,
                            0xFFFFFF,
                            true
                        );
                        g.drawString(
                            font,
                            severity,
                            x + 28,
                            y + 16,
                            blackout || instance.getAmplifier() >= 2 ? 0xFFCD89 : 0xAFBDC6,
                            false
                        );
                        return true;
                    }

                    @Override
                    public boolean renderInventoryIcon(
                        MobEffectInstance instance,
                        EffectRenderingInventoryScreen<?> screen,
                        GuiGraphics g,
                        int x,
                        int y,
                        int z
                    ) {
                        return false; // Let Minecraft draw this condition's atlas sprite.
                    }

                    @Override
                    public boolean renderGuiIcon(
                        MobEffectInstance instance,
                        Gui gui,
                        GuiGraphics g,
                        int x,
                        int y,
                        float z,
                        float alpha
                    ) {
                        return false; // Preserve the HUD's standard positioning and fade alpha.
                    }
                },
                effect.value()
            );
        }
    }

    private DivingEffectIcons() {}
}
