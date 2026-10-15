package com.diggydwarff.deepdiving.diving;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.compat.DeepSeasCompatibility;
import com.diggydwarff.deepdiving.compat.PressureCompatibility;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.config.PressureProfile;
import com.diggydwarff.deepdiving.equipment.BreathingEquipment;
import com.diggydwarff.deepdiving.gas.GasMix;
import com.diggydwarff.deepdiving.network.DivingPackets;
import com.diggydwarff.deepdiving.physiology.DecompressionStops;
import com.diggydwarff.deepdiving.physiology.DiverState;
import com.diggydwarff.deepdiving.physiology.DivingEffects;
import com.diggydwarff.deepdiving.physiology.PressureInjury;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DiverService {
    public static final int LOW_GAS = 1;

    public static final int HYPOXIA = 2;

    public static final int TOXICITY = 4;

    public static final int NARCOSIS = 8;

    public static final int ASCENT = 16;

    public static final int DECO = 32;

    public static final int RATING = 64;

    public static final int BLACKOUT = 128;

    public static final int BACKUP = 256;

    public static final int NO_SUPPLY = 512;

    public static final int PRESSURE_INJURY = 1024;

    public static final int PRESSURE_SHOCK = 2048;

    private static final String STATE_KEY = "deepdiving.physiology";

    private static final ResourceKey<DamageType> PRESSURE_DAMAGE_TYPE =
        ResourceKey.create(Registries.DAMAGE_TYPE, DeepDiving.id("pressure_injury"));

    private static final Map<UUID, DiverState> STATES = new HashMap<>();

    public static DiverState state(Player player) {
        return STATES.computeIfAbsent(
            player.getUUID(),
            id -> DiverState.load(player.getPersistentData().getCompound(STATE_KEY))
        );
    }

    /** A worn diving helmet opts Creative into simulation without changing Creative abilities. */
    public static boolean active(Player player) {
        return DivingConfig.ENABLED.get()
            && !player.isSpectator()
            && (!player.isCreative() || BreathingEquipment.helmet(player));
    }

    public static boolean manages(Player player) {
        if (!active(player)
            || !player.isEyeInFluid(FluidTags.WATER)
            || DeepSeasCompatibility.inDryCompartment(player)
            || !BreathingEquipment.helmet(player)) {
            return false;
        }

        if (DivingConfig.CREATE_AIR.get()) {
            return true;
        }

        if (!BreathingEquipment.carries(player, DeepDiving.REGULATOR.get())) {
            return false;
        }

        var selected = BreathingEquipment.select(player);

        return selected == null || !selected.create();
    }

    /** Called from a narrow Create helmet mixin, preserving vanilla potions and lava behavior. */
    public static void breathe(LivingBreatheEvent event) {
        if (!(event.getEntity() instanceof Player player) || !manages(player)) {
            return;
        }

        // NeoForge treats Creative invulnerability as breathable. Equipped Creative divers still
        // use their supply and breath reserve; actual magical breathing keeps its bypass.
        boolean hasBreathingEffect = player.hasEffect(MobEffects.WATER_BREATHING)
            || player.hasEffect(MobEffects.CONDUIT_POWER);

        if (event.canBreathe()
            && DivingConfig.WATER_BREATHING_BYPASS.get()
            && (!player.isCreative() || hasBreathingEffect)) {
            return;
        }

        var supply = BreathingEquipment.select(player);

        if (player.level().isClientSide) {
            player.getPersistentData().remove("VisualBacktankAir");

            if (supply != null && supply.create()) {
                int visualAir = com.simibubi.create.content.equipment.armor.BacktankUtil.getAllWithAir(player)
                    .stream()
                    .mapToInt(
                        com.simibubi.create.content.equipment.armor.BacktankUtil
                                ::getAir
                    )
                    .sum();

                player.getPersistentData().putInt("VisualBacktankAir", visualAir);
            }
        }

        boolean canBreathe = supply != null && supply.available();

        event.setCanBreathe(canBreathe);

        if (canBreathe) {
            event.setRefillAirAmount(player.getMaxAirSupply());

            if (player instanceof ServerPlayer serverPlayer && player.tickCount % 20 == 0) {
                com.simibubi.create.foundation.advancement.AllAdvancements.DIVING_SUIT.awardTo(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }

        DivingEffects.updateAttributes(player);

        // Attribute symptoms stay current each tick; breathing and tissue integration run once a
        // second.
        if (player.tickCount % 20 != 0) {
            return;
        }

        DiverState state = state(player);

        if (!active(player)) {
            DivingEffects.clear(player);
            state.warnings = 0;
            state.underwater = false;
            state.initialized = false;
            state.gasSeconds = -1;
            state.ceiling = 0;
            state.stopSeconds = 0;
            state.noDecoSeconds = 3600;
            DepthHoldService.clear(state);
            send(player, state);
            return;
        }

        // Establish ambient pressure and a valid motion history before evaluating this breath.
        var profile = DivingConfig.pressureProfile(player.level().dimension().location());

        boolean historyValid = updateEnvironment(player, state, profile);

        var supply = BreathingEquipment.select(player);

        boolean hasBreathingEffect = player.hasEffect(MobEffects.WATER_BREATHING)
            || player.hasEffect(MobEffects.CONDUIT_POWER);

        boolean magicalBypass = hasBreathingEffect && DivingConfig.WATER_BREATHING_BYPASS.get();

        state.supplied = state.underwater && supply != null && supply.available() && !magicalBypass;
        state.emergency = state.supplied && supply.emergency();
        state.mixture = supply != null ? supply.mix() : GasMix.AIR;
        state.gas = supply != null ? supply.remaining() : 0;
        state.gasCapacity = supply != null ? supply.capacity() : 0;
        state.gasSeconds = -1;

        double breathingPressure = state.supplied ? state.pressure : 1;

        GasMix breathingMix = state.supplied ? state.mixture : GasMix.AIR;

        double oxygenPressure = breathingMix.oxygenPartialPressure(breathingPressure);

        double severity = DivingConfig.severity();

        state.warnings = 0;

        consumeBreathingGas(player, state, supply, magicalBypass);

        boolean physiologyEnabled = DivingConfig.PHYSIOLOGY.get();

        if (physiologyEnabled) {
            updatePhysiology(
                player,
                state,
                breathingMix,
                breathingPressure,
                oxygenPressure,
                severity,
                magicalBypass
            );
        } else {
            clearPhysiology(player, state);
        }

        updateDecompressionReadouts(player, state, profile, physiologyEnabled, magicalBypass);

        updateWarnings(player, state, oxygenPressure, magicalBypass);

        applyPressureEffects(player, state, historyValid, severity);
        DivingEffects.pressure(player, state.pressureInjury);
        DivingEffects.updateAttributes(player);
        state.previousPressure = state.pressure;
        DivingAdvancements.check(player, state);
        player.getPersistentData().put(STATE_KEY, state.save());
        DepthHoldService.update(player, state);
        send(player, state);
    }

    /** Sample pressure and motion history; dimension changes must not count as rapid ascents. */
    private static boolean updateEnvironment(
        ServerPlayer player,
        DiverState state,
        PressureProfile profile
    ) {
        boolean historyValid = state.initialized
            && state.previousDimension == player.level().dimension()
            && profile.equals(state.previousProfile);

        state.underwater = player.isEyeInFluid(FluidTags.WATER)
            && !DeepSeasCompatibility.inDryCompartment(player);
        state.depth = state.underwater ? DepthSampler.depth(player) : 0;
        state.pressure = profile.pressure(state.depth);
        // Coordinates may change on dimension transfer: never interpret a teleport as a rapid
        // ascent.
        state.ascentRate = historyValid && Math.abs(player.getY() - state.previousY) < 16
                ? player.getY() - state.previousY
                : 0;
        state.previousY = player.getY();
        state.initialized = true;
        state.previousDimension = player.level().dimension();
        state.previousProfile = profile;

        return historyValid;
    }

    /** Charge this breath once, then refresh the readout from the next available supply. */
    private static void consumeBreathingGas(
        ServerPlayer player,
        DiverState state,
        BreathingEquipment.Supply supply,
        boolean magicalBypass
    ) {
        if (state.supplied && (!supply.create() || DivingConfig.CREATE_AIR.get())) {
            BreathingEquipment.consume(player, supply, BreathingEquipment.consumption(supply, state.pressure));

            var nextSupply = BreathingEquipment.select(player);

            state.gas = nextSupply == null ? 0 : nextSupply.remaining();
            // The readout describes the next selected supply, even when this breath empties a tank.
            state.gasCapacity = nextSupply == null ? supply.capacity() : nextSupply.capacity();

            if (nextSupply != null) {
                state.mixture = nextSupply.mix();
                state.emergency = nextSupply.emergency();
            }

            state.gasSeconds = nextSupply == null
                    ? 0
                    : BreathingEquipment.remainingSeconds(nextSupply, state.pressure);
        } else if (!magicalBypass) {
            state.gasSeconds = BreathingEquipment.remainingSeconds(supply, state.pressure);
        }
    }

    /** Integrate the gas actually inhaled, even if this breath just emptied that cylinder. */
    private static void updatePhysiology(
        ServerPlayer player,
        DiverState state,
        GasMix breathingMix,
        double breathingPressure,
        double oxygenPressure,
        double severity,
        boolean magicalBypass
    ) {
        // Breath holding retains the previous lung/tissue gas; it does not load compressed air.
        if (!state.underwater || state.supplied || magicalBypass) {
            state.tissues.step(
                breathingMix,
                breathingPressure,
                DivingConfig.SIMULATION_RATE.get(),
                DivingConfig.DECOMPRESSION_RATE.get()
            );
        }

        boolean oxygenDeficit = state.underwater
            && !magicalBypass
            && ((!state.supplied && player.getAirSupply() < 60)
                || (state.supplied
                    && oxygenPressure < DivingConfig.MIN_OXYGEN.get()));

        double minimumOxygen = DivingConfig.MIN_OXYGEN.get();

        double deficit = state.supplied
                ? Math.clamp((minimumOxygen - oxygenPressure) / minimumOxygen, 0.15, 1)
                : 1;

        double oxygenChange;

        if (oxygenDeficit) {
            oxygenChange = -DivingConfig.HYPOXIA_RATE.get() * deficit * severity;
        } else {
            oxygenChange = DivingConfig.RECOVERY_RATE.get();
        }

        state.oxygenation = Math.clamp(state.oxygenation + oxygenChange, 0, 100);

        // Excess oxygen accumulates toxicity; safe breathing lets it recover gradually.
        double toxicityChange = -0.2;

        if (state.supplied && oxygenPressure > DivingConfig.MAX_OXYGEN.get()) {
            toxicityChange = (oxygenPressure - DivingConfig.MAX_OXYGEN.get()) * 1.5 * severity;
        }

        state.toxicity = Math.clamp(state.toxicity + toxicityChange, 0, 300);

        if (state.supplied) {
            state.narcosis = Math.max(
                0,
                breathingMix.narcoticPressure(state.pressure)
                        - DivingConfig.NARCOSIS_START.get()
            )
                    * severity;
        } else {
            state.narcosis = 0;
        }

        double excess = state.tissues.supersaturation(state.pressure);

        state.decompressionStress =
            Math.clamp(state.decompressionStress + (excess > 0.03 ? excess * 3 * severity : -0.12), 0, 300);

        if (state.oxygenation < 20 || state.toxicity > 100) {
            state.blackedOut = true;
        } else if (state.oxygenation >= 35 && state.toxicity < 80) {
            state.blackedOut = false;
        }

        applyPhysiologyEffects(player, state);
    }

    private static void clearPhysiology(ServerPlayer player, DiverState state) {
        state.oxygenation = 100;
        state.toxicity = 0;
        state.narcosis = 0;
        state.decompressionStress = 0;
        state.blackedOut = false;
        DivingEffects.clearPhysiology(player);
    }

    /** Forecast from the next breath without changing the diver's actual tissue loading. */
    private static void updateDecompressionReadouts(
        ServerPlayer player,
        DiverState state,
        PressureProfile profile,
        boolean physiologyEnabled,
        boolean magicalBypass
    ) {
        state.ceiling = physiologyEnabled ? DecompressionStops.depth(state.tissues, profile) : 0;

        // The current breath may empty a cylinder. Predict the next breath using the actual next
        // supply, rather than the mixture we just consumed.
        var nextSupply = BreathingEquipment.select(player);

        boolean nextSupplied = state.underwater && nextSupply != null && nextSupply.available() && !magicalBypass;

        GasMix nextMix = nextSupplied ? nextSupply.mix() : GasMix.AIR;

        double nextPressure = nextSupplied ? state.pressure : 1;

        state.stopSeconds = physiologyEnabled
                ? DecompressionStops.secondsToNextStop(
                    state.tissues,
                    profile,
                    nextMix,
                    nextPressure,
                    state.depth,
                    DivingConfig.SIMULATION_RATE.get(),
                    DivingConfig.DECOMPRESSION_RATE.get(),
                    !state.underwater || nextSupplied || magicalBypass
                )
                : 0;
        state.noDecoSeconds = physiologyEnabled
                ? state.tissues.noDecoSeconds(
                    nextMix,
                    nextPressure,
                    DivingConfig.SIMULATION_RATE.get(),
                    DivingConfig.DECOMPRESSION_RATE.get(),
                    profile.pressure(.05 - .000001)
                )
                : 3600;
    }

    /** Combine independent conditions into the existing warning bits used by the HUD and alarms. */
    private static void updateWarnings(
        ServerPlayer player,
        DiverState state,
        double oxygenPressure,
        boolean magicalBypass
    ) {
        if (state.supplied && state.gas < state.gasCapacity * 0.15) {
            state.warnings |= LOW_GAS;
        }

        if (state.underwater
            && BreathingEquipment.helmet(player)
            && !state.supplied
            && !magicalBypass) {
            state.warnings |= NO_SUPPLY;
        }

        if (state.emergency) {
            state.warnings |= BACKUP;
        }

        if (state.oxygenation < 70) {
            state.warnings |= HYPOXIA;
        }

        if (state.toxicity > 15
            || (state.supplied && oxygenPressure > DivingConfig.MAX_OXYGEN.get())) {
            state.warnings |= TOXICITY;
        }

        if (state.narcosis > 0) {
            state.warnings |= NARCOSIS;
        }

        if (state.ascentRate > DivingConfig.SAFE_ASCENT.get()
            && (state.underwater || state.ceiling > 0)) {
            state.warnings |= ASCENT;
        }

        if (state.ceiling > 0 || state.decompressionStress > 10) {
            state.warnings |= DECO;
        }

        if (state.blackedOut) {
            state.warnings |= BLACKOUT;
        }
    }

    private static void applyPhysiologyEffects(ServerPlayer player, DiverState state) {
        DivingEffects.physiology(player, state);

        if (player.tickCount % 60 == 0
            && (state.oxygenation < 10
                || state.toxicity > 140
                || state.decompressionStress > 50)) {
            player.hurt(player.damageSources().drown(), (float) (2 * DivingConfig.DAMAGE_MULTIPLIER.get()));
        }
    }

    private static void applyPressureEffects(
        ServerPlayer player,
        DiverState state,
        boolean historyValid,
        double severity
    ) {
        double rating = BreathingEquipment.protectionRating(player);

        boolean pressureOwned = DivingConfig.PRESSURE_DAMAGE.get()
            && !PressureCompatibility.suppressPressureDamage();

        if (state.underwater && state.pressure > rating) {
            state.warnings |= RATING;
        }

        if (!pressureOwned) {
            state.pressureInjury = 0;
            state.pressureShock = 0;
            return;
        }

        var injury = PressureInjury.step(
            state.pressureInjury,
            state.previousPressure,
            state.pressure,
            rating,
            state.underwater,
            historyValid,
            DivingConfig.SUDDEN_PRESSURE_INJURY.get(),
            DivingConfig.PRESSURE_SHOCK_DELTA.get(),
            DivingConfig.CATASTROPHIC_PRESSURE.get(),
            DivingConfig.CATASTROPHIC_DELTA.get(),
            DivingConfig.CATASTROPHIC_EXCESS.get(),
            severity
        );

        state.pressureInjury = injury.injury();
        state.pressureShock = injury.shock();

        if (state.pressureInjury > 10) {
            state.warnings |= PRESSURE_INJURY;
        }

        if (state.pressureShock > 0) {
            state.warnings |= PRESSURE_SHOCK;
        }

        // Sudden exposure takes precedence over the slower damage from exceeding a gear rating.
        double damage = 0;

        if (injury.catastrophic()) {
            damage = 40;
        } else if (state.pressureShock > 0) {
            damage = Math.min(12, state.pressureShock / 5);
        } else if (state.underwater && state.pressure > rating && player.tickCount % 60 == 0) {
            damage = Math.min(6, (state.pressure - rating) * severity);
        }

        if (damage > 0) {
            // Publish the sensory event before potentially fatal damage.
            send(player, state);

            var source = new net.minecraft.world.damagesource.DamageSource(
                player.registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(PRESSURE_DAMAGE_TYPE)
            );

            player.hurt(source, (float) (damage * DivingConfig.DAMAGE_MULTIPLIER.get()));
        }

        emitPressureParticles(player, state, injury.catastrophic());
    }

    /**
     * Pressure visuals share the same origin and severity decision, independent of damage immunity.
     */
    private static void emitPressureParticles(
        ServerPlayer player,
        DiverState state,
        boolean catastrophic
    ) {
        if (state.underwater
            && DivingConfig.BLOOD_PARTICLES.get()
            && (state.pressureShock > 0
                || state.pressureInjury > 30 && player.tickCount % 60 == 0)) {
            int count = 8;

            if (catastrophic) {
                count = 64;
            } else if (state.pressureShock > 0) {
                count = 28;
            }

            player.serverLevel()
                .sendParticles(
                    new net.minecraft.core.particles.DustParticleOptions(
                        new org.joml.Vector3f(0.48f, 0.015f, 0.025f),
                        1.2f
                    ),
                    player.getX(),
                    player.getEyeY() - .2,
                    player.getZ(),
                    count,
                    .35,
                    .3,
                    .35,
                    .025
                );
            player.serverLevel()
                .sendParticles(
                    net.minecraft.core.particles.ParticleTypes.BUBBLE,
                    player.getX(),
                    player.getEyeY() - .2,
                    player.getZ(),
                    count / 2,
                    .2,
                    .3,
                    .2,
                    .04
                );
        }
    }

    public static void reset(ServerPlayer player) {
        DivingEffects.clear(player);

        DiverState fresh = new DiverState();

        STATES.put(player.getUUID(), fresh);
        player.getPersistentData().put(STATE_KEY, fresh.save());
        DepthSampler.forget(player.getUUID());
        send(player, fresh);
    }

    static void send(ServerPlayer player, DiverState state) {
        PacketDistributor.sendToPlayer(player, DivingPackets.Snapshot.of(state, active(player)));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        DiverState state = STATES.remove(event.getEntity().getUUID());

        if (state != null) {
            event.getEntity().getPersistentData().put(STATE_KEY, state.save());
        }

        DepthSampler.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        STATES.remove(event.getEntity().getUUID());
        DepthSampler.forget(event.getEntity().getUUID());

        // Death is an explicit reset; End return retains the prior diver's obligations.
        if (!event.isWasDeath()) {
            event.getEntity()
                .getPersistentData()
                .put(STATE_KEY, event.getOriginal().getPersistentData().getCompound(STATE_KEY).copy());
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        STATES.clear();
        DepthSampler.clear();
    }

    private DiverService() {}
}
