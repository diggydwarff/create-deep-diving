package com.diggydwarff.deepdiving.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Value definitions contain no Minecraft client or OpenGL classes. */
public final class LampConfig {
    public enum Color {
        WARM,
        COOL,
        CUSTOM
    }

    public static ModConfigSpec.BooleanValue ENABLED;

    public static ModConfigSpec.BooleanValue NEARBY;

    public static ModConfigSpec.BooleanValue REDUCED;

    public static ModConfigSpec.BooleanValue SHADERS;

    public static ModConfigSpec.BooleanValue ENTITY_SHADOWS;

    public static ModConfigSpec.DoubleValue BRIGHTNESS;

    public static ModConfigSpec.DoubleValue RANGE;

    public static ModConfigSpec.DoubleValue WIDTH;

    public static ModConfigSpec.DoubleValue VOLUME;

    public static ModConfigSpec.DoubleValue TRANSITION;

    public static ModConfigSpec.DoubleValue SHADOW_SOFTNESS;

    public static ModConfigSpec.IntValue NEARBY_LIMIT;

    public static ModConfigSpec.IntValue SHADOW_LIMIT;

    public static ModConfigSpec.IntValue SHADOW_UPDATES;

    public static ModConfigSpec.IntValue SHADOW_RATE;

    public static ModConfigSpec.EnumValue<Color> COLOR;

    public static ModConfigSpec.IntValue CUSTOM_COLOR;

    public static void define(ModConfigSpec.Builder builder) {
        builder.push("lamp");
        ENABLED = builder.define("enabled", true);
        BRIGHTNESS = builder.defineInRange("brightness", 1.0, 0, 3);
        RANGE = builder.defineInRange("range", 16.0, 3, 24);
        WIDTH = builder.comment("Full spotlight angle in degrees.")
            .defineInRange("beamWidth", 44.0, 12, 100);
        COLOR = builder.defineEnum("color", Color.WARM);
        CUSTOM_COLOR = builder.comment("RGB as a decimal integer; used when color is CUSTOM.")
            .defineInRange("customColor", 0xFFF0D5, 0, 0xFFFFFF);
        VOLUME = builder.defineInRange("volumetricIntensity", .7, 0, 2);
        TRANSITION = builder.defineInRange("transitionSeconds", .25, .05, 2);
        SHADOW_SOFTNESS = builder.comment("Lamp shadow edge softness. Zero keeps crisp shadows; one is a small diffuse edge.")
            .defineInRange("shadowSoftness", 1.0, 0, 3);
        NEARBY = builder.define("nearbyDivers", true);
        NEARBY_LIMIT = builder.defineInRange("nearbyLightLimit", 2, 0, 7);
        REDUCED = builder.comment("One shadowed lamp, shorter range and no volumetric ray marching.")
            .define("reducedEffects", false);
        SHADERS = builder.comment("Use the Iris material-lighting and depth-buffer bridges with shader packs.",
            "Complementary/Euphoria are integration targets; in-game compatibility is not yet certified.")
            .define("shaderLighting", true);
        ENTITY_SHADOWS = builder.define("entityShadows", true);
        SHADOW_LIMIT = builder.comment("Hard lamp/shadow-map cap. Excess distant lamps are omitted rather than leaking through walls.")
            .defineInRange("maximumShadowedLights", 3, 1, 8);
        SHADOW_UPDATES = builder.defineInRange("shadowUpdatesPerFrame", 2, 1, 8);
        SHADOW_RATE = builder.defineInRange("shadowUpdatesPerSecond", 20, 2, 30);
        builder.pop();
    }

    public static int color() {
        return switch (COLOR.get()) {
            case WARM -> 0xFFF0D5;
            case COOL -> 0xC8E9FF;
            case CUSTOM -> CUSTOM_COLOR.get();
        };
    }

    private LampConfig() {}
}
