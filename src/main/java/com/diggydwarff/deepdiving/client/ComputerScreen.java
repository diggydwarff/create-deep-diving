package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.gas.GasMix;
import com.diggydwarff.deepdiving.menu.ComputerMenu;
import com.diggydwarff.deepdiving.network.DivingPackets;
import com.diggydwarff.deepdiving.physiology.DivingEffects;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Live instruments and a separate recovery guide keep essential readings easy to scan. */
public final class ComputerScreen extends AbstractSimiContainerScreen<ComputerMenu> {
    private int warningPage;

    private int helpPage;

    private boolean help;

    private boolean compact;

    private CreateButton previous;

    private CreateButton next;

    private CreateButton alarm;

    private CreateButton guide;

    public ComputerScreen(ComputerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        setWindowSize(320, 260);
    }

    @Override
    protected void init() {
        compact = height < 284;
        setWindowSize(320, compact ? 236 : 260);
        super.init();
        previous =
                new CreateButton(
                        leftPos + 268,
                        topPos + 8,
                        18,
                        "<",
                        "Previous page",
                        () -> {
                            if (help) {
                                helpPage = Math.max(0, helpPage - 1);
                            } else {
                                warningPage = Math.max(0, warningPage - 1);
                            }
                        });
        next =
                new CreateButton(
                        leftPos + 290,
                        topPos + 8,
                        18,
                        ">",
                        "Next page",
                        () -> {
                            if (help) {
                                helpPage++;
                            } else {
                                warningPage++;
                            }
                        });
        alarm =
                new CreateButton(
                        leftPos + 178,
                        topPos + 8,
                        38,
                        "SND",
                        "Toggle computer alarms",
                        () -> {
                            DivingConfig.COMPUTER_ALARMS.set(!DivingConfig.COMPUTER_ALARMS.get());
                            DivingConfig.CLIENT.save();
                        });
        guide =
                new CreateButton(
                        leftPos + 220,
                        topPos + 8,
                        44,
                        "HELP",
                        "Condition causes and recovery",
                        () -> help = !help);
        addRenderableWidgets(previous, next, alarm, guide);
        next.visible = false;
        previous.visible = false;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        OledDisplay.frame(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        renderHeader(graphics);

        if (help) {
            renderHelp(graphics);
            return;
        }

        var snapshot = ClientRegistration.snapshot;

        if (snapshot == null) {
            next.visible = false;
            previous.visible = false;
            OledDisplay.text(
                    graphics, "Waiting for diver state...", 14, 48, 292, OledDisplay.LABEL);
            return;
        }

        renderDepthAndGas(graphics, snapshot);

        renderAscent(graphics, snapshot);

        var warnings = DiveReadout.warnings(snapshot);

        int pages = Math.max(1, (warnings.size() + 2) / 3);

        warningPage = Math.clamp(warningPage, 0, pages - 1);
        navigation(graphics, warningPage, pages);

        int y = compact ? 186 : 206;

        OledDisplay.rule(graphics, 14, y - 8, 292);

        if (warnings.isEmpty()) {
            OledDisplay.text(graphics, "STATUS NORMAL", 14, y, 292, OledDisplay.LABEL);
        } else {
            for (int i = 0; i < 3; i++) {
                int index = warningPage * 3 + i;

                if (index < warnings.size()) {
                    OledDisplay.text(
                            graphics, warnings.get(index), 14, y + i * 14, 292, OledDisplay.ALERT);
                }
            }
        }
    }

    private void renderHeader(GuiGraphics graphics) {
        OledDisplay.text(
                graphics,
                help ? "RECOVERY GUIDE" : "DIVING COMPUTER",
                14,
                12,
                125,
                OledDisplay.WHITE);
        alarm.setLabel(DivingConfig.COMPUTER_ALARMS.get() ? "SND" : "MUTE");
        alarm.setToolTip(
                Component.literal(
                        DivingConfig.COMPUTER_ALARMS.get()
                                ? "Computer alarms on. Click to mute."
                                : "Computer alarms muted. Click to enable."));
        guide.setLabel(help ? "LIVE" : "HELP");
        guide.setChosen(help);
        alarm.setChosen(DivingConfig.COMPUTER_ALARMS.get());
        guide.setToolTip(
                Component.literal(
                        help ? "Return to live instruments" : "Condition causes and recovery"));
        OledDisplay.rule(graphics, 14, 31, 292);
    }

    private void renderDepthAndGas(GuiGraphics graphics, DivingPackets.Snapshot snapshot) {
        OledDisplay.text(graphics, "DEPTH / BLOCKS", 14, 39, 142, OledDisplay.LABEL);
        OledDisplay.text(graphics, "PRESSURE / ATM", 178, 39, 128, OledDisplay.LABEL);
        OledDisplay.value(
                graphics, DiveReadout.format("%.1f", snapshot.depth()), 14, 54, 2.2f, 145);
        OledDisplay.value(
                graphics, DiveReadout.format("%.2f", snapshot.pressure()), 178, 56, 1.8f, 128);
        OledDisplay.rule(graphics, 14, 80, 292);
        OledDisplay.text(graphics, "GAS TIME / HERE", 14, 88, 142, OledDisplay.LABEL);
        OledDisplay.text(
                graphics,
                snapshot.ceiling() > 0 ? "STOP / BLOCKS" : "NO-DECO TIME",
                178,
                88,
                128,
                OledDisplay.LABEL);
        OledDisplay.text(graphics, DiveReadout.gasTime(snapshot), 14, 102, 142, OledDisplay.WHITE);

        String stopReadout;

        if (snapshot.ceiling() > 0) {
            stopReadout = DiveReadout.format("%.0f", snapshot.ceiling());
        } else if (snapshot.ndl() >= 3600) {
            stopReadout = "60m+";
        } else {
            stopReadout = snapshot.ndl() / 60 + "m " + snapshot.ndl() % 60 + "s";
        }

        OledDisplay.text(graphics, stopReadout, 178, 102, 128, OledDisplay.WHITE);
        OledDisplay.text(
                graphics,
                DiveReadout.format("GAS %.0f / %.0f", snapshot.gas(), snapshot.capacity()),
                14,
                119,
                152,
                OledDisplay.LABEL);
        OledDisplay.text(
                graphics,
                snapshot.ceiling() > 0
                        ? "NEXT " + DiveReadout.stopTime(snapshot)
                        : new GasMix(snapshot.oxygen(), snapshot.helium()).name(),
                178,
                119,
                128,
                snapshot.ceiling() > 0 ? OledDisplay.ALERT : OledDisplay.LABEL);
        OledDisplay.rule(graphics, 14, 132, 292);
    }

    private void renderAscent(GuiGraphics graphics, DivingPackets.Snapshot snapshot) {
        OledDisplay.text(
                graphics,
                DiveReadout.format("ASCENT %+.2f b/s", snapshot.ascent()),
                14,
                140,
                152,
                OledDisplay.WHITE);
        OledDisplay.text(
                graphics,
                DiveReadout.format("LIMIT %.2f b/s", snapshot.safeAscent()),
                178,
                140,
                128,
                OledDisplay.LABEL);
        OledDisplay.ascentBar(graphics, 14, 154, 292, snapshot);
        OledDisplay.text(
                graphics,
                DiveReadout.ascentStatus(snapshot),
                14,
                166,
                292,
                (snapshot.warnings() & com.diggydwarff.deepdiving.diving.DiverService.ASCENT) != 0
                                || snapshot.ceiling() > 0
                        ? OledDisplay.ALERT
                        : OledDisplay.LABEL);

        if (!compact) {
            OledDisplay.text(
                    graphics,
                    DiveReadout.format("O2 reserve %.0f%%", snapshot.oxygenation()),
                    14,
                    184,
                    152,
                    OledDisplay.LABEL);
            OledDisplay.text(
                    graphics,
                    snapshot.depthHold()
                            ? "BUOYANCY HOLD ON"
                            : DiveReadout.format("Deco stress %.0f", snapshot.stress()),
                    178,
                    184,
                    128,
                    OledDisplay.LABEL);
        }
    }

    private void navigation(GuiGraphics graphics, int page, int pages) {
        previous.visible = next.visible = pages > 1;
        previous.active = page > 0;
        next.active = page + 1 < pages;

        if (pages > 1) {
            OledDisplay.text(graphics, (page + 1) + "/" + pages, 144, 13, 29, OledDisplay.LABEL);
        }
    }

    private void renderHelp(GuiGraphics graphics) {
        int perPage = 1;

        int pages = (DivingEffects.ALL.size() + perPage - 1) / perPage;

        helpPage = Math.clamp(helpPage, 0, pages - 1);
        navigation(graphics, helpPage, pages);

        int cardHeight = (imageHeight - 56) / perPage;

        for (int i = 0; i < perPage; i++) {
            var effect = DivingEffects.ALL.get(helpPage * perPage + i);

            int y = 42 + i * cardHeight;

            OledDisplay.text(
                    graphics,
                    effect.value().getDisplayName().getString(),
                    14,
                    y,
                    152,
                    OledDisplay.WHITE);

            var snapshot = ClientRegistration.snapshot;

            if (snapshot != null) {
                String value =
                        effect.equals(DivingEffects.HYPOXIA)
                                ? DiveReadout.format("RESERVE %.0f%%", snapshot.oxygenation())
                                : effect.equals(DivingEffects.NARCOSIS)
                                        ? DiveReadout.format("EXPOSURE %.2f", snapshot.narcosis())
                                        : effect.equals(DivingEffects.TOXICITY)
                                                ? DiveReadout.format(
                                                        "EXPOSURE %.0f", snapshot.toxicity())
                                                : effect.equals(DivingEffects.DECOMPRESSION)
                                                        ? DiveReadout.format(
                                                                "STRESS %.0f", snapshot.stress())
                                                        : effect.equals(DivingEffects.PRESSURE)
                                                                ? DiveReadout.format(
                                                                        "INJURY %.0f",
                                                                        snapshot.pressureInjury())
                                                                : (snapshot.warnings()
                                                                                        & com
                                                                                                .diggydwarff
                                                                                                .deepdiving
                                                                                                .diving
                                                                                                .DiverService
                                                                                                .BLACKOUT)
                                                                                != 0
                                                                        ? "UNCONSCIOUS"
                                                                        : "CONSCIOUS";

                OledDisplay.text(graphics, value, 178, y, 128, OledDisplay.LABEL);
            }

            int lineY = y + 17;

            int paragraph = 0;
            for (var text : DivingRecovery.description(effect)) {
                OledDisplay.text(
                        graphics,
                        paragraph++ == 0 ? "CAUSE" : "RECOVERY",
                        14,
                        lineY,
                        280,
                        OledDisplay.LABEL);
                lineY += 14;
                for (var line : font.split(text, 280)) {
                    graphics.drawString(
                            font,
                            line,
                            14,
                            lineY,
                            text.getStyle().getColor() == null
                                    ? OledDisplay.WHITE
                                    : text.getStyle().getColor().getValue(),
                            false);
                    lineY += 12;
                }

                lineY += 5;
            }

            if (i + 1 < perPage) {
                OledDisplay.rule(graphics, 14, y + cardHeight - 8, 292);
            }
        }
        OledDisplay.text(
                graphics,
                "LEFT / RIGHT: BROWSE CONDITIONS",
                14,
                imageHeight - 22,
                292,
                OledDisplay.LABEL);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 263 || key == 262) {
            int change = key == 263 ? -1 : 1;
            if (help) helpPage += change;
            else warningPage += change;
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.renderForeground(graphics, mouseX, mouseY, partial);

        if (help || ClientRegistration.snapshot == null) {
            return;
        }

        int x = mouseX - leftPos;

        int y = mouseY - topPos;

        String tip = null;

        if (x >= 14 && x < 167 && y >= 86 && y < 129) {
            tip =
                    "Estimated time for the active supply at this depth. Excludes spare cylinders,"
                        + " travel and decompression stops. -- means no managed supply estimate.";
        } else if (x >= 178
                && x < 306
                && y >= 86
                && y < (ClientRegistration.snapshot.ceiling() > 0 ? 130 : 113)) {
            tip =
                    ClientRegistration.snapshot.ceiling() > 0
                            ? "The minimum permitted depth. NEXT estimates when the next shallower"
                                    + " stop clears if you hold your current depth and gas. It"
                                    + " excludes later stops and ascent. MOVE TO STOP means this"
                                    + " depth cannot clear the next step within an hour."
                            : "Estimated time at this depth before a decompression stop becomes"
                                    + " necessary. This is separate from gas time.";
        } else if (x >= 178 && x < 306 && y >= 116 && y < 130) {
            tip =
                    new GasMix(
                                    ClientRegistration.snapshot.oxygen(),
                                    ClientRegistration.snapshot.helium())
                            .summary();
        } else if (x >= 14 && x < 306 && y >= 138 && y < 176) {
            tip =
                    "The marker is your server's sustained ascent limit. Brief depth adjustments"
                        + " have a configurable grace period; amber means a sustained rapid ascent."
                        + " A stop ceiling still applies during that grace period and at a slow"
                        + " ascent rate. "
                            + DepthHoldClient.help();
        }

        if (tip != null) {
            graphics.renderTooltip(font, font.split(Component.literal(tip), 240), mouseX, mouseY);
        }
    }
}
