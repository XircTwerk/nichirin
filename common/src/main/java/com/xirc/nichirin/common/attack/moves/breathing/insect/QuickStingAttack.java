package com.xirc.nichirin.common.attack.moves.breathing.insect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Quick Sting. Rapid poison thrust with no immunity frames — right-click attack for Insect Breathing.
public class QuickStingAttack extends InsectBreathingAttackBase {

    private boolean stingExecuted = false;

    @Override
    protected void onStart() {
        stingExecuted = false;
    }

    @Override
    protected void onActiveStart() {
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BEE_STING, SoundSource.PLAYERS, 0.8f, 1.8f);
        createQuickStingAura();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        if (!stingExecuted && tickCount == windup + 1) {
            executeQuickSting();
            stingExecuted = true;
        }
    }

    private void createQuickStingAura() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * 2 * Math.PI;
            double x = userPos.x + Math.cos(angle) * 0.8;
            double z = userPos.z + Math.sin(angle) * 0.8;
            double y = userPos.y + Math.sin(angle * 2) * 0.3;
            serverLevel.sendParticles(ParticleTypes.WITCH, x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
        }

        for (int i = 1; i <= 3; i++) {
            Vec3 thrustPos = userPos.add(lookDir.scale(i * 0.8));
            serverLevel.sendParticles(ParticleTypes.PORTAL, thrustPos.x, thrustPos.y, thrustPos.z, 1, 0.05, 0.05, 0.05, 0.02);
        }
    }

    private void executeQuickSting() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        createThrustEffect();

        List<LivingEntity> targets = getTargetsInCustomHitbox(
                userPos.add(lookDir.scale(range / 2)),
                hitboxSize, 2.0, range);

        for (LivingEntity target : targets) {
            hitTargetNoImmunity(target);
            target.push(lookDir.x * knockback, 0.1, lookDir.z * knockback);
            createStingImpactEffect(target.position());
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.8f, 2.0f);
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 1.8f);
    }

    private void createThrustEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        for (int i = 0; i <= 8; i++) {
            double progress = i / 8.0;
            Vec3 thrustPos = userPos.add(lookDir.scale(progress * range));
            serverLevel.sendParticles(ParticleTypes.CRIT, thrustPos.x, thrustPos.y, thrustPos.z, 2, 0.1, 0.1, 0.1, 0.05);
            if (i % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.WITCH, thrustPos.x, thrustPos.y, thrustPos.z, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }

        Vec3 tipPos = userPos.add(lookDir.scale(range));
        serverLevel.sendParticles(ParticleTypes.PORTAL, tipPos.x, tipPos.y, tipPos.z, 5, 0.2, 0.2, 0.2, 0.1);
    }

    private void createStingImpactEffect(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 targetPos = impactPos.add(0, 1, 0);
        serverLevel.sendParticles(ParticleTypes.CRIT, targetPos.x, targetPos.y, targetPos.z, 8, 0.2, 0.2, 0.2, 0.1);
        serverLevel.sendParticles(ParticleTypes.WITCH, targetPos.x, targetPos.y, targetPos.z, 10, 0.3, 0.3, 0.3, 0.12);
        for (int i = 0; i < 4; i++) {
            double angle = (i / 4.0) * 2 * Math.PI;
            double x = targetPos.x + Math.cos(angle) * 0.8;
            double z = targetPos.z + Math.sin(angle) * 0.8;
            serverLevel.sendParticles(ParticleTypes.PORTAL, x, targetPos.y, z, 1, 0.1, 0.1, 0.1, 0.05);
        }
    }

    @Override
    protected void onStop() {
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
            serverLevel.sendParticles(ParticleTypes.WITCH, userPos.x, userPos.y, userPos.z, 8, 0.5, 0.5, 0.5, 0.1);
        }
        stingExecuted = false;
    }
}