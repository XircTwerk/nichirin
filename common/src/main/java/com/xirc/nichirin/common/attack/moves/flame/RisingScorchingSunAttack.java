package com.xirc.nichirin.common.attack.moves.flame;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Second Form: Rising Scorching Sun
 * The user unleashes an arcing vertical slash in an upwards motion, often brought up from a tail guard.
 *
 * Mechanics:
 * - Arced slash that knocks targets up
 * - Deals bonus damage to airborne enemies
 *
 * All configuration comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class RisingScorchingSunAttack extends FlameBreathingAttackBase {

    private boolean hasExecuted = false;

    public RisingScorchingSunAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        hasExecuted = false;

        // Rising flame sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.5f);

        // Create initial upward flame particles
        createFlameParticles();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Execute the upward arc once after windup completes
        if (!hasExecuted && tickCount == windup + 1) {
            executeRisingSlash();
            hasExecuted = true;
        }
    }

    private void executeRisingSlash() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create upward arc of flame particles and hit detection
        createRisingArcEffect();

        // Hit enemies in the upward arc path
        for (int i = 1; i <= 6; i++) {
            double arcProgress = i / 6.0;
            double height = Math.sin(arcProgress * Math.PI) * 4; // 4 block high arc
            double forward = arcProgress * range;

            Vec3 arcPoint = userPos.add(lookDir.scale(forward)).add(0, height, 0);
            List<LivingEntity> targets = getTargetsInCustomHitbox(arcPoint, 2.0, 2.0, 2.0);

            for (LivingEntity target : targets) {
                // Check if target is airborne for bonus damage
                boolean isAirborne = !target.onGround() || target.getDeltaMovement().y > 0;

                if (isAirborne) {
                    // Apply bonus damage to airborne enemies (50% more)
                    float originalDamage = damage;
                    damage = damage * 1.5f;
                    hitTarget(target);
                    damage = originalDamage; // Reset damage

                    // Extra flame burst for airborne hits
                    createFlameHitParticles(target.position().add(0, 1, 0));

                    // Bonus fire duration
                    target.setSecondsOnFire(getFireDuration() + 5);
                } else {
                    hitTarget(target);
                }

                // Strong upward knockback regardless
                Vec3 upwardKnockback = new Vec3(
                        target.getDeltaMovement().x * 0.5, // Reduce horizontal momentum
                        knockback * 2.0, // Strong upward launch
                        target.getDeltaMovement().z * 0.5
                );
                target.setDeltaMovement(upwardKnockback);
                target.hurtMarked = true;
                target.hasImpulse = true;

                // Play hit sound
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 1.4f);
            }
        }

        // Sword slash sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.3f);
    }

    /**
     * Create the visual effect of an upward arcing flame slash
     */
    private void createRisingArcEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create the upward arc with flame particles
        for (int i = 0; i <= 20; i++) {
            double progress = i / 20.0;
            double height = Math.sin(progress * Math.PI) * 4; // 4 block high arc
            double forward = progress * range;

            Vec3 arcPos = userPos.add(lookDir.scale(forward)).add(0, height, 0);

            // Main flame trail
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    arcPos.x, arcPos.y, arcPos.z,
                    4, 0.2, 0.2, 0.2, 0.1);

            if (progress > 0.3 && progress < 0.7) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        arcPos.x, arcPos.y, arcPos.z,
                        2, 0.1, 0.1, 0.1, 0.05);
            }

            // Upward flame streams
            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        arcPos.x, arcPos.y, arcPos.z,
                        6, 0.3, 0.8, 0.3, 0.2);
            }
        }

        // Create flame pillars at key points of the arc
        for (int pillar = 0; pillar < 4; pillar++) {
            double pillarProgress = (pillar + 1) / 5.0;
            double pillarHeight = Math.sin(pillarProgress * Math.PI) * 4;
            double pillarForward = pillarProgress * range;

            Vec3 pillarPos = userPos.add(lookDir.scale(pillarForward)).add(0, pillarHeight, 0);

            // Flame pillar
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    pillarPos.x, pillarPos.y, pillarPos.z,
                    10, 0.3, 1.5, 0.3, 0.3);

            // Smoke at the base
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    pillarPos.x, pillarPos.y - 1, pillarPos.z,
                    5, 0.5, 0.5, 0.5, 0.1);
        }
    }

    @Override
    protected void onStop() {
        // Reset state
        hasExecuted = false;

        // Final upward flame burst
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    userPos.x, userPos.y + 2, userPos.z,
                    20, 1.0, 2.0, 1.0, 0.3);
        }
    }
}