package com.xirc.nichirin.common.chainballaxe;

import net.minecraft.world.phys.Vec3;

/**
 * Pushes a simulation point out of the world. Supplied to {@link ChainBallAxeWeaponSimulation} from outside so
 * the sim engine itself stays free of any Minecraft/world dependency.
 */
@FunctionalInterface
public interface PointCollider {
    /**
     * Sweeps a sphere of {@code radius} from {@code from} to {@code to} and returns a corrected position
     * clear of blocks. Sweeping (rather than only testing the destination) stops a fast point from
     * tunnelling straight through thin terrain in a single tick.
     */
    Vec3 resolve(Vec3 from, Vec3 to, double radius);
}
