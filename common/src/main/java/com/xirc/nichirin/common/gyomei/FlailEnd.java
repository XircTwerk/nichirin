package com.xirc.nichirin.common.gyomei;

import net.minecraft.world.phys.Vec3;

/** The heavy spiked flail end — treated as a heavy sphere that lags, orbits, and pulls the chain taut. */
public class FlailEnd extends WeaponEnd {

    public final double radius = GyomeiPhysicsConfig.FLAIL_RADIUS;

    public FlailEnd(Vec3 position) {
        super(position, GyomeiPhysicsConfig.FLAIL_INVERSE_MASS, EndType.FLAIL);
        this.damping = GyomeiPhysicsConfig.FLAIL_DAMPING; // heavy: sheds momentum and settles
    }
}
