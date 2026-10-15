package com.diggydwarff.deepdiving.network;

import com.diggydwarff.deepdiving.DeepDiving;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;
import java.util.function.Consumer;

/** Public equipment presentation, without exposing a diver's gas inventory or medical state. */
public record BreathingPulse(
    int entityId,
    UUID diverId,
    int profile,
    int phase,
    int ticks,
    boolean bubbles
) implements CustomPacketPayload {
    public static final int REGULATOR = 1;

    public static final int CREATE_HELMET = 2;

    public static final Type<BreathingPulse> TYPE = new Type<>(DeepDiving.id("breathing_pulse"));

    public static Consumer<BreathingPulse> clientReceiver = pulse -> {};

    public static final StreamCodec<RegistryFriendlyByteBuf, BreathingPulse> CODEC = StreamCodec.of(
        (buffer, pulse) -> {
            buffer.writeVarInt(pulse.entityId);
            buffer.writeUUID(pulse.diverId);
            buffer.writeByte(pulse.profile);
            buffer.writeByte(pulse.phase);
            buffer.writeVarInt(pulse.ticks);
            buffer.writeBoolean(pulse.bubbles);
        },
        buffer -> new BreathingPulse(
            buffer.readVarInt(),
            buffer.readUUID(),
            buffer.readUnsignedByte(),
            buffer.readUnsignedByte(),
            buffer.readVarInt(),
            buffer.readBoolean()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
