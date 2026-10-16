package com.diggydwarff.deepdiving.client.lighting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Adds artificial light while Complementary still has the original material color. */
public final class LampMaterialShader {
    public static final String MARKER = "// DEEP_DIVING_MATERIAL_LIGHT_V1";

    private static final String ENTRY = "void DoLighting(";

    private static final String MIX = "    vec3 finalDiffuse =";

    public static String patch(String name, String source) {
        if (!name.startsWith("gbuffers_") || source.contains(MARKER)) {
            return source;
        }

        // Match the inspected shared lighting layouts, not arbitrary packs.
        if (!source.contains("pow2(directionShade * vanillaAO)")
            || !source.contains("color.rgb *= finalDiffuse;")
            || !source.contains("vec3 blockLighting =")
            || !source.contains("vec3 worldGeoNormal")) {
            return source;
        }

        int entry = unique(source, ENTRY);

        int mix = unique(source, MIX);

        if (entry < 0 || mix <= entry) {
            return source;
        }

        String injection = "    blockLighting += ddMaterialLight(playerPos, worldGeoNormal);\n";

        source = source.substring(0, mix) + injection + source.substring(mix);

        return source.substring(0, entry) + helpers() + "\n" + source.substring(entry);
    }

    private static int unique(String source, String text) {
        int first = source.indexOf(text);

        return first >= 0 && source.indexOf(text, first + text.length()) < 0 ? first : -1;
    }

    private static String helpers() {
        StringBuilder result = new StringBuilder(MARKER + "\n");

        for (int index = 0; index < 8; index++) {
            for (String field : new String[]{"Position", "Forward", "Right", "Color", "ShadowOrigin"}) {
                result.append("uniform vec4 ddLamp").append(field).append(index).append(";\n");
            }

            result.append("uniform mat4 ddLampMatrix").append(index).append(";\n");
        }

        try (var input = LampMaterialShader.class.getResourceAsStream(
            "/assets/deepdiving/shaders/lighting/material.glsl")) {
            if (input == null) {
                throw new IOException("Missing lamp material shader");
            }

            result.append(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException failure) {
            throw new IllegalStateException("Deep Diving: cannot load material lighting", failure);
        }

        result.append("\nvec3 ddMaterialLight(vec3 point, vec3 normal) {\n    vec3 light = vec3(0.0);\n");

        for (int index = 0; index < 8; index++) {
            result.append("    if (ddLampCount > ").append(index).append(") light += ddOneLamp(point, normal, ")
                .append(index).append(", ddLampPosition").append(index)
                .append(", ddLampForward").append(index).append(", ddLampRight").append(index)
                .append(", ddLampColor").append(index).append(", ddLampShadowOrigin").append(index)
                .append(", ddLampMatrix").append(index).append(");\n");
        }

        return result.append("    return light;\n}\n").toString();
    }

    private LampMaterialShader() {}
}
