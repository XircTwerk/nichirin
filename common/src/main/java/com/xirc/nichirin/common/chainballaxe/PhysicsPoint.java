package com.xirc.nichirin.common.chainballaxe;

import net.minecraft.world.phys.Vec3;

/**
 * A single Verlet particle: current + previous position (velocity is implicit) and an inverse mass
 * (0 = pinned/immovable). Shared by {@link ChainNode} and {@link WeaponEnd} so the simulation can solve
 * the whole rope — axe, chain nodes, flail — as one uniform list of points.
 */
public abstract class PhysicsPoint {

    public Vec3 position;
    public Vec3 previousPosition;
    /** Position at the end of the previous tick — used to interpolate rendering between physics ticks. */
    public Vec3 lastTickPosition;
    public double inverseMass;
    /** Per-point tuning (heavier ends override these). */
    public double damping = ChainBallAxePhysicsConfig.CHAIN_DAMPING;
    public double maxSpeed = ChainBallAxePhysicsConfig.MAX_POINT_SPEED;

    protected PhysicsPoint(Vec3 position, double inverseMass) {
        this.position = position;
        this.previousPosition = position;
        this.lastTickPosition = position;
        this.inverseMass = inverseMass;
    }

    /** Position smoothed for rendering between ticks. */
    public Vec3 renderPosition(float partialTick) {
        return lastTickPosition.lerp(position, partialTick);
    }

    /** Verlet step: x' = x + clamp((x - xPrev)*damping) + gravity. Pinned points (invMass 0) hold still. */
    public void integrate(Vec3 gravity) {
        if (inverseMass == 0.0) {
            previousPosition = position;
            return;
        }
        Vec3 velocity = position.subtract(previousPosition).scale(damping);
        double sp = velocity.length();
        if (sp > maxSpeed && sp > 1.0e-9) {
            velocity = velocity.scale(maxSpeed / sp); // clamp runaway before it compounds
        }
        Vec3 next = position.add(velocity).add(gravity);
        previousPosition = position;
        position = next;
    }

    /** Per-tick displacement (blocks/tick). */
    public Vec3 velocity() {
        return position.subtract(previousPosition);
    }

    public double speed() {
        return velocity().length();
    }

    /** Injects velocity by shifting the previous position — used for release impulses and recall. */
    public void addImpulse(Vec3 delta) {
        previousPosition = previousPosition.subtract(delta);
    }
}
