package com.xirc.nichirin.common.chainballaxe;

import net.minecraft.world.phys.Vec3;

/**
 * Drives the control-point motion for the Chain-Ball-Axe attacks. Attacks move the CONTROL POINT only —
 * the chain physics flings the flail/axe and the resulting end velocity does the damage. Run identically
 * on the server (authoritative, for damage) and on the client (local sim, for the matching visual), so
 * what you see is what hits.
 *
 * <p>Two grip stances: AXE-mode attacks hold the axe end (index 0); FLAIL-mode attacks (after sheathing
 * to grip the middle of the chain) drive the middle node so BOTH ends — spiked ball and axe — scythe.</p>
 */
public final class ChainBallAxeAttackController {

    public enum Attack {
        NONE(0, false),
        // Axe mode (hold the axe):
        AXE_HACK(12, false),      // M1: overhead chop with the axe
        LOB(9, false),            // M2: lob the heavy ball toward where you aim
        // Flail mode (hold the middle of the chain):
        REAPING_SWEEP(12, true),  // M1: wide horizontal ball swing across the front
        METEOR_DROP(16, true),    // M2: whirl overhead then slam straight down
        RISING_CRESCENT(14, true),// crouch-M1: low-to-high launcher
        TWIN_CYCLONE(30, true);   // crouch-M2: spin the whole chain around you

        public final int duration;
        /** True = grip the MIDDLE of the chain (flail stance); false = grip the axe end. */
        public final boolean flailStance;
        Attack(int d, boolean flailStance) { this.duration = d; this.flailStance = flailStance; }
    }

    private Attack attack = Attack.NONE;
    private int tick;

    public boolean isAttacking() { return attack != Attack.NONE; }
    public Attack current() { return attack; }

    /** Start an attack. Ignored if one is already running. */
    public void trigger(Attack next) {
        if (attack != Attack.NONE || next == null || next == Attack.NONE) return;
        attack = next;
        tick = 0;
    }

    /**
     * If an attack is active, set the sim's grip along its curve for this tick and return true (so the
     * caller skips the normal hold grip). Returns false when idle.
     */
    public boolean driveGrip(ChainBallAxeWeaponSimulation sim, Vec3 hand, Vec3 look) {
        if (attack == Attack.NONE) return false;
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 fwd = new Vec3(look.x, 0.0, look.z);
        fwd = fwd.lengthSqr() < 1.0e-6 ? new Vec3(0, 0, 1) : fwd.normalize();
        Vec3 right = new Vec3(-fwd.z, 0.0, fwd.x);
        double s = attack.duration <= 0 ? 1.0 : tick / (double) attack.duration;
        int grip = attack.flailStance ? sim.pointCount() / 2 : sim.axeIndex();

        switch (attack) {
            case AXE_HACK -> {
                Vec3 target;
                if (s < 0.35) {
                    double w = s / 0.35;                       // raise up and back
                    target = hand.add(up.scale(1.0 * w)).add(look.scale(-0.3 * w));
                } else {
                    double d = (s - 0.35) / 0.65;              // chop down and forward
                    target = hand.add(up.scale(1.0 * (1.0 - d) - 0.6 * d)).add(look.scale(1.5 * d));
                }
                sim.setGrip(grip, target);
            }
            case LOB -> {
                // Lob the heavy ball toward where you aim, arcing up-and-out; the hand keeps the axe so the
                // chain tethers it — it flies at the enemy then swings back.
                if (tick == 0) {
                    Vec3 lob = look.scale(0.9).add(0.0, 0.5, 0.0);
                    sim.flail.previousPosition = sim.flail.position.subtract(lob);
                }
                sim.setGrip(grip, hand);
            }
            case REAPING_SWEEP -> {
                // Wide horizontal swing across the front; alternate side handled by the caller's cadence.
                double a = -1.0 + 2.0 * s;                     // sweep left → right
                sim.setGrip(grip, hand.add(right.scale(a * 1.4)).add(fwd.scale(0.5)).add(up.scale(0.1)));
            }
            case METEOR_DROP -> {
                Vec3 target;
                if (s < 0.4) {
                    double w = s / 0.4;                        // whirl up
                    target = hand.add(up.scale(1.3 * w)).add(fwd.scale(0.3 * w));
                } else {
                    double d = (s - 0.4) / 0.6;                // slam down and forward
                    target = hand.add(up.scale(1.3 * (1.0 - d) - 0.9 * d)).add(fwd.scale(0.3 + 1.3 * d));
                }
                sim.setGrip(grip, target);
            }
            case RISING_CRESCENT -> {
                // Low-behind up to high-front — an upward launcher.
                double y = -0.7 + 2.0 * s;
                sim.setGrip(grip, hand.add(up.scale(y)).add(fwd.scale(0.7 * Math.sin(s * Math.PI))));
            }
            case TWIN_CYCLONE -> {
                double angle = s * Math.PI * 4.0;              // two full revolutions
                double radius = 1.3;
                sim.setGrip(grip, hand
                        .add(right.scale(Math.cos(angle) * radius))
                        .add(fwd.scale(Math.sin(angle) * radius))
                        .add(up.scale(0.2)));
            }
            default -> { }
        }

        tick++;
        if (tick >= attack.duration) attack = Attack.NONE;
        return true;
    }
}
