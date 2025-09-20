package com.xirc.nichirin.common.attack.moves.breathing.insect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Third Form: Dance of the Dragonfly – Compound Eye Hexagon
 * 6 rapid slashes in front of the user while stationary.
 */
public class DragonflyAttack extends InsectBreathingAttackBase {

    private static final int STAB_COUNT = 6;
    private static final int STABS_INTERVAL = 5; // 5 ticks between stabs (0.25 seconds)

    private Vec3 slashDirection;
    private int stabsExecuted = 0;
    private int nextStabTick = 0;

    public DragonflyAttack() {
        // Configuration comes from moveset
    }

    @Override
    protected void onStart() {
        stabsExecuted = 0;
        nextStabTick = 0;

        // Set slash direction but DON'T move the user
        slashDirection = user.getLookAngle().normalize();

        // Stop any existing movement - user stays in place
        user.setDeltaMovement(Vec3.ZERO);

        // Dragonfly startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.DROWNED_AMBIENT, SoundSource.PLAYERS, 0.8f, 1.8f);

        // Create compound eye targeting effect
        createCompoundEyeEffect();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Keep user stationary - no movement during attack
        user.setDeltaMovement(Vec3.ZERO);

        // Execute stabs at intervals after windup
        if (tickCount > windup && stabsExecuted < STAB_COUNT) {
            if (tickCount >= windup + nextStabTick) {
                executeStab();
                stabsExecuted++;
                nextStabTick += STABS_INTERVAL;
            }
        }

        // Create continuous targeting effect
        if (tickCount % 3 == 0) {
            createTargetingBeam();
        }
    }

    private void createCompoundEyeEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Create hexagonal compound eye pattern around user
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
        Vec3 beamEnd = userPos.add(slashDirection.scale(range));

        // Create targeting beam in slash direction
        createInsectTrail(userPos, beamEnd);
    }

    private void executeStab() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Hit enemies in front of user in slash direction
        List<LivingEntity> frontTargets = getTargetsInCustomHitbox(
                userPos.add(slashDirection.scale(1.5)), // Slash area in front
                2.0, 2.0, 3.0);

        for (LivingEntity frontTarget : frontTargets) {
            // Execute stab with no immunity frames for rapid multi-hit
            hitTargetNoImmunity(frontTarget);

            // Final stab has extra effects
            boolean isFinalStab = (stabsExecuted == STAB_COUNT - 1);

            if (isFinalStab) {
                // Final stab has stronger knockback and longer stun
                frontTarget.push(
                        slashDirection.x * knockback * 2.0,
                        0.5, // Upward component for stagger
                        slashDirection.z * knockback * 2.0
                );

                // Extended stun for stagger effect
                frontTarget.invulnerableTime = hitStun * 3;

                // Create final stab impact
                createFinalStabEffect();

                // Dramatic final sound
                world.playSound(null, frontTarget.getX(), frontTarget.getY(), frontTarget.getZ(),
                        SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 1.5f);
            } else {
                // Regular light knockback for multi-hit
                frontTarget.push(
                        slashDirection.x * knockback * 0.2,
                        0.05,
                        slashDirection.z * knockback * 0.2
                );

                // Quick stab sound
                world.playSound(null, frontTarget.getX(), frontTarget.getY(), frontTarget.getZ(),
                        SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.8f, 1.8f);
            }

            // Create stab impact effect
            createStabImpactEffect(isFinalStab, frontTarget.position());

            // Poison application sound
            if (stabsExecuted % 2 == 0) {
                playPoisonSound(frontTarget.position());
            }
        }
    }

    private void createStabImpactEffect(boolean isFinalStab, Vec3 targetPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 impactPos = targetPos.add(0, 1, 0);

        if (isFinalStab) {
            // Massive final impact
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    impactPos.x, impactPos.y, impactPos.z,
                    25, 0.5, 0.5, 0.5, 0.3);

            // Explosion of poison
            createPoisonBurst(impactPos, 2.0f);

            // Stagger effect particles
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    impactPos.x, impactPos.y + 1, impactPos.z,
                    5, 0.3, 0.3, 0.3, 0.1);

            // Final dragonfly scatter
            for (int i = 0; i < 12; i++) {
                double angle = (i / 12.0) * 2 * Math.PI;
                double distance = 2.5;

                double x = impactPos.x + Math.cos(angle) * distance;
                double z = impactPos.z + Math.sin(angle) * distance;
                double y = impactPos.y + 1;

                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        x, y, z, 3, 0.3, 0.3, 0.3, 0.15);
            }
        } else {
            // Regular stab impact
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    impactPos.x, impactPos.y, impactPos.z,
                    5, 0.2, 0.2, 0.2, 0.1);

            serverLevel.sendParticles(ParticleTypes.WITCH,
                    impactPos.x, impactPos.y, impactPos.z,
                    8, 0.3, 0.3, 0.3, 0.12);

            // Small dragonfly flutter
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    impactPos.x, impactPos.y, impactPos.z,
                    3, 0.2, 0.2, 0.2, 0.08);
        }
    }

    private void createFinalStabEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Dragonfly wing burst around user
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * 2 * Math.PI;
            double radius = 2.0;
            double wingFlap = Math.sin(angle * 4) * 0.5; // Wing flapping motion

            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + wingFlap;

            serverLevel.sendParticles(ParticleTypes.WITCH,
                    x, y, z, 2, 0.2, 0.2, 0.2, 0.1);

            if (i % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    @Override
    public boolean isPrecisionAttack() {
        return true;
    }

    @Override
    protected void onStop() {
        // Make sure user is stopped
        user.setDeltaMovement(Vec3.ZERO);

        // Final dragonfly effect
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

            // Final compound eye dissolution
            createCompoundEyeEffect();

            // Lingering dragonfly particles
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    userPos.x, userPos.y, userPos.z,
                    30, 1.5, 1.5, 1.5, 0.25);

            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    userPos.x, userPos.y, userPos.z,
                    20, 1.0, 1.0, 1.0, 0.2);
        }

        // Final dragonfly sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.5f);

        // Clear state
        stabsExecuted = 0;
        nextStabTick = 0;
    }
}