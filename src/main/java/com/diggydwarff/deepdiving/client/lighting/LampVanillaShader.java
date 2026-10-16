package com.diggydwarff.deepdiving.client.lighting;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Lights the texture before vanilla fog and the depth mod can make it black. */
public final class LampVanillaShader {
    private static final String MARKER = "// DEEP_DIVING_VANILLA_MATERIAL_V1";

    private static final Set<String> TERRAIN = Set.of(
        "rendertype_solid", "rendertype_cutout", "rendertype_cutout_mipped",
        "rendertype_translucent", "rendertype_tripwire"
    );

    private static final Set<String> ENTITIES = Set.of(
        "rendertype_entity_solid", "rendertype_entity_cutout", "rendertype_entity_cutout_no_cull",
        "rendertype_entity_translucent", "rendertype_entity_translucent_cull"
    );

    private static final Set<String> PATCHED_VERTICES = ConcurrentHashMap.newKeySet();

    public static String vanilla(String name, boolean vertex, String source) {
        if (name.startsWith("minecraft:")) {
            name = name.substring("minecraft:".length());
        }

        boolean terrain = TERRAIN.contains(name);

        if ((!terrain && !ENTITIES.contains(name)) || source.contains(MARKER)) {
            return source;
        }

        if (vertex) {
            PATCHED_VERTICES.remove(name);

            String position = terrain ? "pos" : "Position";

            String transform = "gl_Position = ProjMat * ModelViewMat * vec4(" + position + ", 1.0);";

            if (!source.contains(transform) || !source.contains("in vec4 Color;")) {
                return source;
            }

            PATCHED_VERTICES.add(name);
            source = beforeMain(source, vertexDeclarations());
            return source.replace(transform, transform + "\n"
                + "    ddWorldPosition = (ddInverseView * ModelViewMat * vec4(" + position + ", 1.0)).xyz;\n"
                + "    ddMaterialTint = Color.rgb;");
        }

        String fog = "fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);";

        if (!PATCHED_VERTICES.contains(name) || !source.contains(fog)) {
            return source;
        }

        source = beforeMain(source, fragmentDeclarations());
        return fragmentNormals(source).replace(fog,
            "color.rgb += texture(Sampler0, texCoord0).rgb * ddMaterialTint * ColorModulator.rgb * ddVanillaLight(ddFaceNormal);\n"
                + "    " + fog);
    }

    /** Sodium 0.6 and 0.8 use different material fields, but the same texture stage. */
    public static String sodium(String path, String source) {
        if (source.contains(MARKER)) {
            return source;
        }

        if (path.equals("blocks/block_layer_opaque.vsh")) {
            PATCHED_VERTICES.remove("sodium");

            String transform = "gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);";

            if (!source.contains(transform) || !source.contains("v_Color = _vert_color * texture(")) {
                return source;
            }

            PATCHED_VERTICES.add("sodium");
            source = beforeMain(source, vertexDeclarations());
            return source.replace(transform, transform + "\n"
                + "    ddWorldPosition = (ddInverseView * u_ModelViewMatrix * vec4(position, 1.0)).xyz;\n"
                + "    ddMaterialTint = _vert_color.rgb;");
        }

        if (!path.equals("blocks/block_layer_opaque.fsh") || !PATCHED_VERTICES.contains("sodium")) {
            return source;
        }

        String color = source.contains("vec4 diffuseColor =") ? "diffuseColor" : "color";

        String fog = "fragColor = _linearFog(" + color + ", v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);";

        String texture = color.equals("diffuseColor")
            ? "texture(u_BlockTex, v_TexCoord, v_MaterialMipBias)"
            : "texture(u_BlockTex, v_TexCoord, lodBias)";

        if (!source.contains(fog) || !source.contains(texture)) {
            return source;
        }

        source = beforeMain(source, fragmentDeclarations());
        return fragmentNormals(source).replace(fog, color + ".rgb += " + texture + ".rgb * ddMaterialTint * ddVanillaLight(ddFaceNormal);\n"
            + "    " + fog);
    }

    private static String vertexDeclarations() {
        return MARKER + "\n"
            + "uniform mat4 ddInverseView;\n"
            + "out vec3 ddWorldPosition;\n"
            + "out vec3 ddMaterialTint;\n";
    }

    private static String fragmentDeclarations() {
        return MARKER + "\n"
            + "in vec3 ddWorldPosition;\n"
            + "in vec3 ddMaterialTint;\n"
            + LampMaterialShader.helpers() + "\n"
            + "vec3 ddVanillaLight(vec3 normal) {\n"
            + "    if (ddLampCount == 0 || dot(normal, normal) < 1e-16) return vec3(0.0);\n"
            + "    return ddMaterialLight(ddWorldPosition, normalize(normal));\n"
            + "}\n";
    }

    private static String beforeMain(String source, String declarations) {
        return source.replace("void main() {", declarations + "\nvoid main() {");
    }

    private static String fragmentNormals(String source) {
        // Evaluate derivatives before an alpha-test can discard a neighboring pixel.
        return source.replace("void main() {", "void main() {\n"
            + "    vec3 ddFaceNormal = cross(dFdx(ddWorldPosition), dFdy(ddWorldPosition));");
    }

    private LampVanillaShader() {}
}
