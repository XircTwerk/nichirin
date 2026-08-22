package com.xirc.nichirin.common.chainballaxe;

/**
 * Tunable constants for {@link ChainBallAxeWeaponSimulation}. Nothing here is final-balance — these are the
 * spec's suggested starting values and are meant to be tuned against gameplay feel.
 */
public final class ChainBallAxePhysicsConfig {

    /** Physics chain nodes BETWEEN the two ends (axe and flail are separate points). */
    public static final int CHAIN_NODES = 20;
    /** Total simulated length from the axe socket to the flail socket, in blocks. */
    public static final double TOTAL_CHAIN_LENGTH = 8.0;

    /** Constraint-solve stiffness knobs. SUBSTEPS multiplies constraint passes for a stiffer chain. */
    public static final int SUBSTEPS = 3;
    public static final int CONSTRAINT_ITERATIONS = 12;

    /** Velocity retained per tick (Verlet damping). Lower = bleeds momentum faster / feels heavier. */
    public static final double CHAIN_DAMPING = 0.97;
    /** The flail is heavy — it bleeds momentum faster so it settles instead of orbiting forever. */
    public static final double FLAIL_DAMPING = 0.92;
    /** Fraction of a point's velocity killed on the tick it touches terrain — stops the ball skating
     *  around and makes it land with weight instead of sliding off uncontrollably. */
    public static final double GROUND_FRICTION = 0.5;
    /** Weak straightening (0 = fully floppy, 1 = rigid). Low so the chain drapes/lies flat instead of holding a stiff arc. */
    public static final double BEND_STIFFNESS = 0.03;
    /** Gravity as per-tick Verlet displacement (item gravity is ~0.04). Higher = it has real weight, sags, and
     *  lies flat on the ground instead of floating there weightlessly. */
    public static final double GRAVITY = 0.13;
    /** Hard per-tick speed cap (blocks/tick) so a whipped anchor can't fling a point uncontrollably.
     *  Kept low so the heavy ball moves deliberately instead of snapping around the screen. */
    public static final double MAX_POINT_SPEED = 1.0;

    /** Inverse masses — smaller = heavier / more momentum. The flail is by far the heaviest end. */
    public static final double CHAIN_NODE_INVERSE_MASS = 1.0;
    public static final double FLAIL_INVERSE_MASS = 0.12;
    public static final double AXE_INVERSE_MASS = 0.38;

    /** Collision radii (blocks) for the two ends and the thin chain. */
    public static final double FLAIL_RADIUS = 0.42;
    public static final double AXE_RADIUS = 0.30;
    public static final double CHAIN_RADIUS = 0.08;

    private ChainBallAxePhysicsConfig() {}
}
