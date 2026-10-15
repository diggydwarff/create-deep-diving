package com.diggydwarff.deepdiving.client.ambience;

import com.diggydwarff.deepdiving.compat.DeepSeasCompatibility;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class AmbienceWater {
    public static boolean contains(ClientLevel level, Vec3 point) {
        BlockPos pos = BlockPos.containing(point);

        if (!level.hasChunkAt(pos)) {
            return false;
        }

        var fluid = level.getFluidState(pos);

        return fluid.is(FluidTags.WATER)
            && point.y + .025 < pos.getY() + fluid.getHeight(level, pos)
            && !DeepSeasCompatibility.inDryCompartment(level, point);
    }

    public static boolean clearWater(ClientLevel level, Vec3 point) {
        if (!contains(level, point)) {
            return false;
        }

        return level.noCollision(new AABB(point.x - .035, point.y - .025, point.z - .035,
            point.x + .035, point.y + .025, point.z + .035));
    }

    public static boolean connected(ClientLevel level, Player player, Vec3 point) {
        Vec3 eye = player.getEyePosition();

        int steps = Math.max(1, (int) Math.ceil(eye.distanceTo(point) * 2));

        for (int index = 1; index <= steps; index++) {
            if (!contains(level, eye.lerp(point, index / (double) steps))) {
                return false;
            }
        }

        // Reject particles behind a solid face, including waterlogged walls and slabs.
        return level.clip(new ClipContext(eye, point, ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }

    private AmbienceWater() {}
}
