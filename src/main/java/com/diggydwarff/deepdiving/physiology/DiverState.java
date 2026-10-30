package com.diggydwarff.deepdiving.physiology;

import com.diggydwarff.deepdiving.config.PressureProfile;
import com.diggydwarff.deepdiving.gas.GasMix;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Persistent exposure history and live readouts for one diver; movement holds are transient. */
public final class DiverState {
    public final TissueModel tissues = new TissueModel();

    public final AscentMonitor ascentMonitor = new AscentMonitor();

    public double oxygenation = 100;

    public double toxicity;

    public double decompressionStress;

    public double depth;

    public double pressure = 1;

    public double ascentRate;

    public double previousY;

    public double gas;

    public double gasCapacity;

    public double narcosis;

    public double gasSeconds = -1;

    public GasMix mixture = GasMix.AIR;

    public boolean initialized;

    public boolean supplied;

    public boolean underwater;

    public boolean emergency;

    public int warnings;

    public int noDecoSeconds = 3600;

    public double ceiling;

    public int stopSeconds;

    // Buoyancy assistance is transient: never resume a hold after login or teleport.
    public boolean depthHold;

    public boolean finsFloating;

    public boolean finSwimming;

    public boolean finPoseControlled;

    public boolean finVertical;

    public boolean finMoving;

    public double finTargetY;

    public double finDepth;

    public ResourceKey<Level> finDimension;

    public Vec3 finPreviousPosition;

    public double heldDepth;

    public double holdTargetY;

    public long lastHoldRequest = -20;

    public ResourceKey<Level> holdDimension;

    public PressureProfile holdProfile;

    public Vec3 holdPreviousPosition;

    public double pressureInjury;

    public double pressureShock;

    public double previousPressure = 1;

    public boolean blackedOut;

    public ResourceKey<Level> previousDimension;

    public PressureProfile previousProfile;

    /** Save exposure history only; equipment, coordinates and active holds are sampled again. */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putDouble("oxygenation", oxygenation);
        tag.putDouble("toxicity", toxicity);
        tag.putDouble("stress", decompressionStress);
        tag.putDouble("pressureInjury", pressureInjury);
        tag.putBoolean("blackedOut", blackedOut);

        for (int i = 0; i < 4; i++) {
            tag.putDouble("n" + i, tissues.nitrogen[i]);
            tag.putDouble("h" + i, tissues.helium[i]);
        }

        return tag;
    }

    public static DiverState load(CompoundTag tag) {
        DiverState state = new DiverState();

        if (tag.contains("oxygenation")) {
            state.oxygenation = finite(tag.getDouble("oxygenation"), 100, 0, 100);
        }

        state.toxicity = finite(tag.getDouble("toxicity"), 0, 0, 300);
        state.decompressionStress = finite(tag.getDouble("stress"), 0, 0, 300);
        state.pressureInjury = finite(tag.getDouble("pressureInjury"), 0, 0, 100);
        state.blackedOut = tag.getBoolean("blackedOut");

        for (int i = 0; i < 4; i++) {
            if (tag.contains("n" + i)) {
                state.tissues.nitrogen[i] = finite(tag.getDouble("n" + i), TissueModel.SURFACE_NITROGEN, 0, 1000);
            }

            state.tissues.helium[i] = finite(tag.getDouble("h" + i), 0, 0, 1000);
        }

        return state;
    }

    private static double finite(
        double value,
        double fallback,
        double min,
        double max
    ) {
        return Double.isFinite(value) ? Math.clamp(value, min, max) : fallback;
    }
}
