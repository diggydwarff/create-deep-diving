package com.diggydwarff.deepdiving.logbook;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.network.DivingPackets;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.UUID;
import java.util.function.Consumer;

/** Opening a book and refreshing an existing screen are distinct server responses. */
public final class DiveLogPackets {
    private static final int REQUEST_COOLDOWN = 10;
    public static Consumer<View> clientReceiver = view -> {};

    public record View(InteractionHand hand, UUID id, CompoundTag log, boolean openScreen)
            implements CustomPacketPayload {
        public static final Type<View> TYPE = new Type<>(DeepDiving.id("dive_log_view"));
        public static final StreamCodec<RegistryFriendlyByteBuf, View> CODEC =
                StreamCodec.of(
                        (buffer, view) -> {
                            buffer.writeEnum(view.hand);
                            buffer.writeUUID(view.id);
                            buffer.writeNbt(view.log);
                            buffer.writeBoolean(view.openScreen);
                        },
                        buffer ->
                                new View(
                                        buffer.readEnum(InteractionHand.class),
                                        buffer.readUUID(),
                                        buffer.readNbt(),
                                        buffer.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Only held-book identity and input intent travel to the server, never recorded statistics. */
    public record Action(InteractionHand hand, UUID id, boolean toggle)
            implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(DeepDiving.id("dive_log_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC =
                StreamCodec.of(
                        (buffer, action) -> {
                            buffer.writeEnum(action.hand);
                            buffer.writeUUID(action.id);
                            buffer.writeBoolean(action.toggle);
                        },
                        buffer ->
                                new Action(
                                        buffer.readEnum(InteractionHand.class),
                                        buffer.readUUID(),
                                        buffer.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(DivingPackets.PROTOCOL_VERSION)
                .playToClient(
                        View.TYPE,
                        View.CODEC,
                        (view, context) -> context.enqueueWork(() -> clientReceiver.accept(view)))
                .playToServer(
                        Action.TYPE,
                        Action.CODEC,
                        (action, context) ->
                                context.enqueueWork(
                                        () -> {
                                            if (context.player() instanceof ServerPlayer player) {
                                                handleAction(player, action);
                                            }
                                        }));
    }

    private static void handleAction(ServerPlayer player, Action action) {
        var book = player.getItemInHand(action.hand);
        if (!(book.getItem() instanceof DiveLogBookItem)
                || !DiveLogService.id(book).equals(action.id)) {
            return;
        }

        String cooldownKey = action.toggle ? "deepdiving.logToggle" : "deepdiving.logRefresh";
        long previous = player.getPersistentData().getLong(cooldownKey);
        long elapsed = player.tickCount - previous;
        // Player tick counters restart on login; an old persisted counter must not block requests.
        if (previous > 0 && elapsed >= 0 && elapsed < REQUEST_COOLDOWN) {
            return;
        }
        player.getPersistentData().putLong(cooldownKey, player.tickCount);
        if (action.toggle) {
            DiveLogService.toggle(player, book);
        }
        DiveLogService.sendView(player, action.hand, false);
    }

    private DiveLogPackets() {}
}
