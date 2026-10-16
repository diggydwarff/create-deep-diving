package com.diggydwarff.deepdiving.client.lighting;

import com.BreadRes.illuminationapi.client.ClientLightSource;
import com.diggydwarff.deepdiving.config.LampConfig;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL13;

import java.util.List;

/** One immutable lighting snapshot, prepared before the pack draws its materials. */
public final class LampMaterialBridge {
    record Light(ClientLightSource source, int depth, Matrix4f matrix, Vec3 origin, long revision) {}

    private static final LampShadowAtlas ATLAS = new LampShadowAtlas();

    private static List<Light> lights = List.of();

    private static Vec3 camera = Vec3.ZERO;

    private static Vec3 mediumOrigin = Vec3.ZERO;

    private static int mediumTexture;

    private static float outsideWater;

    private static boolean active;

    static void beginFrame() {
        active = false;
        lights = List.of();
    }

    static void publish(List<Light> values, Vec3 eye, LampMedium medium, boolean wet) {
        camera = eye;
        lights = List.copyOf(values);
        outsideWater = wet ? 1 : 0;

        if (!lights.isEmpty()) {
            mediumTexture = medium.texture();
            mediumOrigin = medium.origin().subtract(camera);
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + 4);
            ATLAS.update(lights);
        }

        active = true;
    }

    static boolean active() {
        return active;
    }

    public static int count() {
        return active && LampConfig.ENABLED.get() && LampConfig.SHADERS.get() ? lights.size() : 0;
    }

    public static int shadowTexture() {
        return ATLAS.texture();
    }

    public static int mediumTexture() {
        return mediumTexture;
    }

    public static Vector3f mediumOrigin() {
        return mediumOrigin.toVector3f();
    }

    public static Vector3f mediumSize() {
        return new Vector3f(LampMedium.X, LampMedium.Y, LampMedium.Z);
    }

    public static float outsideWater() {
        return outsideWater;
    }

    public static Vector4f vector(int index, String field) {
        if (index >= count()) {
            return new Vector4f();
        }

        Light light = lights.get(index);

        var source = light.source();

        return switch (field) {
            case "Position" -> new Vector4f(source.lightPos().subtract(camera).toVector3f(), source.range());
            case "Forward" -> new Vector4f(source.forward(), tangent(source.outerAngle()));
            case "Right" -> new Vector4f(source.right(), tangent(source.innerAngle()));
            case "ShadowOrigin" -> new Vector4f(light.origin().subtract(camera).toVector3f(), source.beamStart());
            case "Color" -> new Vector4f((source.color() >> 16 & 255) / 255f,
                (source.color() >> 8 & 255) / 255f, (source.color() & 255) / 255f, source.surfaceIntensity());
            default -> throw new IllegalArgumentException("Unknown lamp material field: " + field);
        };
    }

    public static Matrix4f matrix(int index) {
        return index < count() ? new Matrix4f(lights.get(index).matrix()) : new Matrix4f();
    }

    private static float tangent(float angle) {
        return (float) Math.tan(Math.toRadians(angle));
    }

    static void reset() {
        beginFrame();
        ATLAS.close();
        mediumTexture = 0;
        mediumOrigin = Vec3.ZERO;
        camera = Vec3.ZERO;
    }

    private LampMaterialBridge() {}
}
