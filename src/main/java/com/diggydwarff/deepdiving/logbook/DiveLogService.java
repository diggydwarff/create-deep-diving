package com.diggydwarff.deepdiving.logbook;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.diving.DiverService;
import com.diggydwarff.deepdiving.equipment.BreathingEquipment;
import com.diggydwarff.deepdiving.gas.GasMix;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server-owned, item-persistent dive records. Sampling reads the existing computer simulation. */
public final class DiveLogService {
    public static final int MAX_ENTRIES = 32;
    private static final long MAX_TICKS = DiveLogStatistics.MAX_TICKS;
    private static final String KEY = "deepdiving_log";

    public static CompoundTag read(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getCompound(KEY);
    }

    private static void write(ItemStack stack, CompoundTag log) {
        CompoundTag root =
                stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(KEY, log);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static List<ItemStack> books(ServerPlayer player) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items)
            if (stack.getItem() instanceof DiveLogBookItem) result.add(stack);
        if (player.getOffhandItem().getItem() instanceof DiveLogBookItem)
            result.add(player.getOffhandItem());
        return result;
    }

    public static UUID id(ItemStack stack) {
        CompoundTag log = read(stack);
        if (!log.hasUUID("id")) {
            log.putUUID("id", UUID.randomUUID());
            write(stack, log);
        }
        return log.getUUID("id");
    }

    public static void open(ServerPlayer player, InteractionHand hand) {
        sendView(player, hand, true);
    }

    public static void sendView(ServerPlayer player, InteractionHand hand, boolean openScreen) {
        ItemStack book = player.getItemInHand(hand);
        if (!(book.getItem() instanceof DiveLogBookItem)) return;
        UUID id = id(book);
        validate(player, book);
        PacketDistributor.sendToPlayer(
                player, new DiveLogPackets.View(hand, id, read(book), openScreen));
    }

    public static void toggle(ServerPlayer player, ItemStack book) {
        if (!(book.getItem() instanceof DiveLogBookItem)) return;
        validate(player, book);
        CompoundTag log = read(book);
        if (log.getBoolean("recording")) {
            finish(player, book, "manual");
            return;
        }
        if (!player.isAlive() || player.isSpectator()) return;
        if (!BreathingEquipment.carries(player, DeepDiving.COMPUTER.get())) {
            player.displayClientMessage(
                    Component.translatable("message.deepdiving.log.needs_computer"), true);
            return;
        }
        for (ItemStack other : books(player))
            if (other != book && read(other).getBoolean("recording"))
                finish(player, other, "replaced");
        id(book);
        log = read(book);
        CompoundTag session = new CompoundTag();
        session.putUUID("owner", player.getUUID());
        session.putString("diver", player.getGameProfile().getName());
        session.putString("dimension", player.level().dimension().location().toString());
        session.putLong("started", System.currentTimeMillis());
        session.putLong("day", player.level().getDayTime() / 24000);
        session.putInt("x", player.blockPosition().getX());
        session.putInt("z", player.blockPosition().getZ());
        session.putLong("last", player.level().getGameTime());
        session.putDouble("maxPressure", 1);
        long previousNumber =
                Math.max(log.getLong("totalDives"), log.getList("dives", Tag.TAG_COMPOUND).size());
        long number = previousNumber == Long.MAX_VALUE ? previousNumber : previousNumber + 1;
        session.putLong("number", number);
        log.putLong("totalDives", number);
        DiveLogStatistics.sample(session, DiverService.state(player), player.level().getGameTime());
        log.put("current", session);
        log.putBoolean("recording", true);
        write(book, log);
        player.displayClientMessage(Component.translatable("message.deepdiving.log.started"), true);
    }

    /** Returns false after sealing a stale or transferred session. */
    private static boolean validate(ServerPlayer player, ItemStack book) {
        CompoundTag log = read(book);
        if (!log.getBoolean("recording")) return false;
        CompoundTag session = log.getCompound("current");
        String reason = null;
        if (!session.hasUUID("owner") || !session.getUUID("owner").equals(player.getUUID()))
            reason = "transferred";
        else if (!session.getString("dimension")
                .equals(player.level().dimension().location().toString())) reason = "dimension";
        else {
            long gap = player.level().getGameTime() - session.getLong("last");
            if (gap < 0 || gap > 60) reason = "interrupted";
        }
        if (reason != null) {
            finish(player, book, reason);
            return false;
        }
        return true;
    }

    private static void sample(ServerPlayer player, ItemStack book) {
        if (!validate(player, book)) return;
        if (!BreathingEquipment.carries(player, DeepDiving.COMPUTER.get())) {
            finish(player, book, "computer_missing");
            return;
        }
        CompoundTag log = read(book), session = log.getCompound("current");
        DiveLogStatistics.sample(session, DiverService.state(player), player.level().getGameTime());
        log.put("current", session);
        write(book, log);
        if (session.getLong("ticks") >= MAX_TICKS) finish(player, book, "limit");
    }

    /**
     * Called only after a real tank debit, so changing supplies cannot look like negative usage.
     */
    public static void gasUsed(ServerPlayer player, double quantity, boolean create, GasMix mix) {
        if (!Double.isFinite(quantity)
                || quantity <= 0
                || !BreathingEquipment.carries(player, DeepDiving.COMPUTER.get())) return;
        for (ItemStack book : books(player)) {
            if (!validate(player, book)) continue;
            CompoundTag log = read(book), session = log.getCompound("current");
            String key = create ? "createAirUsed" : "gasUsed";
            session.putDouble(key, Math.min(1e12, session.getDouble(key) + quantity));
            session.putString("mixture", mix.summary());
            log.put("current", session);
            write(book, log);
        }
    }

    public static void finish(ServerPlayer player, ItemStack book, String reason) {
        CompoundTag log = read(book);
        if (!log.getBoolean("recording")) return;
        CompoundTag session = log.getCompound("current");
        if (session.hasUUID("owner")
                && session.getUUID("owner").equals(player.getUUID())
                && session.getString("dimension")
                        .equals(player.level().dimension().location().toString())
                && !reason.equals("interrupted")
                && !reason.equals("transferred")) {
            DiveLogStatistics.sample(
                    session, DiverService.state(player), player.level().getGameTime());
        }
        if (!session.contains("number")) {
            long previous =
                    Math.max(
                            log.getLong("totalDives"),
                            log.getList("dives", Tag.TAG_COMPOUND).size());
            long number = previous == Long.MAX_VALUE ? previous : previous + 1;
            session.putLong("number", number);
            log.putLong("totalDives", number);
        }
        session.putString("ended", reason);
        session.remove("last");
        ListTag dives = log.getList("dives", Tag.TAG_COMPOUND);
        dives.add(0, session.copy());
        while (dives.size() > MAX_ENTRIES) dives.remove(dives.size() - 1);
        log.put("dives", dives);
        log.remove("current");
        log.putBoolean("recording", false);
        write(book, log);
    }

    private static void finishAll(ServerPlayer player, String reason) {
        for (ItemStack book : books(player)) finish(player, book, reason);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0)
            for (ItemStack book : books(player)) sample(player, book);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) finishAll(player, "logout");
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) finishAll(player, "dimension");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player)
            finishAll(player, "death");
    }

    /** A tossed stack is a copy of the inventory stack, so seal the actual dropped item. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tossed(ItemTossEvent event) {
        if (!event.isCanceled()
                && event.getPlayer() instanceof ServerPlayer player
                && event.getEntity().getItem().getItem() instanceof DiveLogBookItem) {
            finish(player, event.getEntity().getItem(), "book_removed");
        }
    }

    private DiveLogService() {}
}
