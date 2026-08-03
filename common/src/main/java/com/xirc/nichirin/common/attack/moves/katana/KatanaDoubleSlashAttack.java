package com.xirc.nichirin.common.attack.moves.katana;

import net.minecraft.world.entity.LivingEntity;

/** Two timed diagonal hits aligned to the authored X-slash animation. */
public final class KatanaDoubleSlashAttack extends KatanaAttackBase {
    private boolean firstHit;
    private boolean secondHit;

    @Override protected void onStart() {
        firstHit = false;
        secondHit = false;
        playSwing(1.0f);
    }

    @Override protected void perform() {
        if (world.isClientSide) return;
        if (!firstHit && tickCount >= 2) {
            for (LivingEntity target : getTargetsInSweep(90.0f, range, 5)) hitTarget(target);
            firstHit = true;
        }
        if (!secondHit && tickCount >= 5) {
            playSwing(1.1f);
            for (LivingEntity target : getTargetsInSweep(90.0f, range, 5)) hitTargetNoImmunity(target);
            secondHit = true;
        }
    }

    @Override protected void onStop() { firstHit = secondHit = false; }
}
