package com.xirc.nichirin.common.attack.moves.sound;

import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Rhythmic Step (Crouch + Right Click Attack)
 * 8 block near instant dash leaving a particle explosion trail
 *
 * Mechanics:
 * - Deals damage to all enemies where they dashed past
 * - Does a slash at the end
 * - Near-instant movement
 * - Explosion trail effects
 *
 * All configuration comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class RhythmicStepAttack extends SoundBreathingAttackBase {

    private boolean dashExecuted = false;
    private Vec3 dashDirection;
    private Vec3 startPosition;
    private Vec3 endPosition;
    private final List<Vec3> dashPath = new ArrayList<>();
    private boolean finishingSlash = false;

    public RhythmicStepAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        dashExecuted = false;
        finishingSlash = false;
        dashPath.clear();

        // Force horizontal direction only (ignore Y component)
        Vec3 rawDirection = user.getLookAngle();
        dashDirection = new Vec3(rawDirection.x, 0, rawDirection.z).normalize();
        startPosition = user.position();

        // Calculate end position (4 blocks to match the velocity)
        endPosition = startPosition.add(dashDirection.scale(4.0));

        // Rhythmic step preparation sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.8f, 2.0f);

        // Create initial step preparation effect
        createStepPreparationEffect();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Execute near-instant dash after minimal windup
        if (!dashExecuted && tickCount == windup + 1) {
            // Clear notes when the dash executes
            if (user != null && user.level().isClientSide && user.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.MUSICAL_SCORE.get())) {
                com.xirc.nichirin.client.gui.RhythmMeter.clearTargetedNotes();
            }

            executeRhythmicDash();
            dashExecuted = true;
        }

        // Stop the dash after just 2 ticks for shorter distance
        if (dashExecuted && tickCount == windup + 2) {
            user.setDeltaMovement(Vec3.ZERO);
            user.hurtMarked = true;
        }

        // Finishing slash at the end
        if (dashExecuted && !finishingSlash && tickCount >= windup + 5) {
            executeFinishingSlash();
            finishingSlash = true;
        }

        // Continue explosion trail effects
        if (dashExecuted && tickCount <= windup + 8) {
            createContinuousTrailExplosions();
        }
    }

    private void executeRhythmicDash() {
        // Use much lower velocity for proper 4-block movement
        Vec3 dashVelocity = dashDirection.scale(8.0f);
        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;
        user.hasImpulse = true;

        // Calculate path points for damage and effects
        calculateDashPath();

        // Deal damage along the entire dash path
        damageAlongPath();

        // Create massive dash effect with blue particles
        createRhythmicDashEffect();

        // Dash sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 1.8f);
    }

    private void calculateDashPath() {
        // Create path points from start to end
        Vec3 direction = endPosition.subtract(startPosition);
        double distance = direction.length();
        Vec3 normalized = direction.normalize();

        dashPath.clear();
        for (double d = 0; d <= distance; d += 0.5) {
            Vec3 pathPoint = startPosition.add(normalized.scale(d));
            dashPath.add(pathPoint);
        }
    }

    private void damageAlongPath() {
        // Hit all enemies along the dash path
        for (Vec3 pathPoint : dashPath) {
            List<LivingEntity> targets = getTargetsInCustomHitbox(pathPoint, 2.0, 2.0, 2.0);

            for (LivingEntity target : targets) {
                hitTarget(target);

                // Light knockback to not disrupt the dash
                Vec3 lightKnockback = dashDirection.scale(knockback * 0.5);
                target.push(lightKnockback.x, 0.2, lightKnockback.z);
                target.hurtMarked = true;

                // Dash hit effect
                createDashHitEffect(target.position());

                // Hit sound
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 1.8f);
            }
        }
    }

    private void executeFinishingSlash() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Create finishing slash effect
        createFinishingSlashEffect();

        // Hit enemies in front of user at end position
        List<LivingEntity> finishTargets = getTargetsInCone(userPos, dashDirection, 4.0, 60);

        for (LivingEntity target : finishTargets) {
            // Enhanced damage for finishing slash
            float originalDamage = damage;
            damage = damage * 1.5f; // 50% bonus damage

            hitTarget(target);

            damage = originalDamage; // Restore

            // Strong finishing knockback
            Vec3 finishingKnockback = dashDirection.scale(knockback * 2.0);
            target.push(finishingKnockback.x, 0.5, finishingKnockback.z);
            target.hurtMarked = true;

            // Extended stun
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                    30, // 1.5 seconds
                    5,
                    false,
                    false
            ));
        }

        // Finishing slash sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 0.8f);
    }

    private void createContinuousTrailExplosions() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Create explosions along the path over time
        int pathIndex = (tickCount - windup - 2) * 2; // 2 explosions per tick

        for (int i = 0; i < 2 && pathIndex + i < dashPath.size(); i++) {
            Vec3 explosionPos = dashPath.get(pathIndex + i);

            // Trail explosion effect
            serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                    explosionPos.x, explosionPos.y + 0.5, explosionPos.z,
                    2, 1.0, 1.0, 1.0, 0.2);

            serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                    explosionPos.x, explosionPos.y, explosionPos.z,
                    1, 0.8, 0.8, 0.8, 0.1);

            if (i == 0) { // Only one flash particle per tick
                serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                        explosionPos.x, explosionPos.y + 0.5, explosionPos.z,
                        1, 0.5, 0.5, 0.5, 0.1);
            }

            // Small explosion sound
            if (i == 0) { // Only play sound once per tick to avoid spam
                world.playSound(null, explosionPos.x, explosionPos.y, explosionPos.z,
                        SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.2f, 1.5f);
            }
        }
    }

    private void createStepPreparationEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Gathering energy effect with blue particles
        for (int i = 0; i < 15; i++) {
            double angle = (i / 15.0) * 2 * Math.PI;
            double radius = 1.5;
            Vec3 gatherPos = userPos.add(
                    Math.cos(angle) * radius,
                    Math.sin(angle * 2) * 0.3,
                    Math.sin(angle) * radius
            );

            // Mix of sound and blue flash particles
            serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                    gatherPos.x, gatherPos.y, gatherPos.z,
                    1, 0.05, 0.05, 0.05, 0.02);

            if (i % 3 == 0) {
                serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH2.get(),
                        gatherPos.x, gatherPos.y, gatherPos.z,
                        1, 0.05, 0.05, 0.05, 0.02);
            }
        }

        // Ground preparation effect
        serverLevel.sendParticles(ParticleTypes.POOF,
                userPos.x, userPos.y - 1, userPos.z,
                5, 0.5, 0.1, 0.5, 0.1);
    }

    private void createRhythmicDashEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Massive dash trail effect with blue particles
        for (Vec3 pathPoint : dashPath) {
            // Main dash trail - mix of sound and blue flash
            serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                    pathPoint.x, pathPoint.y + 0.5, pathPoint.z,
                    3, 0.3, 0.3, 0.3, 0.1);

            serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH2.get(),
                    pathPoint.x, pathPoint.y + 1, pathPoint.z,
                    2, 0.2, 0.2, 0.2, 0.08);

            // Speed lines
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    pathPoint.x, pathPoint.y + 0.5, pathPoint.z,
                    1, 0.1, 0.1, 0.1, 0.05);
        }

        // Start position burst with blue flash
        serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                startPosition.x, startPosition.y + 1, startPosition.z,
                15, 1.0, 1.0, 1.0, 0.3);

        serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH2.get(),
                startPosition.x, startPosition.y + 1, startPosition.z,
                10, 0.8, 0.8, 0.8, 0.2);

        // End position impact with blue flash
        serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                endPosition.x, endPosition.y + 1, endPosition.z,
                15, 1.5, 1.5, 1.5, 0.4);

        serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH2.get(),
                endPosition.x, endPosition.y + 1, endPosition.z,
                12, 1.2, 1.2, 1.2, 0.3);

        serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                endPosition.x, endPosition.y + 1, endPosition.z,
                2, 0.5, 0.5, 0.5, 0);
    }

    private void createDashHitEffect(Vec3 hitPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Quick hit burst
        serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                hitPos.x, hitPos.y + 0.5, hitPos.z,
                3, 0.2, 0.2, 0.2, 0.1);

        serverLevel.sendParticles(ParticleTypes.CRIT,
                hitPos.x, hitPos.y + 0.5, hitPos.z,
                5, 0.3, 0.3, 0.3, 0.2);
    }

    private void createFinishingSlashEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Finishing slash arc
        for (int i = -30; i <= 30; i += 10) {
            double angle = Math.toRadians(i);
            Vec3 rightDir = dashDirection.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 slashDir = dashDirection.scale(Math.cos(angle)).add(rightDir.scale(Math.sin(angle)));

            for (double r = 1.0; r <= 4.0; r += 0.4) {
                Vec3 slashPos = userPos.add(slashDir.scale(r));

                serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                        slashPos.x, slashPos.y, slashPos.z,
                        2, 0.1, 0.1, 0.1, 0.05);
            }
        }

        // Central finishing burst
        serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                userPos.x, userPos.y, userPos.z,
                20, 0.8, 0.8, 0.8, 0.3);

        serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                userPos.x, userPos.y, userPos.z,
                15, 0.6, 0.6, 0.6, 0.2);
    }

    /**
     * Get targets in a cone shape
     */
    private List<LivingEntity> getTargetsInCone(Vec3 origin, Vec3 direction, double range, double angleDegrees) {
        double angleRadians = Math.toRadians(angleDegrees / 2);

        return world.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(origin.subtract(range, 2, range), origin.add(range, 2, range)),
                entity -> {
                    if (entity == user || !entity.isAlive()) return false;

                    Vec3 toEntity = entity.position().subtract(origin).normalize();
                    double dot = direction.dot(toEntity);
                    double angle = Math.acos(Math.max(-1.0, Math.min(1.0, dot)));

                    return angle <= angleRadians && origin.distanceTo(entity.position()) <= range;
                });
    }

    @Override
    public boolean isDashAttack() {
        return true; // This is a dash attack
    }

    @Override
    protected void onStop() {
        // COMPLETELY stop all movement when attack ends
        user.setDeltaMovement(Vec3.ZERO);
        user.setOnGround(true);
        user.hurtMarked = true; // Force velocity update
        user.hasImpulse = false; // Stop all physics impulses

        // Reset state
        dashExecuted = false;
        finishingSlash = false;
        dashPath.clear();

        // Final rhythmic echo
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.0f);
        }
    }
}