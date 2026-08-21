package com.xirc.nichirin.common.gyomei;

import net.minecraft.world.phys.Vec3;

/** A plain chain segment particle. */
public class ChainNode extends PhysicsPoint {

    public ChainNode(Vec3 position) {
        super(position, GyomeiPhysicsConfig.CHAIN_NODE_INVERSE_MASS);
    }
}
