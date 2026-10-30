package com.diggydwarff.deepdiving.network;

import com.diggydwarff.deepdiving.DeepDiving;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;
import java.util.function.Consumer;

/** Public equipment state only. The server never creates rendering objects. */
public record LampState(UUID diver, ResourceLocation dimension, boolean active, boolean held, int profile)
        implements CustomPacketPayload {
    public static final Type<LampState> TYPE = new Type<>(DeepDiving.id("lamp_state"));

    public static Consumer<LampState> clientReceiver = state -> {};

    public static final StreamCodec<RegistryFriendlyByteBuf, LampState> CODEC = StreamCodec.of(
        (buffer, state) -> {
            buffer.writeUUID(state.diver);
            buffer.writeResourceLocation(state.dimension);
            buffer.writeBoolean(state.active);
            buffer.writeBoolean(state.held);
            buffer.writeVarInt(state.profile);
        },
        buffer -> new LampState(buffer.readUUID(), buffer.readResourceLocation(),
            buffer.readBoolean(), buffer.readBoolean(), buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
