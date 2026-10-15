package com.diggydwarff.deepdiving.sediment;

import net.minecraft.world.phys.Vec3;

/** A temporary visual volume. It never places blocks or persists to disk. */
public final class SedimentCloud {

    private final Vec3 center;

    private final double maximumRadius;

    private final double lifetime;

    private final double initialDensity;

    private double age;

    public SedimentCloud(Vec3 center, double radius, double lifetime, double density) {

        this.center = center;

        this.maximumRadius = radius;

        this.lifetime = lifetime;

        this.initialDensity = density;
    }

    public void tick() {
        age += 0.05;
    }

    public Vec3 center() {
        return center;
    }

    public double radius() {

        double expansion = Math.min(1.0, age / (lifetime * 0.35));

        return maximumRadius * (0.35 + 0.65 * expansion);
    }

    public double density() {
        return initialDensity * SedimentMath.cloudEnvelope(age, lifetime);
    }

    public double densityAt(Vec3 position) {

        double distanceSquared = center.distanceToSqr(position);

        return density() * SedimentMath.radialDensity(distanceSquared, radius());
    }

    public boolean expired() {
        return age >= lifetime;
    }
}
