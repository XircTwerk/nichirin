package com.xirc.nichirin.common.attack.moves.breathing.beast;

import com.xirc.nichirin.common.vfx.VfxIds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// First Fang: Pierce. Forward thrust with knockback intentionally delayed by a few ticks.
public class BeastPierceAttack extends BeastBreathingAttackBase {

    private boolean slashExecuted = false;
    private boolean knockbackApplied = false;
    private LivingEntity storedTarget = null;
    private static final int KNOCKBACK_DELAY = 2;

    @Override
    protected void onStart() {
        slashExecuted = false;
        knockbackApplied = false;
        storedTarget = null;
        playSlashSound();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        if (!slashExecuted) {
            executeThrust();
            slashExecuted = true;
        }

        if (!knockbackApplied && storedTarget != null && tickCount >= KNOCKBACK_DELAY) {
            Vec3 dir = storedTarget.position().subtract(user.position()).normalize();
            storedTarget.push(dir.x * knockback, 0.1 * knockback, dir.z * knockback);
            knockbackApplied = true;
        }
    }

    private void executeThrust() {
        Vec3 pos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 look = user.getLookAngle();

        List<LivingEntity> targets = getTargetsInThrust();
        for (LivingEntity target : targets) {
            // Suppress knockback here — applied manually after KNOCKBACK_DELAY ticks
            float savedKnockback = knockback;
            knockback = 0;
            hitTarget(target);
            knockback = savedKnockback;
            if (storedTarget == null) storedTarget = target;
        }

        playBeastVfx(VfxIds.BEAST_PIERCE, pos, look, 1.0f);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    @Override
    protected void onStop() {
        slashExecuted = false;
        knockbackApplied = false;
        storedTarget = null;
    }
}
