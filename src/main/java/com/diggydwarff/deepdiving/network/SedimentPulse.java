package com.diggydwarff.deepdiving.network;

import com.diggydwarff.deepdiving.DeepDiving;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/** Only a nearby disturbance is broadcast. Every temporary cloud belongs to the viewing client. */
public record SedimentPulse(ResourceLocation dimension, BlockPos position, boolean movement, float intensity)
    implements CustomPacketPayload {
    public static final Type<SedimentPulse> TYPE = new Type<>(DeepDiving.id("sediment_pulse"));

    public static Consumer<SedimentPulse> clientReceiver = pulse -> {};

    public static final StreamCodec<RegistryFriendlyByteBuf, SedimentPulse> CODEC = StreamCodec.of(
        (buffer, pulse) -> {
            buffer.writeResourceLocation(pulse.dimension);
            buffer.writeBlockPos(pulse.position);
            buffer.writeBoolean(pulse.movement);
            buffer.writeFloat(pulse.intensity);
        },
        buffer -> {
            ResourceLocation dimension = buffer.readResourceLocation();

            BlockPos position = buffer.readBlockPos();

            boolean movement = buffer.readBoolean();

            float intensity = buffer.readFloat();

            if (!Float.isFinite(intensity) || intensity < 0 || intensity > 4) {
                throw new IllegalArgumentException("Invalid Deep Diving sediment intensity");
            }

            return new SedimentPulse(dimension, position, movement, intensity);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
