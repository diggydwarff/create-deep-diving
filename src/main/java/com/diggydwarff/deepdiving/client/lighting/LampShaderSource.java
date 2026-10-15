package com.diggydwarff.deepdiving.client.lighting;

/** Small, checked edits to the pinned MIT library shader; the bundled JAR stays unchanged. */
public final class LampShaderSource {
    public static String adapt(String source, String medium) {
        source = replace(source, "uniform vec2 ShadowTexel;", "uniform vec2 ShadowTexel;\n" + medium);
        source = replace(source, "biasedWorldPos - LightPos", "biasedWorldPos - ShadowOrigin");
        source = replace(source,
            "added += LightColor * profile * visibility * distanceFalloff * grazing * SurfaceIntensity;",
            "float endFade = 1.0 - smoothstep(Range * 0.65, Range, physicalAxial);\n"
                + "            added += LightColor * profile * visibility * distanceFalloff * grazing * SurfaceIntensity"
                + " * endFade * lampTransmission(LightPos, surfaceWorld) * viewTransmission(surfaceWorld);");
        source = replace(source,
            "if (rayConeInterval(CameraPos, rayDir, marchLimit, tEnter, tExit)) {",
            "if (VolumeIntensity > 0.00001 && rayConeInterval(CameraPos, rayDir, marchLimit, tEnter, tExit)) {");
        source = replace(source, "const int STEPS = 28;", "const int STEPS = 16;");
        source = replace(source,
            "accumulated += profile * visibility * edgeMist * startFade * endFade * receiverFade * phase * stepLength;",
            "vec2 medium = mediumAt(samplePos);\n"
                + "            float scatter = medium.x * (1.0 + min(medium.y, 3.0) * 1.6);\n"
                + "            accumulated += profile * visibility * edgeMist * startFade * endFade * receiverFade"
                + " * phase * stepLength * scatter * volumeTransmission(samplePos);");
        source = replace(source, "float depth = texture(DepthTex, uv).r;",
            "float depth = texture(DepthTex, uv).r;\n"
                + "    if (MaskHand > 0.5 && texture(FrontDepthTex, uv).r < 0.56) {\n"
                + "        fragColor = vec4(0.0);\n        return;\n    }");
        return source;
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
