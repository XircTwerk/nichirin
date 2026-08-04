package com.xirc.nichirin.common.attack.moves.dual;

import net.minecraft.world.entity.LivingEntity;

/** Opening right-hand cut in the alternating light combo. */
public final class DualKatanaM1Attack extends DualKatanaAttackBase {
    private boolean executed;

    @Override
    protected void onStart() {
        executed = false;
        playSwingSound(1.05f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide || executed) return;
        for (LivingEntity target : getTargetsInSweep(85.0f, range, 5)) hitTarget(target);
        executed = true;
    }

    @Override protected void onStop() { executed = false; }
}
