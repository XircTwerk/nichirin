package com.xirc.nichirin.common.attack.moves.breathing.water;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Third Form: Flowing Dance
 * Empowers the user with strength and speed while they slash forth
 * Creates a trail behind the user and constantly damages in front during active duration
 * Allows for comboing with other moves
 *
 * All configuration comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class FlowingDanceAttack extends WaterBreathingAttackBase {

    private boolean danceStarted = false;
    private boolean empowered = false;
    private Set<LivingEntity> hitEntities = new HashSet<>();
    private int danceTicks = 0;

    public FlowingDanceAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        danceStarted = false;
        empowered = false;
        hitEntities.clear();
        danceTicks = 0;

        // Flowing dance startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Start dance after windup
        if (!danceStarted && tickCount == windup + 1) {
            startDance();
            danceStarted = true;
        }

        // Continue dance effects during duration
        if (danceStarted && tickCount > windup && tickCount < windup + duration) {
            danceTicks++;
            performDance();
        }
    }

    private void startDance() {
        // Apply empowerment effects to user
        applyEmpowerment();
        empowered = true;

        // Dance start sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.5f);

        // Create initial empowerment effect
        createEmpowermentEffect();
    }

    private void applyEmpowerment() {
        // Apply strength and speed buffs
        int empowermentDuration = duration;

        // Strength effect (damage boost)
        user.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST,
                empowermentDuration,
                1, // Amplifier 1 (Strength II)
                false, // Not ambient
                true   // Show particles
        ));

        // Speed effect
        user.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                empowermentDuration,
                1, // Amplifier 1 (Speed II)
                false, // Not ambient
                true   // Show particles
        ));

        // Regeneration for combo potential
        user.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                empowermentDuration,
                0, // Amplifier 0 (Regeneration I)
                false, // Not ambient
                false  // Don't show particles (too much visual noise)
        ));
    }

    private void performDance() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create flowing trail behind user
        createFlowingTrail(userPos);

        // Constantly damage enemies in front every few ticks
        if (danceTicks % 8 == 0) { // Hit every 8 ticks (0.4 seconds)
            List<LivingEntity> targets = getTargetsAtRange();

            for (LivingEntity target : targets) {
                // Allow re-hitting the same target but track for spacing
                if (!hasRecentlyHit(target)) {
                    hitTargetNoImmunity(target);
                    trackRecentHit(target);

                    // Very light knockback to keep enemies close for comboing
                    Vec3 lightKnockback = target.position().subtract(userPos).normalize();
                    target.push(lightKnockback.x * knockback * 0.5, 0.02, lightKnockback.z * knockback * 0.5);

                    // Individual hit sound
                    world.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.4f, 1.3f + danceTicks * 0.02f);
                }
            }

            // Create forward slashing effect
            createForwardSlashEffect(userPos, lookDir);
        }

        // Continuous flowing sound
        if (danceTicks % 15 == 0) {
            playWaterFlowSound(userPos);
        }

        // Empowerment particle effects around user
        if (danceTicks % 5 == 0) {
            createEmpowermentParticles(userPos);
        }
    }

    private boolean hasRecentlyHit(LivingEntity target) {
        // Allow hitting the same target again after 16 ticks (0.8 seconds)
        return hitEntities.contains(target) && danceTicks % 16 > 8;
    }

    private void trackRecentHit(LivingEntity target) {
        hitEntities.add(target);
        // Clear old hits periodically to allow re-hitting
        if (danceTicks % 32 == 0) {
            hitEntities.clear();
        }
    }

    private void createEmpowermentEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Empowerment aura - expanding rings
        for (int ring = 1; ring <= 3; ring++) {
            float radius = ring * 1.0f;
            int particlesInRing = 8 * ring;

            for (int i = 0; i < particlesInRing; i++) {
                double angle = (2 * Math.PI * i) / particlesInRing;
                double x = userPos.x + Math.cos(angle) * radius;
                double z = userPos.z + Math.sin(angle) * radius;
                double y = userPos.y + Math.sin(angle * 3) * 0.3; // Wavy effect

                // Empowerment particles (enchant + water)
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        x, y, z, 2, 0.1, 0.1, 0.1, 0.05);

                if (ring == 1) { // Inner ring gets water particles
                    serverLevel.sendParticles(ParticleTypes.SPLASH,
                            x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
                }
            }
        }
    }

    private void createFlowingTrail(Vec3 userPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Get user's movement velocity to create trail
        Vec3 velocity = user.getDeltaMovement();
        Vec3 trailDirection = velocity.normalize();

        // If not moving much, use look direction
        if (velocity.length() < 0.1) {
            trailDirection = user.getLookAngle().scale(-1); // Behind user
        }

        // Create flowing trail behind user
        for (int i = 1; i <= 6; i++) {
            Vec3 trailPos = userPos.add(trailDirection.scale(i * 0.3));

            // Flowing water particles
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    trailPos.x, trailPos.y, trailPos.z,
                    2, 0.2, 0.2, 0.2, 0.1);

            // Empowerment sparkles in trail
            if (i <= 3) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        trailPos.x, trailPos.y, trailPos.z,
                        1, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    private void createForwardSlashEffect(Vec3 userPos, Vec3 lookDir) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Create slashing water effect in front of user
        for (int i = 1; i <= 3; i++) {
            Vec3 slashPos = userPos.add(lookDir.scale(i * (range / 3)));

            // Main slash particles
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    slashPos.x, slashPos.y, slashPos.z,
                    4, 0.3, 0.3, 0.3, 0.12);

            // Side slashes for wider area
            Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();
            for (int side = -1; side <= 1; side += 2) {
                Vec3 sideSlashPos = slashPos.add(rightDir.scale(side * 0.8));
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        sideSlashPos.x, sideSlashPos.y, sideSlashPos.z,
                        2, 0.2, 0.2, 0.2, 0.08);
            }
        }
    }

    private void createEmpowermentParticles(Vec3 userPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Continuous empowerment aura
        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                userPos.x, userPos.y, userPos.z,
                3, 0.4, 0.4, 0.4, 0.1);

        // Flowing water around user
        double angle = danceTicks * 0.2; // Rotating particles
        for (int i = 0; i < 4; i++) {
            double particleAngle = angle + (i * Math.PI / 2);
            double x = userPos.x + Math.cos(particleAngle) * 1.2;
            double z = userPos.z + Math.sin(particleAngle) * 1.2;
            double y = userPos.y + Math.sin(particleAngle * 2) * 0.3;

            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    x, y, z, 1, 0.05, 0.05, 0.05, 0.03);
        }
    }

    @Override
    protected void onStop() {
        // Clear hit tracking
        hitEntities.clear();
        danceTicks = 0;
        danceStarted = false;
        empowered = false;

        // Final flowing effect
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

            // Final empowerment burst
            createEmpowermentEffect();

            // Final water flourish
            createWaterExplosion(userPos, 1.0f);
        }

        // Final empowerment sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.0f);
    }
}