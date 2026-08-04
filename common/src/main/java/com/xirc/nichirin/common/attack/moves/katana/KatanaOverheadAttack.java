package com.xirc.nichirin.common.attack.moves.katana;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Heavy overhead cut that drives airborne targets down. */
public final class KatanaOverheadAttack extends KatanaAttackBase {
    private boolean executed;

    @Override protected void onStart() { executed = false; playSwing(0.72f); }

    @Override protected void perform() {
        if (world.isClientSide || executed) return;
        for (LivingEntity target : getTargetsInSweep(72.0f, range, 5)) {
            hitTarget(target);
            if (!target.onGround()) {
                Vec3 motion = target.getDeltaMovement();
                target.setDeltaMovement(motion.x * 0.3, motion.y - 0.5, motion.z * 0.3);
                target.hurtMarked = true;
            }
        }
        executed = true;
    }

    @Override protected void onStop() { executed = false; }
}
