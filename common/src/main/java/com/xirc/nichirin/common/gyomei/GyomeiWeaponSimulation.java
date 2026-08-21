package com.xirc.nichirin.common.gyomei;

import net.minecraft.world.phys.Vec3;

/**
 * The Gyomei weapon: a Verlet / position-based-dynamics rope whose two ENDS are themselves physical
 * points — {@code AXE <-> node0 <-> ... <-> nodeN <-> FLAIL}. Pulling one end physically affects the
 * other through the shared distance constraints unless a grip pins a point in between.
 *
 * <p>This class is engine-only: it knows nothing about Minecraft entities, rendering, or netcode. It is
 * fed a hand/grip position each tick and stepped; everything else (where the flail is, whether it hit
 * something) is read back off the simulated points. The simulation IS the weapon.</p>
 */
public class GyomeiWeaponSimulation {

    public final AxeEnd axe;
    public final ChainNode[] nodes;
    public final FlailEnd flail;

    /** Unified solve order: [axe, node0..nodeN, flail]. */
    private final PhysicsPoint[] points;
    /** Per-point flag: did this point touch terrain this tick? Drives ground friction. */
    private final boolean[] contacted;
    private final double segmentRest;
    /** 1.0 = full chain; lower = reeled in (the ball rides closer to the hand). */
    public double lengthScale = 1.0;

    /** Current effective per-segment rest length (shrinks while reeling the ball in). */
    private double rest() { return segmentRest * lengthScale; }

    public GripMode gripMode = GripMode.AXE;
    private GripConstraint grip;
    private PointCollider collider;

    /** 0 (fully slack) .. 1 (fully taut). */
    public double tension;

    public GyomeiWeaponSimulation(Vec3 origin, Vec3 direction) {
        int n = GyomeiPhysicsConfig.CHAIN_NODES;
        int segments = n + 1; // axe->node0, node-to-node..., nodeN->flail
        segmentRest = GyomeiPhysicsConfig.TOTAL_CHAIN_LENGTH / segments;

        Vec3 dir = direction.lengthSqr() < 1.0e-6 ? new Vec3(0, -1, 0) : direction.normalize();

        axe = new AxeEnd(origin);
        nodes = new ChainNode[n];
        for (int i = 0; i < n; i++) {
            nodes[i] = new ChainNode(origin.add(dir.scale(segmentRest * (i + 1))));
        }
        flail = new FlailEnd(origin.add(dir.scale(segmentRest * (n + 1))));

        points = new PhysicsPoint[n + 2];
        points[0] = axe;
        for (int i = 0; i < n; i++) points[i + 1] = nodes[i];
        points[n + 1] = flail;
        contacted = new boolean[points.length];
    }

    public int pointCount() { return points.length; }
    public int nodeCount() { return nodes.length; }
    public PhysicsPoint point(int i) { return points[i]; }
    public int axeIndex() { return 0; }
    public int flailIndex() { return points.length - 1; }
    public double segmentRest() { return segmentRest; }

    /** Terrain collision resolver; null = no collision (both ends and chain clip through blocks). */
    public void setCollider(PointCollider collider) {
        this.collider = collider;
    }

    private double radiusFor(int pointIndex) {
        if (pointIndex == axeIndex()) return GyomeiPhysicsConfig.AXE_RADIUS;
        if (pointIndex == flailIndex()) return GyomeiPhysicsConfig.FLAIL_RADIUS;
        return GyomeiPhysicsConfig.CHAIN_RADIUS;
    }

    /** Pin a point to the hand. A negative index clears the grip (both ends free). */
    public void setGrip(int pointIndex, Vec3 handPosition) {
        if (pointIndex < 0) {
            grip = null;
            return;
        }
        if (grip == null) {
            grip = new GripConstraint(pointIndex, handPosition);
        } else {
            grip.pointIndex = pointIndex;
            grip.handPosition = handPosition;
        }
    }

    public void step() {
        // Snapshot for render interpolation before this tick moves anything.
        for (PhysicsPoint p : points) p.lastTickPosition = p.position;
        java.util.Arrays.fill(contacted, false);

        Vec3 gravity = new Vec3(0, -GyomeiPhysicsConfig.GRAVITY, 0);
        integrate(gravity);
        int iterations = GyomeiPhysicsConfig.CONSTRAINT_ITERATIONS * GyomeiPhysicsConfig.SUBSTEPS;
        for (int it = 0; it < iterations; it++) {
            solveGrip();
            // Bidirectional Gauss-Seidel: a forward then backward sweep propagates tension both ways
            // each pass, so a long chain with a heavy end converges to its rest length instead of
            // stretching like a rubber band.
            solveDistance(true);
            solveDistance(false);
            solveBending();
        }
        // Converge inextensibility AND collision together. Each pass: hard-length (follow-the-leader),
        // then push out of blocks. FTL runs BEFORE collision every pass, so it never undoes the push-out
        // (the previous bug: a single FTL after collision pulled points straight back into the blocks,
        // which is why collision "disappeared"). Ending on collision means the chain never clips.
        for (int it = 0; it < 8; it++) {
            solveGrip();
            followTheLeader();
            if (collider != null) resolveCollisions();
        }
        // Terrain contact bleeds velocity so the ball lands with weight and doesn't skate around.
        applyGroundFriction();
        // Final word: nothing may sit past its reach. If a stuck ball is dragged past the chain's length,
        // length wins over collision and yanks it free — the rope can never stretch to infinity.
        clampMaxStretch();
        // Grip wins on the final pass so the held end never gets shoved off the hand.
        solveGrip();
        updateTension();
    }

    /**
     * Hard cap on reach: no point may be further from the anchor than its cumulative rest length. The
     * straight-line distance is always ≤ the chain's rest length unless it is being over-stretched, so
     * clamping it to that sphere pulls an over-stretched (e.g. stuck) end back in without ever letting the
     * rope grow past its real length.
     */
    private void clampMaxStretch() {
        int anchor = grip != null ? grip.pointIndex : 0;
        Vec3 a = points[anchor].position;
        for (int i = anchor + 1; i < points.length; i++) clampReach(a, i, rest() * (i - anchor));
        for (int i = anchor - 1; i >= 0; i--) clampReach(a, i, rest() * (anchor - i));
    }

    private void clampReach(Vec3 anchorPos, int index, double maxDist) {
        Vec3 d = points[index].position.subtract(anchorPos);
        double dist = d.length();
        if (dist > maxDist && dist > 1.0e-6) {
            points[index].position = anchorPos.add(d.scale(maxDist / dist));
        }
    }

    private void resolveCollisions() {
        for (int i = 0; i < points.length; i++) {
            if (grip != null && grip.pointIndex == i) continue; // the hand-held point isn't pushed
            // Sweep from where the point began this tick to where the constraints put it, so a fast point
            // can't tunnel through the floor (lastTickPosition is the pre-integration snapshot).
            Vec3 before = points[i].position;
            Vec3 after = collider.resolve(points[i].lastTickPosition, before, radiusFor(i));
            points[i].position = after;
            if (after.distanceToSqr(before) > 1.0e-8) contacted[i] = true;
        }
    }

    /**
     * For every point that touched terrain this tick, pull its previousPosition toward its current
     * position — Verlet velocity is {@code position - previousPosition}, so this bleeds a fraction of the
     * velocity, killing the frictionless "skating" that made the ball wander around on the ground.
     */
    private void applyGroundFriction() {
        double f = GyomeiPhysicsConfig.GROUND_FRICTION;
        if (f <= 0.0) return;
        for (int i = 0; i < points.length; i++) {
            if (!contacted[i]) continue;
            if (grip != null && grip.pointIndex == i) continue;
            points[i].previousPosition = points[i].previousPosition.lerp(points[i].position, f);
        }
    }

    private void integrate(Vec3 gravity) {
        for (PhysicsPoint p : points) {
            p.integrate(gravity);
        }
    }

    /** Effective inverse mass: a gripped point is immovable within the constraint solve. */
    private double effectiveInvMass(PhysicsPoint p) {
        if (grip != null && points[grip.pointIndex] == p) return 0.0;
        return p.inverseMass;
    }

    private void solveGrip() {
        if (grip == null) return;
        points[grip.pointIndex].position = grip.handPosition;
    }

    private void solveDistance(boolean forward) {
        if (forward) {
            for (int i = 0; i < points.length - 1; i++) solveSegment(i, i + 1);
        } else {
            for (int i = points.length - 2; i >= 0; i--) solveSegment(i, i + 1);
        }
    }

    private void solveSegment(int ia, int ib) {
        PhysicsPoint a = points[ia];
        PhysicsPoint b = points[ib];
        Vec3 delta = b.position.subtract(a.position);
        double d = delta.length();
        if (d < 1.0e-6) return;
        double wa = effectiveInvMass(a);
        double wb = effectiveInvMass(b);
        double wsum = wa + wb;
        if (wsum <= 0.0) return;
        double diff = (d - rest()) / d;
        Vec3 correction = delta.scale(diff);
        a.position = a.position.add(correction.scale(wa / wsum));
        b.position = b.position.subtract(correction.scale(wb / wsum));
    }

    /** Walk out from the anchor placing each point at EXACTLY rest length from the previous one. */
    private void followTheLeader() {
        int anchor = grip != null ? grip.pointIndex : 0;
        for (int i = anchor; i < points.length - 1; i++) enforceLength(i, i + 1);
        for (int i = anchor; i > 0; i--) enforceLength(i, i - 1);
    }

    private void enforceLength(int fixedIndex, int moveIndex) {
        Vec3 d = points[moveIndex].position.subtract(points[fixedIndex].position);
        double len = d.length();
        if (len > 1.0e-6) {
            points[moveIndex].position = points[fixedIndex].position.add(d.scale(rest() / len));
        }
    }

    private void solveBending() {
        double k = GyomeiPhysicsConfig.BEND_STIFFNESS;
        if (k <= 0.0) return;
        for (int i = 1; i < points.length - 1; i++) {
            PhysicsPoint b = points[i];
            if (effectiveInvMass(b) <= 0.0) continue;
            Vec3 mid = points[i - 1].position.add(points[i + 1].position).scale(0.5);
            b.position = b.position.add(mid.subtract(b.position).scale(k));
        }
    }

    /**
     * Tension is the strongest signal for attack power/sound/feedback: how close the whole weapon is to
     * being pulled straight, plus any per-segment overstretch when a heavy end yanks the chain.
     */
    private void updateTension() {
        double maxStretch = 0.0;
        for (int i = 0; i < points.length - 1; i++) {
            double d = points[i + 1].position.distanceTo(points[i].position);
            maxStretch = Math.max(maxStretch, (d - rest()) / rest());
        }
        double span = axe.position.distanceTo(flail.position) / GyomeiPhysicsConfig.TOTAL_CHAIN_LENGTH;
        tension = Math.max(0.0, Math.min(1.0, Math.max(maxStretch * 8.0, span)));
    }
}
