package com.diggydwarff.deepdiving.config;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class DivingConfig {
    public enum Difficulty {
        ARCADE,
        STANDARD,
        HARDCORE
    }

    public static final ModConfigSpec SERVER;

    public static final ModConfigSpec CLIENT;

    public static final ModConfigSpec.BooleanValue ENABLED;

    public static final ModConfigSpec.BooleanValue PHYSIOLOGY;

    public static final ModConfigSpec.BooleanValue PRESSURE_DAMAGE;

    public static final ModConfigSpec.BooleanValue CREATE_AIR;

    public static final ModConfigSpec.BooleanValue CREATE_HELMET_NIGHT_VISION;

    public static final ModConfigSpec.BooleanValue BREATHING_BUBBLES;

    public static final ModConfigSpec.BooleanValue ORBIT_CAMERA;

    public static final ModConfigSpec.BooleanValue WATER_BREATHING_BYPASS;

    public static final ModConfigSpec.BooleanValue DEPTH_HOLD;

    public static final ModConfigSpec.BooleanValue VISUAL_EFFECTS;

    public static final ModConfigSpec.BooleanValue HUD;

    public static final ModConfigSpec.BooleanValue AUTO_PRESSURE_COMPAT;

    public static final ModConfigSpec.BooleanValue SUDDEN_PRESSURE_INJURY;

    public static final ModConfigSpec.BooleanValue CATASTROPHIC_PRESSURE;

    public static final ModConfigSpec.BooleanValue BLOOD_PARTICLES;

    public static final ModConfigSpec.BooleanValue EYELID_EFFECTS;

    public static final ModConfigSpec.BooleanValue BLOOD_EFFECTS;

    public static final ModConfigSpec.BooleanValue CAMERA_EFFECTS;

    public static final ModConfigSpec.BooleanValue SOUND_EFFECTS;

    public static final ModConfigSpec.BooleanValue SHADER_EFFECTS;

    public static final ModConfigSpec.BooleanValue HYPOXIA_SHADER;

    public static final ModConfigSpec.BooleanValue NARCOSIS_SHADER;

    public static final ModConfigSpec.BooleanValue TOXICITY_SHADER;

    public static final ModConfigSpec.BooleanValue DECOMPRESSION_SHADER;

    public static final ModConfigSpec.BooleanValue PRESSURE_SHADER;

    public static final ModConfigSpec.BooleanValue REDUCED_MOTION;

    public static final ModConfigSpec.BooleanValue SHADER_PACK_FALLBACK;

    public static final ModConfigSpec.BooleanValue SHADER_OVERLAY_FALLBACK;

    public static final ModConfigSpec.BooleanValue COMPUTER_ALARMS;

    public static final ModConfigSpec.BooleanValue DEEP_SEAS_COMPARTMENTS;

    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>>
            EXTERNAL_PRESSURE_OWNERS;

    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>>
            DIMENSION_PROFILES;

    public static final ModConfigSpec.DoubleValue BLOCKS_PER_ATM;

    public static final ModConfigSpec.DoubleValue GAS_PER_SECOND;

    public static final ModConfigSpec.DoubleValue SIMULATION_RATE;

    public static final ModConfigSpec.DoubleValue DECOMPRESSION_RATE;

    public static final ModConfigSpec.DoubleValue DAMAGE_MULTIPLIER;

    public static final ModConfigSpec.DoubleValue SAFE_ASCENT;

    public static final ModConfigSpec.DoubleValue REFERENCE_DEPTH;

    public static final ModConfigSpec.DoubleValue REFERENCE_PRESSURE;

    public static final ModConfigSpec.DoubleValue PRESSURE_EXPONENT;

    public static final ModConfigSpec.DoubleValue COPPER_RATING;

    public static final ModConfigSpec.DoubleValue NETHERITE_RATING;

    public static final ModConfigSpec.DoubleValue ADVANCED_RATING;

    public static final ModConfigSpec.DoubleValue HIGH_CAPACITY_RATING;

    public static final ModConfigSpec.DoubleValue EMERGENCY_RATING;

    public static final ModConfigSpec.DoubleValue UNPROTECTED_RATING;

    public static final ModConfigSpec.DoubleValue MIN_OXYGEN;

    public static final ModConfigSpec.DoubleValue MAX_OXYGEN;

    public static final ModConfigSpec.DoubleValue NARCOSIS_START;

    public static final ModConfigSpec.DoubleValue HYPOXIA_RATE;

    public static final ModConfigSpec.DoubleValue RECOVERY_RATE;

    public static final ModConfigSpec.DoubleValue PRESSURE_SHOCK_DELTA;

    public static final ModConfigSpec.DoubleValue CATASTROPHIC_DELTA;

    public static final ModConfigSpec.DoubleValue CATASTROPHIC_EXCESS;

    public static final ModConfigSpec.DoubleValue EFFECT_INTENSITY;

    public static final ModConfigSpec.DoubleValue ALARM_VOLUME;

    public static final ModConfigSpec.DoubleValue SHADER_INTENSITY;

    public static final ModConfigSpec.DoubleValue SHADER_CENTER_CALM;

    public static final ModConfigSpec.IntValue SURFACE_SEARCH_LIMIT;

    public static final ModConfigSpec.IntValue HUD_X;

    public static final ModConfigSpec.IntValue HUD_Y;

    public static final ModConfigSpec.IntValue FIXED_SURFACE_Y;

    public static final ModConfigSpec.IntValue ALARM_COOLDOWN;

    public static final ModConfigSpec.EnumValue<PressureProfile.Mode> PRESSURE_MODE;

    public enum SurfaceMode {
        LOCAL_WATER,
        FIXED
    }

    public static final ModConfigSpec.EnumValue<SurfaceMode> SURFACE_MODE;

    public static final ModConfigSpec.EnumValue<Difficulty> PRESET;

    public enum ShaderQuality {
        LOW,
        HIGH
    }

    public static final ModConfigSpec.EnumValue<ShaderQuality> SHADER_QUALITY;

    static {
        var builder = new ModConfigSpec.Builder();

        ENABLED = builder.comment("Master switch. Disable when another mod owns diving simulation.")
            .define("enabled", true);
        PHYSIOLOGY = builder.define("physiology", true);
        PRESSURE_DAMAGE = builder.define("equipmentPressureDamage", true);
        CREATE_AIR = builder.comment("Replace Create's underwater drain with one pressure-scaled drain.")
            .define("manageCreateBacktanks", true);
        CREATE_HELMET_NIGHT_VISION = builder.comment(
            "Allow Create diving helmets' enhanced underwater visibility (often called night vision).",
            "Create 6 extends underwater fog distance; disabling this keeps ordinary water fog.",
            "Night Vision potions, lamps and netherite helmets' lava vision are unaffected."
        )
            .define("createHelmetNightVision", false);
        BREATHING_BUBBLES =
            builder.comment("Emit small periodic exhalation bubbles while supplied diving gear breathes underwater.")
                .define("breathingBubbles", true);
        WATER_BREATHING_BYPASS =
            builder.comment("Magic breathing avoids gas requirements but does not erase prior tissue loading.")
                .define("waterBreathingBypass", true);
        AUTO_PRESSURE_COMPAT =
            builder.comment("Suppress equipment-pressure damage when Crush Depth owns that mechanic.")
                .define("autoAvoidDuplicatePressureDamage", true);
        EXTERNAL_PRESSURE_OWNERS = builder.comment("Additional installed mod IDs that own player pressure damage.")
            .defineListAllowEmpty("externalPressureOwners", java.util.List.of(), value -> value instanceof String);
        PRESET = builder.defineEnum("difficulty", Difficulty.STANDARD);
        BLOCKS_PER_ATM = builder.defineInRange("blocksPerAtmosphere", 10.0, 0.1, 65536);
        PRESSURE_MODE = builder.comment(
            "LINEAR preserves existing worlds. REFERENCE maps a configured ocean depth to"
                    + " referencePressure."
        )
            .defineEnum("pressureMode", PressureProfile.Mode.LINEAR);
        REFERENCE_DEPTH = builder.defineInRange("referenceOceanDepth", 64.0, 1, 65536);
        REFERENCE_PRESSURE = builder.defineInRange("referencePressure", 21.0, 1.01, 1000);
        PRESSURE_EXPONENT = builder.comment(
            "1 = straight curve; greater than 1 makes shallow water gentler. Pressure continues"
                    + " beyond reference depth."
        )
            .defineInRange("pressureCurveExponent", 1.35, 0.25, 4);
        SURFACE_MODE = builder.comment(
            "LOCAL_WATER measures the local surface. FIXED supports roofed oceans and flooded"
                    + " dimensions."
        )
            .defineEnum("surfaceMode", SurfaceMode.LOCAL_WATER);
        FIXED_SURFACE_Y = builder.defineInRange("fixedSurfaceY", 63, -65536, 65536);
        DIMENSION_PROFILES = builder.comment(
            "Optional overrides: dimension|LINEAR or REFERENCE|scale|reference atm|curve|AUTO"
                    + " or surface Y. Last duplicate wins.",
            "Example: minecraft:overworld|REFERENCE|200|21|1.35|AUTO"
        )
            .defineListAllowEmpty("dimensionPressureProfiles", java.util.List.of(), PressureProfile::validEntry);
        COPPER_RATING = builder.defineInRange("copperBacktankPressureRating", 6.0, 1, 1000);
        NETHERITE_RATING = builder.defineInRange("netheriteBacktankPressureRating", 12.0, 1, 1000);
        ADVANCED_RATING = builder.defineInRange("advancedCylinderPressureRating", 12.0, 1, 1000);
        HIGH_CAPACITY_RATING = builder.defineInRange("highCapacityCylinderPressureRating", 21.0, 1, 1000);
        EMERGENCY_RATING = builder.defineInRange("emergencyCylinderPressureRating", 12.0, 1, 1000);
        UNPROTECTED_RATING =
            builder.comment("Game protection threshold for a diver without a rated helmet and chest supply.")
                .defineInRange("unprotectedPressureRating", 6.0, 1, 1000);
        MIN_OXYGEN = builder.defineInRange("minimumOxygenPartialPressure", 0.16, 0.01, 1);
        MAX_OXYGEN = builder.defineInRange("maximumOxygenPartialPressure", 1.4, 1.01, 10);
        NARCOSIS_START = builder.defineInRange("narcosisNitrogenPressure", 3.2, 0.1, 1000);
        HYPOXIA_RATE = builder.defineInRange("hypoxiaDepletionPerSecond", 7.0, 0.1, 100);
        RECOVERY_RATE = builder.defineInRange("oxygenRecoveryPerSecond", 5.0, 0.1, 100);
        SUDDEN_PRESSURE_INJURY =
            builder.comment("Unprotected rapid compression causes injury even with no breathing supply.")
                .define("suddenPressureInjury", true);
        PRESSURE_SHOCK_DELTA = builder.defineInRange("pressureShockDelta", 4.0, 0.1, 1000);
        CATASTROPHIC_PRESSURE =
            builder.comment("Extreme sudden exposure can be fatal; not a literal whole-body implosion model.")
                .define("catastrophicPressureExposure", true);
        CATASTROPHIC_DELTA = builder.defineInRange("catastrophicPressureDelta", 12.0, 0.1, 1000);
        CATASTROPHIC_EXCESS = builder.defineInRange("catastrophicPressureExcess", 8.0, 0.1, 1000);
        BLOOD_PARTICLES = builder.define("pressureBloodParticles", true);
        DEEP_SEAS_COMPARTMENTS = builder.comment(
            "Optional, guarded detection of dry Create Deep Seas compartments. No submarine"
                    + " dependency required."
        )
            .define("deepSeasCompartmentSupport", true);
        GAS_PER_SECOND = builder.defineInRange("gasPerSecondAtSurface", 8.0, 0.1, 100);
        SIMULATION_RATE =
            builder.comment("Scales all tissue time. Use decompressionRate to shorten stops without faster loading.")
                .defineInRange("tissueRate", 1.0, 0.1, 10);
        DECOMPRESSION_RATE = builder.comment(
            "Off-gassing speed only, multiplied by tissueRate. Does not accelerate gas uptake.",
            "6 gives short game-friendly stops; 1 restores the previous off-gassing pace."
        )
            .defineInRange("decompressionRate", 6.0, 0.1, 60);
        DEPTH_HOLD = builder.comment("Allow the depth-hold hotkey with diving equipment, a computer and breathing gas.")
            .define("allowDepthHold", true);
        DAMAGE_MULTIPLIER = builder.defineInRange("damageMultiplier", 1.0, 0, 10);
        SAFE_ASCENT = builder.defineInRange("maximumAscentBlocksPerSecond", 0.75, 0.1, 10);
        SURFACE_SEARCH_LIMIT = builder.comment("Bounded local surface search, cached per diver for five seconds.")
            .defineInRange("surfaceSearchLimit", 2048, 64, 8192);
        SERVER = builder.build();
        builder = new ModConfigSpec.Builder();
        HUD = builder.define("showHud", true);
        ORBIT_CAMERA = builder.comment(
            "Add an independent swimming orbit view after the normal front view in the perspective cycle."
        )
            .define("swimmingOrbitCamera", true);
        HUD_X = builder.defineInRange("hudX", 8, 0, 4096);
        HUD_Y = builder.defineInRange("hudY", 8, 0, 4096);
        VISUAL_EFFECTS = builder.define("visualAndSoundEffects", true);
        EFFECT_INTENSITY = builder.defineInRange("effectIntensity", 1.0, 0, 1);
        EYELID_EFFECTS = builder.define("eyelidBlinks", true);
        BLOOD_EFFECTS = builder.define("pressureBloodOverlay", true);
        CAMERA_EFFECTS = builder.define("cameraSway", true);
        SOUND_EFFECTS = builder.define("distressSounds", true);
        REDUCED_MOTION =
            builder.comment("Use steady vision changes; disable camera sway and animated distortion/pulses.")
                .define("reducedMotion", false);
        builder.push("shaders");
        SHADER_EFFECTS = builder.comment("World-only post processing. Menus and the dive computer remain sharp.")
            .define("enabled", true);
        SHADER_INTENSITY = builder.defineInRange("intensity", 1.0, 0, 1);
        SHADER_QUALITY = builder.comment("LOW uses fewer blur samples and omits colour separation.")
            .defineEnum("quality", ShaderQuality.HIGH);
        SHADER_CENTER_CALM =
            builder.comment("0 affects the whole view equally; 1 concentrates distortion/blur at the edges.")
                .defineInRange("centerCalm", 0.75, 0, 1);
        HYPOXIA_SHADER = builder.define("hypoxia", true);
        NARCOSIS_SHADER = builder.define("narcosis", true);
        TOXICITY_SHADER = builder.define("oxygenToxicity", true);
        DECOMPRESSION_SHADER = builder.define("decompressionSickness", true);
        PRESSURE_SHADER = builder.define("pressureInjury", true);
        SHADER_PACK_FALLBACK =
            builder.comment("Automatically use overlays while an Iris shader pack is active, or with Oculus.")
                .define("shaderPackFallback", true);
        SHADER_OVERLAY_FALLBACK =
            builder.comment("Retain gentle condition-specific edge cues if post processing is unavailable.")
                .define("overlayFallback", true);
        builder.pop();
        builder.push("computer");
        COMPUTER_ALARMS = builder.comment(
            "One quiet tone when a warning appears; urgent escalation can interrupt the"
                    + " cooldown."
        )
            .define("alarms", true);
        ALARM_VOLUME = builder.defineInRange("alarmVolume", 0.15, 0, 1);
        ALARM_COOLDOWN = builder.defineInRange("alarmCooldownSeconds", 8, 1, 300);
        builder.pop();
        CLIENT = builder.build();
    }

    public static double severity() {
        return switch (PRESET.get()) {
            case ARCADE -> 0.45;
            case STANDARD -> 1;
            case HARDCORE -> 1.6;
        };
    }

    public static PressureProfile pressureProfile(ResourceLocation dimension) {
        PressureProfile result = new PressureProfile(
            PRESSURE_MODE.get(),
            PRESSURE_MODE.get() == PressureProfile.Mode.LINEAR
                    ? BLOCKS_PER_ATM.get()
                    : REFERENCE_DEPTH.get(),
            REFERENCE_PRESSURE.get(),
            PRESSURE_EXPONENT.get(),
            SURFACE_MODE.get() == SurfaceMode.FIXED ? FIXED_SURFACE_Y.get() : null
        );

        for (String entry : DIMENSION_PROFILES.get()) {
            var override = PressureProfile.parse(entry);

            if (override.dimension().equals(dimension)) {
                result = override.profile();
            }
        }

        return result;
    }

    /** Disabling the addon's master switch restores Create's ordinary visual behavior. */
    public static boolean allowCreateHelmetVision() {
        return !ENABLED.get() || CREATE_HELMET_NIGHT_VISION.get();
    }

    private DivingConfig() {}
}
