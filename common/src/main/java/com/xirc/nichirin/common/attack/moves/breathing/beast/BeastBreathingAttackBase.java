package com.xirc.nichirin.common.attack.moves.breathing.beast;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("rawtypes")
public abstract class BeastBreathingAttackBase extends AbstractBreathingAttack<BeastBreathingAttackBase, IBreathingAttacker> {

    protected void createSlashParticles(Vec3 center, Vec3 direction) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        Vec3 perp = new Vec3(-direction.z, 0, direction.x).normalize();
        for (int i = -4; i <= 4; i++) {
            Vec3 p = center.add(perp.scale(i * 0.4));
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0);
            serverLevel.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 2, 0.15, 0.15, 0.15, 0.05);
        }
    }

    protected void createWildParticles(Vec3 center, double radius) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * 2 * Math.PI;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, x, center.y, z, 1, 0.05, 0.1, 0.05, 0);
            serverLevel.sendParticles(ParticleTypes.CRIT, x, center.y + 0.5, z, 1, 0.1, 0.1, 0.1, 0.05);
        }
    }

    protected void createImpactParticles(Vec3 pos) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        serverLevel.sendParticles(ParticleTypes.CRIT, pos.x, pos.y + 1, pos.z, 8, 0.3, 0.3, 0.3, 0.15);
        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + 1, pos.z, 3, 0.2, 0.2, 0.2, 0);
    }

    protected void playSlashSound() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
        }
    }

    protected void playHeavySlashSound() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.9f);
        }
    }

    @Override
    protected void hitTarget(LivingEntity target) {
        super.hitTarget(target);
        createImpactParticles(target.position());
        playHeavySlashSound();
    }

    @Override
    protected abstract void onStart();

    @Override
    protected abstract void perform();
}