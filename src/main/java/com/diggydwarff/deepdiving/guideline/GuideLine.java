package com.diggydwarff.deepdiving.guideline;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** A single dimension-local segment. The saved network owns its lifetime and refunds. */
public record GuideLine(
    UUID id,
    BlockPos a,
    BlockPos b,
    Direction aFace,
    Direction bFace,
    DyeColor color,
    boolean glowing
) {
    public static final double MAX_LENGTH = 64;

    public BlockPos other(BlockPos node) {
        return node.equals(a) ? b : a;
    }

    public Vec3 start() {
        return anchor(a, aFace);
    }

    public Vec3 end() {
        return anchor(b, bFace);
    }

    public static Vec3 anchor(BlockPos pos, Direction face) {
        // The pin head sits 2.5 model pixels out from its supporting face.
        return Vec3.atCenterOf(pos)
            .subtract(Vec3.atLowerCornerOf(face.getNormal()).scale(.5 - 2.5 / 16));
    }

    public Vec3 point(double fraction) {
        Vec3 start = start();

        Vec3 end = end();

        // The shallow sag is bounded so a floor-mounted guideline stays above its support.
        double maximumSag = Math.min(.08, start.distanceTo(end) * .006);

        double sag = -Math.sin(Math.PI * fraction) * maximumSag;

        return start.lerp(end, fraction).add(0, sag, 0);
    }

    public AABB bounds() {
        return new AABB(start(), end()).inflate(.25);
    }

    public GuideLine withGlow() {
        return new GuideLine(id, a, b, aFace, bFace, color, true);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putUUID("id", id);
        tag.putLong("a", a.asLong());
        tag.putLong("b", b.asLong());
        tag.putInt("af", aFace.get3DDataValue());
        tag.putInt("bf", bFace.get3DDataValue());
        tag.putInt("color", color.getId());
        tag.putBoolean("glow", glowing);
        return tag;
    }

    public static GuideLine load(CompoundTag tag) {
        if (!tag.hasUUID("id")) {
            return null;
        }

        BlockPos a = BlockPos.of(tag.getLong("a"));

        BlockPos b = BlockPos.of(tag.getLong("b"));

        if (a.equals(b) || a.distSqr(b) > MAX_LENGTH * MAX_LENGTH) {
            return null;
        }

        return new GuideLine(
            tag.getUUID("id"),
            a,
            b,
            Direction.from3DDataValue(tag.getInt("af")),
            Direction.from3DDataValue(tag.getInt("bf")),
            DyeColor.byId(tag.getInt("color")),
            tag.getBoolean("glow")
        );
    }

    /**
     * Return the squared separation and the fraction along the view ray, in that order. The network
     * uses the ray fraction to choose the closest visible cord segment.
     */
    public static double[] rayDistance(
        Vec3 segmentStart,
        Vec3 segmentEnd,
        Vec3 eye,
        Vec3 rayEnd
    ) {
        Vec3 segmentDirection = segmentEnd.subtract(segmentStart);

        Vec3 rayDirection = rayEnd.subtract(eye);

        Vec3 offsetFromEye = segmentStart.subtract(eye);

        double segmentLengthSquared = segmentDirection.dot(segmentDirection);

        double segmentRayDot = segmentDirection.dot(rayDirection);

        double rayLengthSquared = rayDirection.dot(rayDirection);

        double segmentOffsetDot = segmentDirection.dot(offsetFromEye);

        double rayOffsetDot = rayDirection.dot(offsetFromEye);

        // A zero-length segment cannot produce a useful hit or a safe division.
        if (segmentLengthSquared < 1e-10 || rayLengthSquared < 1e-10) {
            return new double[] {Double.POSITIVE_INFINITY, 1};
        }

        // Seed the closest-point solution, then refine it within both finite segments.
        double denominator = segmentLengthSquared * rayLengthSquared - segmentRayDot * segmentRayDot;

        double segmentFraction = denominator > 1e-10
                ? Math.clamp(
                    (segmentRayDot * rayOffsetDot - rayLengthSquared * segmentOffsetDot)
                            / denominator,
                    0,
                    1
                )
                : 0;

        double rayFraction = Math.clamp((segmentRayDot * segmentFraction + rayOffsetDot) / rayLengthSquared, 0, 1);

        segmentFraction = Math.clamp((segmentRayDot * rayFraction - segmentOffsetDot) / segmentLengthSquared, 0, 1);
        rayFraction = Math.clamp((segmentRayDot * segmentFraction + rayOffsetDot) / rayLengthSquared, 0, 1);
        return new double[] {
            segmentStart.add(segmentDirection.scale(segmentFraction))
                .distanceToSqr(eye.add(rayDirection.scale(rayFraction))),
            rayFraction
        };
    }
}
