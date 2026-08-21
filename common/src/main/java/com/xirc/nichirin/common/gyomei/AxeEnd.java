package com.xirc.nichirin.common.gyomei;

import net.minecraft.world.phys.Vec3;

/** The heavy Nichirin axe end. Carries orientation so the blade can point believably. */
public class AxeEnd extends WeaponEnd {

    public final double radius = GyomeiPhysicsConfig.AXE_RADIUS;

    public AxeEnd(Vec3 position) {
        super(position, GyomeiPhysicsConfig.AXE_INVERSE_MASS, EndType.AXE);
    }
}
