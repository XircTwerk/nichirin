package com.xirc.nichirin.common.attack.moves.katana;

import com.xirc.nichirin.common.vfx.VfxIds;
import net.minecraft.world.entity.LivingEntity;

/** Tight pommel check with strong knockback and stun. */
public final class KatanaCheckAttack extends KatanaAttackBase {
    private boolean executed;

    @Override protected void onStart() {
        executed = false;
        playSwing(0.8f);
        playVfx(VfxIds.KATANA_CHECK, 0.42f, 0.48f, 0.85f);
    }

    @Override protected void perform() {
        if (world.isClientSide || executed) return;
        for (LivingEntity target : getTargetsInSweep(42.0f, range, 3)) hitTarget(target);
        executed = true;
    }

    @Override protected void onStop() { executed = false; }
}
