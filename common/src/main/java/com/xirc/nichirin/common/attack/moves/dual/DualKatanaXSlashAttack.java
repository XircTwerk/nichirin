package com.xirc.nichirin.common.attack.moves.dual;

import net.minecraft.world.entity.LivingEntity;

/** Two crossing cuts synchronized to the X-slash animation. */
public final class DualKatanaXSlashAttack extends DualKatanaAttackBase {
    private boolean firstCut;
    private boolean secondCut;

    @Override
    protected void onStart() {
        firstCut = false;
        secondCut = false;
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;
        int activeTick = tickCount - windup;
        if (!firstCut && activeTick >= 1) {
            strike(false, 0.95f);
            firstCut = true;
        }
        if (!secondCut && activeTick >= 3) {
            strike(true, 1.1f);
            secondCut = true;
        }
    }

    private void strike(boolean applyConfiguredKnockback, float pitch) {
        float savedKnockback = knockback;
        if (!applyConfiguredKnockback) knockback = 0.0f;
        for (LivingEntity target : getTargetsInSweep(100.0f, range, 7)) hitTargetNoImmunity(target);
        knockback = savedKnockback;
        playSwingSound(pitch);
    }

    @Override protected void onStop() { firstCut = secondCut = false; }
}
