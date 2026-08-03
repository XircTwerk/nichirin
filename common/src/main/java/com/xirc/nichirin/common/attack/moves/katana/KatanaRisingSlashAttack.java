package com.xirc.nichirin.common.attack.moves.katana;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Rising cut that launches struck targets. */
public final class KatanaRisingSlashAttack extends KatanaAttackBase {
    private boolean executed;

    @Override protected void onStart() { executed = false; playSwing(0.8f); }

    @Override protected void perform() {
        if (world.isClientSide || executed) return;
        for (LivingEntity target : getTargetsInSweep(75.0f, range, 5)) {
            hitTarget(target);
            Vec3 lateral = target.position().subtract(user.position()).normalize().scale(knockback);
            target.setDeltaMovement(lateral.x, 0.8, lateral.z);
            target.hurtMarked = true;
        }
        executed = true;
    }

    @Override protected void onStop() { executed = false; }
}
