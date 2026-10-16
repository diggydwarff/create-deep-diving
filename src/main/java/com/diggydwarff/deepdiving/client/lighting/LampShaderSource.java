package com.diggydwarff.deepdiving.client.lighting;

/** Small, checked edits to the pinned MIT library shader; the bundled JAR stays unchanged. */
public final class LampShaderSource {
    public static String adapt(String source, String medium) {
        source = replace(source, "uniform vec2 ShadowTexel;", "uniform vec2 ShadowTexel;\n" + medium);
        source = replace(source, "biasedWorldPos - LightPos", "biasedWorldPos - ShadowOrigin");

        source = replace(source, "vec3 dx = dFdx(surfaceWorld);\n"
                + "            vec3 dy = dFdy(surfaceWorld);\n"
                + "            vec3 normal = normalize(cross(dx, dy));",
            "vec3 normal = surfaceNormal(uv, surfaceWorld);");
        source = replace(source, "float ndotl = abs(dot(normal, toLight));",
            "float ndotl = max(0.0, dot(normal, toLight));");
        source = replace(source, "float worldBias = 0.012 + (1.0 - ndotl) * 0.028;\n"
                + "            float visibility = shadowVisibility(surfaceWorld, worldBias);",
            "float visibility = surfaceShadow(surfaceWorld, normal, ndotl);");
        source = replace(source, "float grazing = mix(0.38, 1.0, pow(ndotl, 0.55));",
            "float grazing = mix(0.60, 1.0, ndotl);");

        // There is no visible bulb at the virtual emitter. The upstream internal
        // cone also lit a diver's face when the lamp was close to the camera.
        source = removeInternalGlow(source);
        source = replace(source, "if (profile > 0.0) {", "if (profile > 0.0 && SurfaceIntensity > 0.00001) {");

        source = replace(source,
            "added += LightColor * profile * visibility * distanceFalloff * grazing * SurfaceIntensity;",
            "float endFade = 1.0 - smoothstep(Range * 0.65, Range, physicalAxial);\n"
                + "            added += LightColor * profile * visibility * distanceFalloff * grazing * SurfaceIntensity"
                + " * endFade * lampTransmission(LightPos, surfaceWorld) * viewTransmission(surfaceWorld)"
                + " * surfaceResponse(uv, surfaceWorld);");
        source = replace(source,
            "float edge = 1.0 - smoothstep(0.74, 1.0, radial);\n"
                + "    return hotspot * 1.52 + corona * 0.66 + edge * 0.14;",
            "float edge = 1.0 - smoothstep(0.55, 1.0, radial);\n"
                + "    return (hotspot * 1.30 + corona * 0.60) * edge;");
        source = replace(source,
            "if (rayConeInterval(CameraPos, rayDir, marchLimit, tEnter, tExit)) {",
            "if (VolumeIntensity > 0.00001 && rayConeInterval(CameraPos, rayDir, marchLimit, tEnter, tExit)) {");
        source = replace(source, "const int STEPS = 28;", "const int STEPS = 16;");
        source = replace(source,
            "float startFade = smoothstep(0.0, 0.012, physicalAxial);",
            "float startFade = smoothstep(0.25, 0.85, physicalAxial);");
        source = replace(source,
            "accumulated += profile * visibility * edgeMist * startFade * endFade * receiverFade * phase * stepLength;",
            "vec2 medium = mediumAt(samplePos);\n"
                + "            float scatter = medium.x * (0.25 + min(medium.y, 3.0) * 1.2);\n"
                + "            accumulated += profile * visibility * edgeMist * startFade * endFade * receiverFade"
                + " * phase * stepLength * scatter * volumeTransmission(samplePos);");
        source = replace(source, "float depth = texture(DepthTex, uv).r;",
            "float depth = texture(DepthTex, uv).r;\n"
                + "    float frontDepth = texture(FrontDepthTex, uv).r;\n"
                + "    if (MaskHand > 0.5 && frontDepth < 0.56 && frontDepth + 0.00001 < depth) {\n"
                + "        fragColor = vec4(0.0);\n        return;\n    }");
        return source;
    }

    private static String removeInternalGlow(String source) {
        String start = "        float internalAxial;";

        String finish = "        if (profile > 0.0) {";

        int first = source.indexOf(start);

        int last = source.indexOf(finish, first);

        if (first < 0 || last < first) {
            throw new IllegalStateException("Illumination API internal glow shader changed.");
        }

        return replace(source, source.substring(first, last), "");
    }

    private static String replace(String source, String before, String after) {
        int index = source.indexOf(before);

        if (index < 0 || source.indexOf(before, index + before.length()) >= 0) {
            throw new IllegalStateException("Illumination API shader changed near: " + before);
        }

        return source.substring(0, index) + after + source.substring(index + before.length());
    }

    private LampShaderSource() {}
}
