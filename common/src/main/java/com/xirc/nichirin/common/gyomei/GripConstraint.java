package com.xirc.nichirin.common.gyomei;

import net.minecraft.world.phys.Vec3;

/**
 * Pins one simulation point toward the player's hand. {@code pointIndex} indexes the sim's unified point
 * list: 0 = axe, 1..N = chain nodes, N+1 = flail. Points on BOTH sides of the grip stay physical, which
 * is what lets the player hold the middle of the weapon while the axe swings one way and the flail the
 * other.
 */
public class GripConstraint {

    public int pointIndex;
    public Vec3 handPosition;

    public GripConstraint(int pointIndex, Vec3 handPosition) {
        this.pointIndex = pointIndex;
        this.handPosition = handPosition;
    }
}
