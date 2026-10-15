package com.diggydwarff.deepdiving.sediment;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/** Actual displacement avoids gravity-driven puffs from a motionless player on the bottom. */
public final class SedimentMovement {
    public record Motion(Vec3 travel, boolean landed) {}

    private record Sample(Vec3 position, int tick, ResourceKey<Level> dimension, boolean grounded, Vec3 travel) {}

    private final Map<Player, Sample> previous = new WeakHashMap<>();

    public Motion sample(Player player) {
        Sample last = previous.get(player);

        Vec3 travel = Vec3.ZERO;

        boolean grounded = player.onGround() || player.verticalCollisionBelow;

        boolean landed = false;

        if (last != null && last.dimension().equals(player.level().dimension())) {
            int elapsed = player.tickCount - last.tick();

            if (elapsed > 0 && elapsed <= 20) {
                travel = player.position().subtract(last.position()).scale(1.0 / elapsed);

                if (travel.lengthSqr() > 1) {
                    travel = Vec3.ZERO;
                } else {
                    landed = grounded && !last.grounded();
                }
            }
        }

        previous.put(player, new Sample(player.position(), player.tickCount,
            player.level().dimension(), grounded, travel));

        // Collision may already have stopped the downward travel on the exact landing tick.
        // Retain only the preceding measured descent for that one transition.
        if (landed && last != null && last.travel().y < travel.y) {
            travel = new Vec3(travel.x, last.travel().y, travel.z);
        }

        return new Motion(travel, landed);
    }

    public void clear() {
        previous.clear();
    }
}
