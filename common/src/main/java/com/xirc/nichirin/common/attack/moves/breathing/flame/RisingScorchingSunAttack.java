package com.xirc.nichirin.common.attack.moves.breathing.flame;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Second Form: Rising Scorching Sun
 * The user creates a massive spinning flame wheel around themselves.
 *
 * Mechanics:
 * - Creates a full 360-degree spinning wheel of flames around the player
 * - Deals continuous damage to enemies within the wheel
 * - Deals bonus damage to airborne enemies
 */
public class RisingScorchingSunAttack extends FlameBreathingAttackBase {

    private boolean hasExecuted = false;
    private int wheelTickCount = 0;
    private static final int WHEEL_DURATION = 60; // 3 seconds at 20 ticks per second

    public RisingScorchingSunAttack() {
    }

    @Override
    protected void onStart() {
        hasExecuted = false;
        wheelTickCount = 0;
    }

    @Override
    protected void onActiveStart() {
        // Rising flame sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.5f);

        // Create initial flame particles
        createFlameParticles();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Execute the flame wheel after windup completes
        if (!hasExecuted && tickCount == windup + 1) {
            hasExecuted = true;

            // Play wheel creation sound
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.8f);
        }

        // Continue the wheel rotation
        if (hasExecuted && wheelTickCount < WHEEL_DURATION) {
            executeFlameWheel();
            wheelTickCount++;
        }
    }

    private void executeFlameWheel() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Create the spinning flame wheel effect around the player
        createFlameWheelEffect(userPos);

        // Play spinning sound every 20 ticks
        if (wheelTickCount % 20 == 0) {
            world.playSound(null, userPos.x, userPos.y, userPos.z,
                    SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.8f, 1.5f);
        }
    }

    /**
     * Create the visual effect of a spinning flame wheel around the player
     */
    private void createFlameWheelEffect(Vec3 playerPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        double wheelRadius = 4.5;
        double rotationSpeed = wheelTickCount * 0.3;

        // Get player's look direction
        Vec3 lookDir = user.getLookAngle();
        Vec3 upDir = new Vec3(0, 1, 0); // Up vector

        // Create ONE vertical ring facing the player's look direction
        for (int i = 0; i < 24; i++) {
            double angle = (i / 24.0) * 2 * Math.PI + rotationSpeed;

            // Calculate position on the vertical ring
            double forwardOffset = Math.cos(angle) * wheelRadius;
            double upOffset = Math.sin(angle) * wheelRadius;

            Vec3 rimPos = playerPos.add(lookDir.scale(forwardOffset)).add(upDir.scale(upOffset));

            // Flame particles on the ring
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    rimPos.x, rimPos.y, rimPos.z,
                    3, 0.1, 0.1, 0.1, 0.05);

            // Create hitbox at each particle position
            List<LivingEntity> targets = getTargetsInCustomHitbox(rimPos, hitboxSize * 0.75, hitboxSize * 0.75, hitboxSize * 0.75);

            for (LivingEntity target : targets) {
                // Only hit each target every 10 ticks to prevent spam
                if (wheelTickCount % 10 == 0) {
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
                        target.igniteForSeconds(getFireDuration() + 5);
                    } else {
                        hitTarget(target);
                    }

                    // Moderate knockback away from particle position
                    Vec3 knockbackDir = target.position().subtract(rimPos).normalize();
                    Vec3 finalKnockback = knockbackDir.scale(knockback * 0.8);
                    target.setDeltaMovement(target.getDeltaMovement().add(finalKnockback));
                    target.hurtMarked = true;
                    target.hasImpulse = true;

                    // Play hit sound
                    world.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.6f, 1.2f);
                }
            }
        }
    }

    @Override
    protected void onStop() {
        // Reset state
        hasExecuted = false;
        wheelTickCount = 0;

        // Final flame explosion when wheel dissipates
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

            // Large final flame burst around the player
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    userPos.x, userPos.y, userPos.z,
                    40, 3.0, 2.0, 3.0, 0.5);

            // Additional explosion rings
            for (int ring = 1; ring <= 3; ring++) {
                double ringRadius = ring * 1.5;
                for (int i = 0; i < 16; i++) {
                    double angle = (i / 16.0) * 2 * Math.PI;
                    double x = Math.cos(angle) * ringRadius;
                    double z = Math.sin(angle) * ringRadius;

                    Vec3 ringPos = userPos.add(x, 0, z);
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                            ringPos.x, ringPos.y, ringPos.z,
                            8, 0.5, 0.5, 0.5, 0.2);
                }
            }

            // Explosion sound
            world.playSound(null, userPos.x, userPos.y, userPos.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.2f);
        }
    }
}