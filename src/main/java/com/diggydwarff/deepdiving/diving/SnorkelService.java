package com.diggydwarff.deepdiving.diving;

import com.diggydwarff.deepdiving.compat.DeepSeasCompatibility;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;

/** Surface air only: the snorkel never supplies compressed gas at depth. */
public final class SnorkelService {
    public static final double SURFACE_REACH = 2.0;

    public static boolean canBreathe(Player player) {
        if (!player.isAlive()
                || player.isSpectator()
                || !(player.getItemBySlot(EquipmentSlot.HEAD).getItem()
                        instanceof com.diggydwarff.deepdiving.equipment.SnorkelMaskItem)
                || !player.isEyeInFluid(FluidTags.WATER)
                || DeepSeasCompatibility.inDryCompartment(player)) {
            return false;
        }

        var level = player.level();

        Vec3 eyes = player.getEyePosition();

        int bottom = Mth.floor(eyes.y);

        int top = Mth.floor(eyes.y + SURFACE_REACH);

        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

        // At most three water cells are inspected. Never scan toward sea level
        // or load a distant chunk just to find somewhere the tube could breathe.
        for (int y = bottom; y <= top; y++) {
            position.set(Mth.floor(eyes.x), y, Mth.floor(eyes.z));

            if (level.isOutsideBuildHeight(position) || !level.hasChunkAt(position)) {
                return false;
            }

            var water = level.getFluidState(position);

            if (!water.is(FluidTags.WATER)) {
                return false;
            }

            double surfaceY = y + water.getHeight(level, position);

            if (surfaceY < eyes.y || surfaceY - eyes.y >= SURFACE_REACH) {
                continue;
            }

            Vec3 air = new Vec3(eyes.x, surfaceY + .02, eyes.z);

            BlockPos airPosition = BlockPos.containing(air);

            if (level.isOutsideBuildHeight(airPosition) || !level.hasChunkAt(airPosition)) {
                return false;
            }

            var fluidAbove = level.getFluidState(airPosition);

            // Partially filled water has air inside the same block. Check the
            // actual fluid height instead of treating that whole block as wet.
            if (!fluidAbove.isEmpty()
                    && air.y < airPosition.getY() + fluidAbove.getHeight(level, airPosition)) {
                continue;
            }

            // Waterlogged slabs, closed trapdoors and glass ceilings still
            // obstruct the tube. Plants and other non-colliding blocks do not.
            var obstruction =
                    level.clip(
                            new ClipContext(
                                    eyes,
                                    air,
                                    ClipContext.Block.COLLIDER,
                                    ClipContext.Fluid.NONE,
                                    player));

            return obstruction.getType() == HitResult.Type.MISS;
        }

        return false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void breathe(LivingBreatheEvent event) {
        if (event.getEntity() instanceof Player player && canBreathe(player)) {
            event.setCanBreathe(true);
            event.setRefillAirAmount(Math.max(event.getRefillAirAmount(), 4));
        }
    }

    private SnorkelService() {}
}
