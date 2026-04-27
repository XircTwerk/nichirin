package com.xirc.nichirin.common.attack.moves.breathing.beast;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
    private static final int KNOCKBACK_DELAY = 5;

    @Override
    protected void onStart() {
        slashExecuted = false;
        knockbackApplied = false;
        storedTarget = null;
        playSlashSound();
        createChargeEffect();
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
            storedTarget.push(dir.x * 2.0, 0.3, dir.z * 2.0);
            knockbackApplied = true;
            if (world instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, storedTarget.getX(), storedTarget.getY() + 1, storedTarget.getZ(),
                        15, 0.4, 0.4, 0.4, 0.2);
            }
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

        createThrustEffect(pos, look);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    private void createChargeEffect() {
        if (!(world instanceof ServerLevel sl)) return;
        Vec3 pos = user.position().add(0, user.getBbHeight() / 2, 0);
        sl.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 8, 0.3, 0.3, 0.3, 0.1);
    }

    private void createThrustEffect(Vec3 origin, Vec3 direction) {
        if (!(world instanceof ServerLevel sl)) return;
        for (int i = 1; i <= 6; i++) {
            Vec3 p = origin.add(direction.scale(i * 0.5));
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
            sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.05);
        }
    }

    @Override
    protected void onStop() {
        slashExecuted = false;
        knockbackApplied = false;
        storedTarget = null;
    }
}
