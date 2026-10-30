package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.logbook.DiveLogBookItem;
import com.diggydwarff.deepdiving.logbook.DiveLogPackets;
import com.diggydwarff.deepdiving.logbook.DiveLogService;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** A compact field journal: live telemetry on one page, completed dives on the other. */
public final class DiveLogScreen extends Screen {
    private DiveLogPackets.View view;
    private int page, refreshTicks, x, y, journalWidth, journalHeight;
    private CreateButton toggle, previous, next;
    private List<Component> hoverLines = List.of();

    public DiveLogScreen(DiveLogPackets.View view) {
        super(Component.translatable("item.deepdiving.dive_log_book"));
        this.view = view;
    }

    public static void receive(DiveLogPackets.View view) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DiveLogScreen screen
                && screen.view.id().equals(view.id())
                && screen.view.hand() == view.hand()) {
            long selectedNumber =
                    screen.view
                            .log()
                            .getList("dives", Tag.TAG_COMPOUND)
                            .getCompound(screen.page)
                            .getLong("number");
            boolean diveJustEnded =
                    screen.view.log().getBoolean("recording")
                            && !view.log().getBoolean("recording");
            screen.view = view;
            if (diveJustEnded) {
                screen.page = 0;
            } else if (selectedNumber > 0) {
                var entries = view.log().getList("dives", Tag.TAG_COMPOUND);
                for (int index = 0; index < entries.size(); index++) {
                    if (entries.getCompound(index).getLong("number") == selectedNumber) {
                        screen.page = index;
                        break;
                    }
                }
            }
            screen.updateButtons();
        } else if (view.openScreen()) {
            // A refresh reply arriving after Escape must never reopen a closed journal.
            mc.setScreen(new DiveLogScreen(view));
        }
    }

    @Override
    protected void init() {
        journalWidth = Math.min(356, width - 16);
        journalHeight = Math.min(244, height - 16);
        x = (width - journalWidth) / 2;
        y = (height - journalHeight) / 2;
        int bottom = y + journalHeight - 28;
        toggle =
                addRenderableWidget(
                        new CreateButton(
                                x + 12,
                                bottom,
                                Math.min(130, journalWidth / 2 - 22),
                                "",
                                Component.translatable("gui.deepdiving.log.toggle_help")
                                        .getString(),
                                () -> send(true)));
        previous =
                addRenderableWidget(
                        new CreateButton(
                                x + journalWidth / 2 + 8,
                                bottom,
                                22,
                                "<",
                                Component.translatable("gui.deepdiving.log.previous").getString(),
                                () -> {
                                    page--;
                                    updateButtons();
                                }));
        next =
                addRenderableWidget(
                        new CreateButton(
                                x + journalWidth / 2 + 66,
                                bottom,
                                22,
                                ">",
                                Component.translatable("gui.deepdiving.log.next").getString(),
                                () -> {
                                    page++;
                                    updateButtons();
                                }));
        addRenderableWidget(
                new CreateButton(
                        x + journalWidth - 62,
                        bottom,
                        50,
                        Component.translatable("gui.done").getString(),
                        Component.translatable("gui.done").getString(),
                        this::onClose));
        updateButtons();
    }

    private void send(boolean toggle) {
        PacketDistributor.sendToServer(new DiveLogPackets.Action(view.hand(), view.id(), toggle));
    }

    private void updateButtons() {
        if (toggle == null) return;
        int count = view.log().getList("dives", Tag.TAG_COMPOUND).size();
        page = Math.clamp(page, 0, Math.max(0, count - 1));
        toggle.setLabel(
                Component.translatable(
                                view.log().getBoolean("recording")
                                        ? "gui.deepdiving.log.end"
                                        : "gui.deepdiving.log.start")
                        .getString());
        toggle.setChosen(view.log().getBoolean("recording"));
        previous.active = page > 0;
        next.active = page + 1 < count;
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null) {
            onClose();
            return;
        }
        var held = minecraft.player.getItemInHand(view.hand());
        if (!(held.getItem() instanceof DiveLogBookItem)) {
            onClose();
            return;
        }
        refreshTicks++;
        var heldLog = DiveLogService.read(held);
        if (heldLog.hasUUID("id") && !heldLog.getUUID("id").equals(view.id())
                || !heldLog.hasUUID("id") && refreshTicks > 10) {
            onClose();
            return;
        }
        if (refreshTicks % 20 == 0) send(false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 263 || key == 262) {
            page += key == 263 ? -1 : 1;
            updateButtons();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.renderBackground(g, mouseX, mouseY, partial);
        hoverLines = List.of();
        FieldNotebookStyle.cover(g, x, y, journalWidth, journalHeight);
        g.fill(x + 8, y + 8, x + journalWidth - 8, y + journalHeight - 8, 0xFFADAB91);
        g.fill(x + 9, y + 9, x + journalWidth / 2 - 4, y + journalHeight - 9, 0xFFF1ECD9);
        g.fill(
                x + journalWidth / 2 + 4,
                y + 9,
                x + journalWidth - 9,
                y + journalHeight - 9,
                0xFFF6F0DF);
        g.fill(
                x + journalWidth / 2 - 3,
                y + 9,
                x + journalWidth / 2 + 3,
                y + journalHeight - 9,
                0xFF26464C);
        for (int yy = y + 25; yy < y + journalHeight - 30; yy += 32) {
            g.fill(x + journalWidth / 2 - 6, yy, x + journalWidth / 2 + 6, yy + 3, 0xFF584E3C);
            g.fill(
                    x + journalWidth / 2 - 6,
                    yy,
                    x + journalWidth / 2 + 5,
                    yy + 1,
                    FieldNotebookStyle.BRASS);
        }
        for (int yy = y + 63; yy < y + journalHeight - 38; yy += 12) {
            if (!view.log().getBoolean("recording"))
                g.fill(x + 12, yy, x + journalWidth / 2 - 12, yy + 1, 0x14294D50);
            if (view.log().getList("dives", Tag.TAG_COMPOUND).isEmpty())
                g.fill(x + journalWidth / 2 + 12, yy, x + journalWidth - 12, yy + 1, 0x14294D50);
        }
        // Flush bookmark, with enough margin to keep the text and controls on the paper.
        g.fill(x + journalWidth - 18, y + 9, x + journalWidth - 11, y + 32, 0xFF3A7C79);
        g.drawString(
                font,
                ScreenText.fit(font, title.getString(), journalWidth / 2 - 48),
                x + 12,
                y + 12,
                0x243F47,
                false);
        int left = x + 12, right = x + journalWidth / 2 + 12;
        for (int start : new int[] {left, right})
            g.fill(start, y + 44, start + journalWidth / 2 - 24, y + 45, 0xFFC6B99B);
        int color = 0x425258;
        boolean recording = view.log().getBoolean("recording");
        g.drawString(
                font,
                Component.translatable(
                        recording ? "gui.deepdiving.log.recording" : "gui.deepdiving.log.standby"),
                left,
                y + 30,
                recording ? 0xA13730 : 0x3C7068,
                false);
        if (recording) {
            CompoundTag current = view.log().getCompound("current");
            stats(g, current, left, y + 51, mouseX, mouseY);
        } else {
            var lines =
                    font.split(
                            Component.translatable("gui.deepdiving.log.help"),
                            journalWidth / 2 - 26);
            int lineY = y + 52;
            for (var line : lines) {
                g.drawString(font, line, left, lineY, color, false);
                lineY += 12;
            }
        }
        var history = view.log().getList("dives", Tag.TAG_COMPOUND);
        g.drawString(
                font,
                Component.translatable("gui.deepdiving.log.history"),
                right,
                y + 12,
                0x243F47,
                false);
        if (history.isEmpty()) {
            g.drawWordWrap(
                    font,
                    Component.translatable("gui.deepdiving.log.empty"),
                    right,
                    y + 52,
                    journalWidth / 2 - 26,
                    color);
        } else {
            CompoundTag entry = history.getCompound(page);
            g.drawString(
                    font,
                    Component.translatable(
                            "gui.deepdiving.log.dive",
                            entry.contains("number")
                                    ? entry.getLong("number")
                                    : history.size() - page),
                    right,
                    y + 30,
                    0x3C7068,
                    false);
            stats(g, entry, right, y + 51, mouseX, mouseY);
        }
        String count =
                Component.translatable(
                                "gui.deepdiving.bundle.page",
                                history.isEmpty() ? 0 : page + 1,
                                history.size())
                        .getString();
        g.drawString(
                font,
                count,
                x + journalWidth / 2 + 48 - font.width(count) / 2,
                y + journalHeight - 23,
                0x425258,
                false);
    }

    private void stats(
            GuiGraphics g, CompoundTag entry, int left, int top, int mouseX, int mouseY) {
        int maxWidth = journalWidth / 2 - 25;
        long ticks = Math.max(0, entry.getLong("ticks"));
        long wet = Math.max(0, entry.getLong("wetTicks"));
        double average = wet == 0 ? 0 : entry.getDouble("depthSum") / wet;
        String[] lines = {
            DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault())
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(entry.getLong("started"))),
            text("duration", String.format(Locale.ROOT, "%d:%02d", ticks / 1200, ticks / 20 % 60)),
            text("maximum", String.format(Locale.ROOT, "%.1f", entry.getDouble("maxDepth"))),
            wet > 0
                    ? text("average", String.format(Locale.ROOT, "%.1f", average))
                    : Component.translatable("gui.deepdiving.log.average_unsampled").getString(),
            text("pressure", String.format(Locale.ROOT, "%.1f", entry.getDouble("maxPressure"))),
            text("gas", String.format(Locale.ROOT, "%.0f mB", entry.getDouble("gasUsed"))),
            entry.getDouble("createAirUsed") > 0
                    ? text(
                            "create_air",
                            String.format(Locale.ROOT, "%.0f", entry.getDouble("createAirUsed")))
                    : entry.getString("mixture"),
            text("ceiling", String.format(Locale.ROOT, "%.1f", entry.getDouble("maxCeiling"))),
            text("ascent", String.format(Locale.ROOT, "%.1f", entry.getDouble("maxAscent"))),
            entry.getString("dimension"),
            entry.getInt("x") + ", " + entry.getInt("z"),
            text(
                    "warnings",
                    Integer.toString(
                            DiveReadout.warnings(
                                            entry.getInt("warnings"),
                                            entry.getDouble("maxCeiling") > 0)
                                    .size())),
            entry.contains("ended")
                    ? Component.translatable("gui.deepdiving.log.ended." + entry.getString("ended"))
                            .getString()
                    : Component.translatable("gui.deepdiving.log.live").getString()
        };
        int yy = top;
        int lineHeight = Math.max(10, Math.min(13, (y + journalHeight - 38 - top) / lines.length));
        for (String line : lines) {
            if (yy + 9 > y + journalHeight - 38) break;
            if (lineHeight >= 11) g.fill(left, yy + 10, left + maxWidth, yy + 11, 0x14294D50);
            g.drawString(font, ScreenText.fit(font, line, maxWidth), left, yy, 0x425258, false);
            if (mouseX >= left
                    && mouseX < left + maxWidth
                    && mouseY >= yy
                    && mouseY < yy + lineHeight) {
                if (line.equals(lines[lines.length - 2])) {
                    hoverLines =
                            DiveReadout.warnings(
                                            entry.getInt("warnings"),
                                            entry.getDouble("maxCeiling") > 0)
                                    .stream()
                                    .map(Component::literal)
                                    .map(c -> (Component) c)
                                    .toList();
                } else if (font.width(line) > maxWidth)
                    hoverLines = List.of(Component.literal(line));
            }
            yy += lineHeight;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        g.renderItem(
                new net.minecraft.world.item.ItemStack(
                        com.diggydwarff.deepdiving.DeepDiving.LOG_BOOK.get()),
                x + journalWidth / 2 - 30,
                y + 12);
        if (!hoverLines.isEmpty()) {
            var wrapped = new java.util.ArrayList<net.minecraft.util.FormattedCharSequence>();
            for (Component line : hoverLines)
                wrapped.addAll(font.split(line, Math.min(240, width - 24)));
            g.renderTooltip(font, wrapped, mouseX, mouseY);
        }
    }

    private static String text(String key, String value) {
        return Component.translatable("gui.deepdiving.log." + key, value).getString();
    }
}
