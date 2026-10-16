package com.diggydwarff.deepdiving.client.lighting;

import com.diggydwarff.deepdiving.client.sediment.SedimentClouds;
import com.diggydwarff.deepdiving.compat.DeepSeasCompatibility;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

/** Small local water/sediment texture shared by every lamp in the frame. */
final class LampMedium {
    static final int X = 41;

    // Cover a full default-range beam vertically as well as horizontally.
    static final int Y = 41;

    static final int Z = 41;

    private int texture;

    private BlockPos origin;

    private long updated = Long.MIN_VALUE;

    private final float[] values = new float[X * Y * Z * 2];

    void update(ClientLevel level, Vec3 camera) {
        BlockPos center = BlockPos.containing(camera);

        boolean moved = origin == null || Math.abs(center.getX() - origin.getX() - X / 2) >= 3
            || Math.abs(center.getY() - origin.getY() - Y / 2) >= 3
            || Math.abs(center.getZ() - origin.getZ() - Z / 2) >= 3;

        if (!moved && level.getGameTime() - updated < 4) {
            return;
        }

        origin = center.offset(-X / 2, -Y / 2, -Z / 2);
        updated = level.getGameTime();

        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

        for (int z = 0; z < Z; z++) {
            for (int y = 0; y < Y; y++) {
                for (int x = 0; x < X; x++) {
                    position.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);

                    int index = ((z * Y + y) * X + x) * 2;

                    var fluid = level.hasChunkAt(position) ? level.getFluidState(position) : null;

                    Vec3 point = position.getCenter();

                    boolean wet = fluid != null && fluid.is(FluidTags.WATER)
                        && !DeepSeasCompatibility.inDryCompartment(level, point);

                    values[index] = wet ? fluid.getHeight(level, position) : 0;
                    values[index + 1] = wet ? (float) SedimentClouds.densityAt(point) : 0;
                }
            }
        }

        int previous = GL11.glGetInteger(GL12.GL_TEXTURE_BINDING_3D);

        int unpack = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);

        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);

        if (texture == 0) {
            texture = GL11.glGenTextures();
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, texture);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
            GL12.glTexImage3D(GL12.GL_TEXTURE_3D, 0, GL30.GL_RG32F, X, Y, Z, 0,
                GL30.GL_RG, GL11.GL_FLOAT, values);
        } else {
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, texture);
            GL12.glTexSubImage3D(GL12.GL_TEXTURE_3D, 0, 0, 0, 0, X, Y, Z,
                GL30.GL_RG, GL11.GL_FLOAT, values);
        }

        GL11.glBindTexture(GL12.GL_TEXTURE_3D, previous);
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpack);
    }

    int texture() {
        return texture;
    }

    Vec3 origin() {
        return Vec3.atLowerCornerOf(origin);
    }

    void close() {
        if (texture != 0) {
            GL11.glDeleteTextures(texture);
        }
        texture = 0;
        origin = null;
        updated = Long.MIN_VALUE;
    }
}
