package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.physiology.DivingEffects;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.GatherEffectScreenTooltipsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

/** The computer guide and inventory tooltips use the same concise recovery explanation. */
@EventBusSubscriber(
    modid = DeepDiving.MOD_ID,
    value = Dist.CLIENT
)
public final class DivingRecovery {
    private record Hover(
        EffectRenderingInventoryScreen<?> screen,
        MobEffectInstance effect,
        int x,
        int y,
        int height
    ) {}

    private static final List<Hover> hovers = new ArrayList<>();

    public static List<Component> description(Holder<MobEffect> effect) {
        String key = effect.value().getDescriptionId();

        return List.of(
            Component.translatable(key + ".cause").withStyle(ChatFormatting.GRAY),
            Component.translatable(key + ".recovery").withStyle(ChatFormatting.WHITE)
        );
    }

    static void track(
        MobEffectInstance effect,
        EffectRenderingInventoryScreen<?> screen,
        int x,
        int y
    ) {
        int count = Minecraft.getInstance().player.getActiveEffects().size();

        hovers.add(new Hover(screen, effect, x, y, count > 5 ? 132 / (count - 1) : 33));
    }

    @SubscribeEvent
    public static void compactTooltip(GatherEffectScreenTooltipsEvent event) {
        var effect = event.getEffectInstance().getEffect();

        if (!DivingEffects.ALL.contains(effect)) {
            return;
        }

        // Infinite effects recover from physiology, rather than counting down an ordinary timer.
        if (event.getEffectInstance().isInfiniteDuration() && event.getTooltip().size() > 1) {
            event.getTooltip().remove(1);
        }

        event.getTooltip().addAll(description(effect));
    }

    @SubscribeEvent
    public static void wideTooltip(ScreenEvent.Render.Post event) {
        for (var hover : hovers) {
            if (event.getScreen() != hover.screen
                || event.getMouseX() < hover.x
                || event.getMouseX() >= hover.x + 120
                || event.getMouseY() < hover.y
                || event.getMouseY() >= hover.y + hover.height) {
                continue;
            }

            var font = Minecraft.getInstance().font;

            var lines = new ArrayList<net.minecraft.util.FormattedCharSequence>();

            lines.add(hover.effect.getEffect().value().getDisplayName().getVisualOrderText());

            for (var text : description(hover.effect.getEffect())) {
                lines.addAll(font.split(text, 240));
            }

            event.getGuiGraphics().renderTooltip(font, lines, event.getMouseX(), event.getMouseY());
            break;
        }

        hovers.clear();
    }

    private DivingRecovery() {}
}
