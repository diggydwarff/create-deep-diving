package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.physiology.BuoyancyMotion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/** Hold a rest position softly; directional input immediately restores ordinary swimming. */
final class BuoyancyClient {
    private static long lastTick = Long.MIN_VALUE;

    private static int previousMode;

    private static boolean captured;

    private static double x;

    private static double z;

    private static ClientLevel previousLevel;

    static Vec3 horizontal(Vec3 motion, int mode) {
        var mc = Minecraft.getInstance();

        long time = mc.level.getGameTime();

        boolean moving = Math.abs(mc.player.input.forwardImpulse) > .01
            || Math.abs(mc.player.input.leftImpulse) > .01;

        // A session gap or a change of assistance mode invalidates the old rest position.
        if (previousLevel != mc.level || time != lastTick + 1 || previousMode != mode) {
            captured = false;
        }

        previousLevel = mc.level;
        previousMode = mode;
        lastTick = time;

        // Steering releases the horizontal anchor immediately; it must not fight the swimmer.
        if (moving) {
            captured = false;
            return motion;
        }

        if (!captured || Math.abs(mc.player.getX() - x) + Math.abs(mc.player.getZ() - z) > 8) {
            x = mc.player.getX();
            z = mc.player.getZ();
            captured = true;
        }

        return new Vec3(
            BuoyancyMotion.horizontal(mc.player.getX(), x + .04 * Math.sin(time * Math.PI * 2 / 360)),
            motion.y,
            BuoyancyMotion.horizontal(mc.player.getZ(), z + .03 * Math.cos(time * Math.PI * 2 / 440))
        );
    }

    private BuoyancyClient() {}
}
