package com.xirc.nichirin.common.attack.moves.dual;

import net.minecraft.world.entity.LivingEntity;

/** Four-beat close-range combo synchronized to the authored animation contacts. */
public final class DualKatanaComboAttack extends DualKatanaAttackBase {
    private static final int[] HIT_TICKS = {5, 10, 15, 23, 25};

    @Override protected void onStart() {}

    @Override
    protected void perform() {
        if (world.isClientSide || !isHitTick(tickCount)) return;

        boolean finisher = tickCount == HIT_TICKS[HIT_TICKS.length - 1];
        float configuredDamage = damage;
        if (finisher) damage = configuredDamage * 2.0f;
        try {
            for (LivingEntity target : getTargetsAtRange()) hitTargetNoImmunity(target);
        } finally {
            damage = configuredDamage;
        }
        playSwingSound(finisher ? 0.78f : 1.0f + tickCount * 0.006f);
    }

    private static boolean isHitTick(int tick) {
        for (int hitTick : HIT_TICKS) if (tick == hitTick) return true;
        return false;
    }

    @Override protected void onStop() {}
}
