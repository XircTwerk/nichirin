package com.xirc.nichirin.common.attack.moves.breathing.sound;

import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;

/**
 * Rhythmic Step (Crouch + Right Click Attack)
 * 12 block dash with continuous hitboxes leaving a particle explosion trail
 *
 * Mechanics:
 * - Deals damage to all enemies during dash (like UnknowingFireAttack)
 * - Does a slash at the end
 * - Continuous movement with constant velocity
 * - Explosion trail effects
 */
public class RhythmicStepAttack extends SoundBreathingAttackBase {

    private static final float SLASH_WIDTH = 6.0f;
    private static final float SLASH_DEPTH = 4.0f;

    private boolean dashStarted = false;
    private boolean slashExecuted = false;
    private Vec3 dashDirection;
    private int dashTick = 0;
    private final Set<LivingEntity> hitEntities = new HashSet<>();

    public RhythmicStepAttack() {
    }

    @Override
    protected void onStart() {
        dashStarted = false;
        slashExecuted = false;
        dashTick = 0;
        hitEntities.clear();

        dashDirection = angledDashDirection();
    }

    @Override
    protected void onActiveStart() {
        // Rhythmic step preparation sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.8f, 2.0f);

        // Create initial step preparation effect
        createStepPreparationEffect();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Start dash immediately since windup is 0
        if (!dashStarted && tickCount == 1) {
            startDash();
            dashStarted = true;
        }

        // Continue dash for dashDuration ticks, then stop and slash
        if (dashStarted) {
            int dashDuration = dashSpeed != null && dashSpeed > 0
                    ? Math.round(range / dashSpeed * 20f)
                    : duration;
            dashDuration = Math.max(1, Math.min(dashDuration, duration));

            if (dashTick < dashDuration) {
                continueDash();
                dashTick++;
            } else if (!slashExecuted) {
                user.setDeltaMovement(Vec3.ZERO);
                user.hurtMarked = true;
                executeFinishingSlash();
                slashExecuted = true;
            }
        }

        // Explosion trail during dash
        if (dashStarted && !slashExecuted) {
            createContinuousTrailExplosions();
        }
    }

    private void startDash() {
        float speed = dashSpeed != null ? dashSpeed : 4.0f;
        Vec3 dashVelocity = dashDirection.scale(speed);
        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;
        user.hasImpulse = true;

        // Dash start sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 1.8f);

        // Create initial dash burst
        createRhythmicDashEffect();
    }

    private void continueDash() {
        float speed = dashSpeed != null ? dashSpeed : 4.0f;
        Vec3 dashVelocity = dashDirection.scale(speed);
        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;

        // Create continuous dash trail
        createIntenseDashTrail();

        // Hit enemies during dash with constant hitboxes - using UnknowingFireAttack's method
        List<LivingEntity> dashTargets = getTargetsInCustomHitbox(
                user.position().add(0, user.getBbHeight() / 2, 0),
                hitboxSize, hitboxSize * 1.25, hitboxSize);

        for (LivingEntity target : dashTargets) {
            if (!hitEntities.contains(target)) {
                // Light dash damage - respects immunity frames
                hitTarget(target);

                applyDisorientedEffect(target);

                hitEntities.add(target);

                // Create impact particles
                createDashHitEffect(target.position());

                // Hit sound
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 1.8f);
            }
        }
    }



    private void executeFinishingSlash() {
        // Create MASSIVE horizontal slash effect like UnknowingFireAttack
        createFinishingSlashEffect();

        // Hit all enemies in the massive slash area
        Vec3 userPos = user.position().add(0, user.getBbHeight() * 0.7, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();

        // Create multiple hitboxes for the wide slash
        for (int i = -3; i <= 3; i++) {
            double offset = i * (SLASH_WIDTH / 6.0);
            Vec3 slashCenter = userPos.add(lookDir.scale(SLASH_DEPTH / 2)).add(rightDir.scale(offset));

            List<LivingEntity> targets = getTargetsInCustomHitbox(slashCenter, hitboxSize, hitboxSize * 1.5, hitboxSize);

            for (LivingEntity target : targets) {
                // Enhanced damage for finishing slash
                float originalDamage = damage;
                damage = damage * 1.5f; // 50% bonus damage

                hitTarget(target);

                damage = originalDamage; // Restore

                // Extended stun
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        30, // 1.5 seconds
                        5,
                        false,
                        false
                ));
            }
        }

        // Finishing slash sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 0.8f);
    }

    private void createIntenseDashTrail() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Dense sound trail behind user
        for (int i = 1; i <= 6; i++) {
            Vec3 trailPos = userPos.subtract(dashDirection.scale(i * 0.4));

            serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                    trailPos.x, trailPos.y, trailPos.z,
                    2, 0.3, 0.3, 0.3, 0.1);

            serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH2.get(),
                    trailPos.x, trailPos.y, trailPos.z,
                    1, 0.2, 0.2, 0.2, 0.08);
        }

        // Side sound waves during dash
        Vec3 rightDir = dashDirection.cross(new Vec3(0, 1, 0)).normalize();
        for (int side = -1; side <= 1; side += 2) {
            Vec3 sidePos = userPos.add(rightDir.scale(side * 1.0));
            serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                    sidePos.x, sidePos.y, sidePos.z,
                    1, 0.2, 0.2, 0.2, 0.1);
        }
    }

    private void createContinuousTrailExplosions() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Create trail explosions at current position every few ticks
        if (tickCount % 3 == 0) { // Every 3 ticks
            Vec3 explosionPos = user.position().add(0, 0.5, 0);

            // Trail explosion effect
            serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                    explosionPos.x, explosionPos.y, explosionPos.z,
                    2, 1.0, 1.0, 1.0, 0.2);

            serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                    explosionPos.x, explosionPos.y, explosionPos.z,
                    1, 0.8, 0.8, 0.8, 0.1);

            serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                    explosionPos.x, explosionPos.y, explosionPos.z,
                    1, 0.5, 0.5, 0.5, 0.1);

            // Small explosion sound
            world.playSound(null, explosionPos.x, explosionPos.y, explosionPos.z,
                    SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.2f, 1.5f);
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

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Start position burst with blue flash
        serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                userPos.x, userPos.y, userPos.z,
                15, 1.0, 1.0, 1.0, 0.3);

        serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH2.get(),
                userPos.x, userPos.y, userPos.z,
                10, 0.8, 0.8, 0.8, 0.2);

        serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                userPos.x, userPos.y, userPos.z,
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
        Vec3 rightDir = dashDirection.cross(new Vec3(0, 1, 0)).normalize();

        // Create slash effect in front of player
        Vec3 slashCenter = userPos.add(dashDirection.scale(2.0));

        // Finishing slash arc
        for (int i = -30; i <= 30; i += 10) {
            double angle = Math.toRadians(i);
            Vec3 slashDir = dashDirection.scale(Math.cos(angle)).add(rightDir.scale(Math.sin(angle)));

            for (double r = 0.5; r <= 3.0; r += 0.4) {
                Vec3 slashPos = slashCenter.add(slashDir.scale(r));

                serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                        slashPos.x, slashPos.y, slashPos.z,
                        2, 0.1, 0.1, 0.1, 0.05);
            }
        }

        // Horizontal slash trail
        for (double t = -2.0; t <= 2.0; t += 0.4) {
            Vec3 slashPos = slashCenter.add(rightDir.scale(t));

            serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                    slashPos.x, slashPos.y, slashPos.z,
                    2, 0.2, 0.2, 0.2, 0.1);
        }

        // Central finishing burst
        serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                slashCenter.x, slashCenter.y, slashCenter.z,
                15, 0.8, 0.8, 0.8, 0.3);

        serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                slashCenter.x, slashCenter.y, slashCenter.z,
                10, 0.6, 0.6, 0.6, 0.2);
    }

    /**
     * Get targets in a cone shape
     */
    private List<LivingEntity> getTargetsInCone(Vec3 origin, Vec3 direction, double range, double angleDegrees) {
        double angleRadians = Math.toRadians(angleDegrees / 2);

        return world.getEntitiesOfClass(LivingEntity.class,
                new AABB(origin.subtract(range, 2, range), origin.add(range, 2, range)),
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
        dashStarted = false;
        slashExecuted = false;
        dashTick = 0;
        hitEntities.clear();

        // Final rhythmic echo
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.0f);
        }
    }

    private Vec3 angledDashDirection() {
        Vec3 look = user.getLookAngle();
        return new Vec3(look.x, Math.max(-0.25, Math.min(0.25, look.y)), look.z).normalize();
    }
}