package com.xirc.nichirin.common.attack.moves.katana;

import net.minecraft.world.entity.LivingEntity;

/** Reverse cut used for the second light-combo input. */
public final class KatanaSlashFollowupAttack extends KatanaAttackBase {
    private boolean executed;

    @Override protected void onStart() {
        executed = false;
        playSwing(1.08f);
    }

    @Override protected void perform() {
        if (world.isClientSide || executed) return;
        for (LivingEntity target : getTargetsInSweep(90.0f, range, 5)) hitTarget(target);
        executed = true;
    }

    @Override protected void onStop() { executed = false; }
}
