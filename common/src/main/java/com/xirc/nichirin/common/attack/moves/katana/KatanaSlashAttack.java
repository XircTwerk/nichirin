package com.xirc.nichirin.common.attack.moves.katana;

import net.minecraft.world.entity.LivingEntity;

/** Opening cut in the neutral katana light combo. */
public final class KatanaSlashAttack extends KatanaAttackBase {
    private boolean executed;

    @Override protected void onStart() {
        executed = false;
        playSwing(1.0f);
    }

    @Override protected void perform() {
        if (world.isClientSide || executed) return;
        for (LivingEntity target : getTargetsInSweep(85.0f, range, 5)) hitTarget(target);
        executed = true;
    }

    @Override protected void onStop() { executed = false; }
}
