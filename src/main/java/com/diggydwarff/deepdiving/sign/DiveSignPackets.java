package com.diggydwarff.deepdiving.sign;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.network.DivingPackets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.UUID;
import java.util.function.Consumer;

/** A server-issued edit token binds each response to its player and current block entity. */
public final class DiveSignPackets {
    public static Consumer<View> clientReceiver = view -> {};

    public record View(
            BlockPos pos,
            UUID token,
            DiveSignDesign design,
            String caption,
            int color,
            int arrow,
            boolean glowing)
            implements CustomPacketPayload {
        public static final Type<View> TYPE = new Type<>(DeepDiving.id("dive_sign_view"));
        public static final StreamCodec<RegistryFriendlyByteBuf, View> CODEC =
                StreamCodec.of(
                        (b, v) -> {
                            b.writeBlockPos(v.pos);
                            b.writeUUID(v.token);
                            b.writeEnum(v.design);
                            b.writeUtf(v.caption, 64);
                            b.writeVarInt(v.color);
                            b.writeVarInt(v.arrow);
                            b.writeBoolean(v.glowing);
                        },
                        b ->
                                new View(
                                        b.readBlockPos(),
                                        b.readUUID(),
                                        b.readEnum(DiveSignDesign.class),
                                        b.readUtf(64),
                                        b.readVarInt(),
                                        b.readVarInt(),
                                        b.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Action(BlockPos pos, UUID token, String caption, int color, int arrow)
            implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(DeepDiving.id("dive_sign_edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC =
                StreamCodec.of(
                        (b, v) -> {
                            b.writeBlockPos(v.pos);
                            b.writeUUID(v.token);
                            b.writeUtf(v.caption, 64);
                            b.writeVarInt(v.color);
                            b.writeVarInt(v.arrow);
                        },
                        b ->
                                new Action(
                                        b.readBlockPos(),
                                        b.readUUID(),
                                        b.readUtf(64),
                                        b.readVarInt(),
                                        b.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static boolean apply(ServerPlayer player, Action action) {
        if (!player.mayBuild()
                || action.color < 0
                || action.color > 15
                || action.arrow < 0
                || action.arrow > 4) return false;
        var level = player.serverLevel();
        if (!level.hasChunkAt(action.pos)
                || player.distanceToSqr(
                                action.pos.getX() + .5,
                                action.pos.getY() + .5,
                                action.pos.getZ() + .5)
                        > 64
                || !level.mayInteract(player, action.pos)) return false;
        if (!(level.getBlockEntity(action.pos) instanceof DivePanelBlockEntity panel)
                || !panel.accepts(player, action.token)) return false;
        if (EventHooks.onBlockPlace(
                player,
                BlockSnapshot.create(level.dimension(), level, action.pos),
                panel.getBlockState().getValue(DivePanelBlock.FACING))) return false;
        panel.setSettings(action.caption, action.color, action.arrow);
        panel.finishEditing();
        return true;
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
                                            if (context.player() instanceof ServerPlayer player
                                                    && !apply(player, action))
                                                player.displayClientMessage(
                                                        Component.translatable(
                                                                "message.deepdiving.dive_sign.retry"),
                                                        true);
                                        }));
    }

    private DiveSignPackets() {}
}
