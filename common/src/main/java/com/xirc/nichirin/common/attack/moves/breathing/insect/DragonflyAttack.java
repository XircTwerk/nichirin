package com.xirc.nichirin.common.attack.moves.breathing.insect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Third Form: Dance of the Dragonfly – Compound Eye Hexagon. 6 rapid forward stabs while the user stays stationary.
public class DragonflyAttack extends InsectBreathingAttackBase {

    private static final int STAB_COUNT = 6;
    private static final int STABS_INTERVAL = 5;

    private Vec3 slashDirection;
    private int stabsExecuted = 0;
    private int nextStabTick = 0;

    @Override
    protected void onStart() {
        stabsExecuted = 0;
        nextStabTick = 0;
        slashDirection = user.getLookAngle().normalize();
        user.setDeltaMovement(Vec3.ZERO);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.DROWNED_AMBIENT, SoundSource.PLAYERS, 0.8f, 1.8f);
        createCompoundEyeEffect();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        user.setDeltaMovement(Vec3.ZERO);

        if (tickCount > windup && stabsExecuted < STAB_COUNT) {
            if (tickCount >= windup + nextStabTick) {
                executeStab();
                stabsExecuted++;
                nextStabTick += STABS_INTERVAL;
            }
        }

        if (tickCount % 3 == 0) {
            createTargetingBeam();
        }
    }

    private void createCompoundEyeEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        for (int ring = 0; ring < 3; ring++) {
            int pointsInRing = ring == 0 ? 1 : ring * 6;
            float ringRadius = ring * 0.8f;

            for (int i = 0; i < pointsInRing; i++) {
                double angle = ring == 0 ? 0 : (i / (double) pointsInRing) * 2 * Math.PI;
                double x = userPos.x + Math.cos(angle) * ringRadius;
                double z = userPos.z + Math.sin(angle) * ringRadius;
                double y = userPos.y + 1.5;

                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        x, y, z, 1, 0.02, 0.02, 0.02, 0.01);

                if (ring == 2) {
                    serverLevel.sendParticles(ParticleTypes.WITCH,
                            x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
                }
            }
        }
    }

    private void createTargetingBeam() {
        if (!(world instanceof ServerLevel serverLevel)) return;
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        createInsectTrail(userPos, userPos.add(slashDirection.scale(range)));
    }

    private void executeStab() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        List<LivingEntity> frontTargets = getTargetsInCustomHitbox(
                userPos.add(slashDirection.scale(1.5)), 2.0, 2.0, 3.0);

        for (LivingEntity frontTarget : frontTargets) {
            hitTargetNoImmunity(frontTarget);

            boolean isFinalStab = (stabsExecuted == STAB_COUNT - 1);

            if (isFinalStab) {
                frontTarget.push(slashDirection.x * knockback * 2.0, 0.5, slashDirection.z * knockback * 2.0);
                frontTarget.invulnerableTime = hitStun * 3;
                createFinalStabEffect();
                world.playSound(null, frontTarget.getX(), frontTarget.getY(), frontTarget.getZ(),
                        SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 1.5f);
            } else {
                frontTarget.push(slashDirection.x * knockback * 0.2, 0.05, slashDirection.z * knockback * 0.2);
                world.playSound(null, frontTarget.getX(), frontTarget.getY(), frontTarget.getZ(),
                        SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.8f, 1.8f);
            }

            createStabImpactEffect(isFinalStab, frontTarget.position());
            if (stabsExecuted % 2 == 0) {
                playPoisonSound(frontTarget.position());
            }
        }
    }

    private void createStabImpactEffect(boolean isFinalStab, Vec3 targetPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        Vec3 impactPos = targetPos.add(0, 1, 0);

        if (isFinalStab) {
            serverLevel.sendParticles(ParticleTypes.CRIT, impactPos.x, impactPos.y, impactPos.z, 25, 0.5, 0.5, 0.5, 0.3);
            createPoisonBurst(impactPos, 2.0f);
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, impactPos.x, impactPos.y + 1, impactPos.z, 5, 0.3, 0.3, 0.3, 0.1);
            for (int i = 0; i < 12; i++) {
                double angle = (i / 12.0) * 2 * Math.PI;
                double x = impactPos.x + Math.cos(angle) * 2.5;
                double z = impactPos.z + Math.sin(angle) * 2.5;
                serverLevel.sendParticles(ParticleTypes.PORTAL, x, impactPos.y + 1, z, 3, 0.3, 0.3, 0.3, 0.15);
            }
        } else {
            serverLevel.sendParticles(ParticleTypes.CRIT, impactPos.x, impactPos.y, impactPos.z, 5, 0.2, 0.2, 0.2, 0.1);
            serverLevel.sendParticles(ParticleTypes.WITCH, impactPos.x, impactPos.y, impactPos.z, 8, 0.3, 0.3, 0.3, 0.12);
            serverLevel.sendParticles(ParticleTypes.PORTAL, impactPos.x, impactPos.y, impactPos.z, 3, 0.2, 0.2, 0.2, 0.08);
        }
    }

    private void createFinalStabEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * 2 * Math.PI;
            double x = userPos.x + Math.cos(angle) * 2.0;
            double z = userPos.z + Math.sin(angle) * 2.0;
            double y = userPos.y + Math.sin(angle * 4) * 0.5;
            serverLevel.sendParticles(ParticleTypes.WITCH, x, y, z, 2, 0.2, 0.2, 0.2, 0.1);
            if (i % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT, x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    @Override
    public boolean isPrecisionAttack() {
        return true;
    }

    @Override
    protected void onStop() {
        user.setDeltaMovement(Vec3.ZERO);

        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
            createCompoundEyeEffect();
            serverLevel.sendParticles(ParticleTypes.PORTAL, userPos.x, userPos.y, userPos.z, 30, 1.5, 1.5, 1.5, 0.25);
            serverLevel.sendParticles(ParticleTypes.ENCHANT, userPos.x, userPos.y, userPos.z, 20, 1.0, 1.0, 1.0, 0.2);
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.5f);

        stabsExecuted = 0;
        nextStabTick = 0;
    }
}