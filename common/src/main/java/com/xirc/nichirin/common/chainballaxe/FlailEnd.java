package com.xirc.nichirin.common.chainballaxe;

import net.minecraft.world.phys.Vec3;

/** The heavy spiked flail end — treated as a heavy sphere that lags, orbits, and pulls the chain taut. */
public class FlailEnd extends WeaponEnd {

    public final double radius = ChainBallAxePhysicsConfig.FLAIL_RADIUS;

    public FlailEnd(Vec3 position) {
        super(position, ChainBallAxePhysicsConfig.FLAIL_INVERSE_MASS, EndType.FLAIL);
        this.damping = ChainBallAxePhysicsConfig.FLAIL_DAMPING; // heavy: sheds momentum and settles
    }
}
