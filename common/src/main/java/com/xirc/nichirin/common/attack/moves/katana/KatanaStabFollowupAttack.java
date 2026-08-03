package com.xirc.nichirin.common.attack.moves.katana;

import com.xirc.nichirin.common.vfx.VfxIds;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Armored, high-stun stab that closes the three-hit light combo. */
public final class KatanaStabFollowupAttack extends KatanaAttackBase {
    private boolean executed;

    @Override protected void onStart() {
        executed = false;
        playSwing(0.92f);
    }

    @Override protected void onActiveStart() {
        Vec3 look = user.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() > 1.0E-6) {
            teleportSafe(user.position().add(horizontal.normalize().scale(2.0)));
        }
        playVfx(VfxIds.KATANA_PIERCING_FINISH, 1.65f, 0.62f, 1.0f);
    }

    @Override protected void perform() {
        if (world.isClientSide || executed) return;
        for (LivingEntity target : getTargetsInRangeLine(0.55f)) hitTarget(target);
        executed = true;
    }

    @Override protected void onStop() { executed = false; }
}
