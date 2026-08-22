package com.xirc.nichirin.common.chainballaxe;

import net.minecraft.world.phys.Vec3;

/** The heavy Nichirin axe end. Carries orientation so the blade can point believably. */
public class AxeEnd extends WeaponEnd {

    public final double radius = ChainBallAxePhysicsConfig.AXE_RADIUS;

    public AxeEnd(Vec3 position) {
        super(position, ChainBallAxePhysicsConfig.AXE_INVERSE_MASS, EndType.AXE);
    }
}
