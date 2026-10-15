package com.diggydwarff.deepdiving.network;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.physiology.DiverState;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.Consumer;

/** Server telemetry and toggle requests; physiology, gas and held depth remain server-owned. */
public final class DivingPackets {
    public static Consumer<Snapshot> clientReceiver = snapshot -> {};

    public static Consumer<TreadingPose> poseReceiver = pose -> {};

    public record Snapshot(
        double depth,
        double pressure,
        double gas,
        double capacity,
        double ascent,
        double oxygenation,
        double toxicity,
        double narcosis,
        double stress,
        double ceiling,
        double pressureInjury,
        double pressureShock,
        int ndl,
        int oxygen,
        int helium,
        int warnings,
        boolean underwater,
        double gasSeconds,
        double safeAscent,
        int stopSeconds,
        boolean depthHold,
        double heldDepth,
        double holdTargetY,
        boolean finsFloating,
        boolean finSwimming,
        double finDepth,
        double finTargetY
    ) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(DeepDiving.id("diver_state"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = StreamCodec.of(
            (b, s) -> {
                b.writeDouble(s.depth);
                b.writeDouble(s.pressure);
                b.writeDouble(s.gas);
                b.writeDouble(s.capacity);
                b.writeDouble(s.ascent);
                b.writeDouble(s.oxygenation);
                b.writeDouble(s.toxicity);
                b.writeDouble(s.narcosis);
                b.writeDouble(s.stress);
                b.writeDouble(s.ceiling);
                b.writeDouble(s.pressureInjury);
                b.writeDouble(s.pressureShock);
                b.writeVarInt(s.ndl);
                b.writeVarInt(s.oxygen);
                b.writeVarInt(s.helium);
                b.writeVarInt(s.warnings);
                b.writeBoolean(s.underwater);
                b.writeDouble(s.gasSeconds);
                b.writeDouble(s.safeAscent);
                b.writeVarInt(s.stopSeconds);
                b.writeBoolean(s.depthHold);
                b.writeDouble(s.heldDepth);
                b.writeDouble(s.holdTargetY);
                b.writeBoolean(s.finsFloating);
                b.writeBoolean(s.finSwimming);
                b.writeDouble(s.finDepth);
                b.writeDouble(s.finTargetY);
            },
            b -> new Snapshot(
                b.readDouble(),
                b.readDouble(),
                b.readDouble(),
                b.readDouble(),
                b.readDouble(),
                b.readDouble(),
                b.readDouble(),
                b.readDouble(),
                b.readDouble(),
                b.readDouble(),
                b.readDouble(),
                b.readDouble(),
                b.readVarInt(),
                b.readVarInt(),
                b.readVarInt(),
                b.readVarInt(),
                b.readBoolean(),
                b.readDouble(),
                b.readDouble(),
                b.readVarInt(),
                b.readBoolean(),
                b.readDouble(),
                b.readDouble(),
                b.readBoolean(),
                b.readBoolean(),
                b.readDouble(),
                b.readDouble()
            )
        );

        public Snapshot(
            double depth,
            double pressure,
            double gas,
            double capacity,
            double ascent,
            double oxygenation,
            double toxicity,
            double narcosis,
            double stress,
            double ceiling,
            double pressureInjury,
            double pressureShock,
            int ndl,
            int oxygen,
            int helium,
            int warnings,
            boolean underwater,
            double gasSeconds,
            double safeAscent,
            int stopSeconds,
            boolean depthHold,
            double heldDepth,
            double holdTargetY
        ) {
            this(
                depth,
                pressure,
                gas,
                capacity,
                ascent,
                oxygenation,
                toxicity,
                narcosis,
                stress,
                ceiling,
                pressureInjury,
                pressureShock,
                ndl,
                oxygen,
                helium,
                warnings,
                underwater,
                gasSeconds,
                safeAscent,
                stopSeconds,
                depthHold,
                heldDepth,
                holdTargetY,
                false,
                false,
                0,
                0
            );
        }

        /** Older visual fixtures have no stop estimate or active buoyancy assistance. */
        public Snapshot(
            double depth,
            double pressure,
            double gas,
            double capacity,
            double ascent,
            double oxygenation,
            double toxicity,
            double narcosis,
            double stress,
            double ceiling,
            double pressureInjury,
            double pressureShock,
            int ndl,
            int oxygen,
            int helium,
            int warnings,
            boolean underwater,
            double gasSeconds,
            double safeAscent
        ) {
            this(
                depth,
                pressure,
                gas,
                capacity,
                ascent,
                oxygenation,
                toxicity,
                narcosis,
                stress,
                ceiling,
                pressureInjury,
                pressureShock,
                ndl,
                oxygen,
                helium,
                warnings,
                underwater,
                gasSeconds,
                safeAscent,
                ceiling > 0 ? -1 : 0,
                false,
                0,
                0
            );
        }

        public static Snapshot of(DiverState s) {
            return of(s, true);
        }

        public static Snapshot of(DiverState s, boolean active) {
            return new Snapshot(
                s.depth,
                s.pressure,
                s.gas,
                s.gasCapacity,
                s.ascentRate,
                active ? s.oxygenation : 100,
                active ? s.toxicity : 0,
                active ? s.narcosis : 0,
                active ? s.decompressionStress : 0,
                s.ceiling,
                active ? s.pressureInjury : 0,
                active ? s.pressureShock : 0,
                s.noDecoSeconds,
                s.mixture.oxygen(),
                s.mixture.helium(),
                s.warnings,
                s.underwater,
                active ? s.gasSeconds : -1,
                com.diggydwarff.deepdiving.config.DivingConfig.SAFE_ASCENT.get(),
                active ? s.stopSeconds : 0,
                active && s.depthHold,
                active && s.depthHold ? s.heldDepth : 0,
                active && s.depthHold ? s.holdTargetY : 0,
                active && s.finsFloating,
                active && s.finSwimming,
                active && s.finsFloating ? s.finDepth : 0,
                active && s.finsFloating ? s.finTargetY : 0
            );
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** A toggle request contains no depth or coordinates for the client to forge. */
    public record DepthHoldRequest(boolean enabled) implements CustomPacketPayload {
        public static final Type<DepthHoldRequest> TYPE = new Type<>(DeepDiving.id("depth_hold"));

        public static final StreamCodec<RegistryFriendlyByteBuf, DepthHoldRequest> CODEC =
            StreamCodec.of((b, request) -> b.writeBoolean(request.enabled), b -> new DepthHoldRequest(b.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent e) {
        e.registrar("6")
            .playToClient(Snapshot.TYPE, Snapshot.CODEC, (s, ctx) -> ctx.enqueueWork(() -> clientReceiver.accept(s)))
            .playToClient(
                TreadingPose.TYPE,
                TreadingPose.CODEC,
                (pose, ctx) -> ctx.enqueueWork(() -> poseReceiver.accept(pose))
            )
            .playToServer(
                DepthHoldRequest.TYPE,
                DepthHoldRequest.CODEC,
                (request, ctx) -> ctx.enqueueWork(
                    () -> {
                        if (ctx.player()
                                instanceof
                                net.minecraft.server.level.ServerPlayer
                                                player) {
                            com.diggydwarff.deepdiving.diving.DepthHoldService.request(player, request.enabled());
                        }
                    }
                )
            )
            .playToServer(
                FinControl.TYPE,
                FinControl.CODEC,
                (request, ctx) -> ctx.enqueueWork(
                    () -> {
                        if (ctx.player()
                                instanceof
                                net.minecraft.server.level.ServerPlayer
                                                player) {
                            com.diggydwarff.deepdiving.diving.FinsService.control(
                                player,
                                request.swimming(),
                                request.vertical(),
                                request.moving()
                            );
                        }
                    }
                )
            );
    }

    /** Input intent only; buoyancy coordinates and gear checks belong to the server. */
    public record FinControl(boolean swimming, boolean vertical, boolean moving) implements CustomPacketPayload {
        public static final Type<FinControl> TYPE = new Type<>(DeepDiving.id("fin_control"));

        public static final StreamCodec<RegistryFriendlyByteBuf, FinControl> CODEC = StreamCodec.of(
            (b, input) -> {
                b.writeBoolean(input.swimming);
                b.writeBoolean(input.vertical);
                b.writeBoolean(input.moving);
            },
            b -> new FinControl(b.readBoolean(), b.readBoolean(), b.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TreadingPose(
        int entityId,
        boolean active,
        boolean finPoseControlled,
        boolean finSwimming
    ) implements CustomPacketPayload {
        public static final Type<TreadingPose> TYPE = new Type<>(DeepDiving.id("treading_pose"));

        public static final StreamCodec<RegistryFriendlyByteBuf, TreadingPose> CODEC = StreamCodec.of(
            (b, pose) -> {
                b.writeVarInt(pose.entityId);
                b.writeBoolean(pose.active);
                b.writeBoolean(pose.finPoseControlled);
                b.writeBoolean(pose.finSwimming);
            },
            b -> new TreadingPose(
                b.readVarInt(),
                b.readBoolean(),
                b.readBoolean(),
                b.readBoolean()
            )
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private DivingPackets() {}
}
