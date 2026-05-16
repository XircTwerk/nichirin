package com.xirc.nichirin.common.attack.moves.breathing.flame;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fourth Form: Blooming Flame Undulation
 * The user performs a singular slash or multiple if needed, in a fluid circular motion
 * to defend from incoming attacks and decapitate multiple targets.
 *
 * Mechanics:
 * - 360 degree spinning slash, omnidirectional
 * - User becomes invulnerable during active frames
 * - Deflects or destroys projectiles during active frames
 * - 3.5 block radius
 *
 * All configuration comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class BloomingFlameUndulationAttack extends FlameBreathingAttackBase {

    private Set<LivingEntity> hitEntities = new HashSet<>();
    private boolean invulnerabilityApplied = false;
    private int spinTicks = 0; // Local counter to avoid threading issues

    public BloomingFlameUndulationAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        hitEntities.clear();
        invulnerabilityApplied = false;
        spinTicks = 0; // Reset local counter

        // Defensive stance sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 1.2f);

        // Initial flame particles
        createFlameParticles();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Apply invulnerability during active phase
        if (tickCount > windup && tickCount < windup + duration) {
            if (!invulnerabilityApplied) {
                user.setInvulnerable(true);
                user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 4, false, false));
                invulnerabilityApplied = true;

                // Start spinning sound
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.8f);
            }

            // Increment our local spin counter
            spinTicks++;

            // Continuous spinning attack
            performSpinAttack();

            // Deflect projectiles during spinning
            deflectProjectiles();
        }

        // Remove invulnerability when attack ends
        if (tickCount >= windup + duration - 1 && invulnerabilityApplied) {
            user.setInvulnerable(false);
        }
    }

    private void performSpinAttack() {
        Vec3 userPos = user.position();

        // Create continuous flame circle effect
        createFlameCircle(userPos.add(0, 1, 0), range, 24);

        // Get all enemies in the radius
        List<LivingEntity> targets = getTargetsInCustomHitbox(
                userPos.add(0, user.getBbHeight() / 2, 0),
                range * 2, // width (diameter)
                user.getBbHeight() + 1, // height
                range * 2  // depth (diameter)
        );

        // Hit all targets in range (allowing multiple hits for continuous damage)
        for (LivingEntity target : targets) {
            // For spinning attack, we want to hit enemies multiple times but with reduced damage per hit
            // Check if we haven't hit this enemy recently (every 10 ticks = 0.5 seconds)
            if (spinTicks % 10 == 0 || !hitEntities.contains(target)) {
                // Zero knockback during spin so enemies stay in range for all 4 hits
                float savedKnockback = knockback;
                knockback = 0;
                hitTargetNoImmunity(target);
                knockback = savedKnockback;
                hitEntities.add(target);

                // Individual hit sound
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.4f);
            }
        }

        // Create spinning flame effect every few ticks
        if (spinTicks % 3 == 0) {
            createSpinningFlameEffect();
        }

        // Play whoosh sound every half rotation
        if (spinTicks % 8 == 0) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 1.5f);
        }
    }

    private void deflectProjectiles() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Find all projectiles in range
        List<Projectile> projectiles = world.getEntitiesOfClass(Projectile.class,
                new net.minecraft.world.phys.AABB(userPos.subtract(range, 2, range), userPos.add(range, 2, range)),
                projectile -> projectile.isAlive() && projectile.getOwner() != user);

        for (Projectile projectile : projectiles) {
            // Destroy arrows and similar projectiles with flame
            if (projectile instanceof AbstractArrow) {
                projectile.discard();

                // Create flame deflection effect
                createFlameDeflectionEffect(projectile.position());

                // Deflection sound
                world.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.5f);
            } else {
                // For other projectiles, reflect them back with flame trail
                Vec3 reflectDirection = projectile.position().subtract(userPos).normalize();
                projectile.setDeltaMovement(reflectDirection.scale(1.5));
                projectile.hurtMarked = true;

                // Create flame deflection effect
                createFlameDeflectionEffect(projectile.position());

                // Deflection sound
                world.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(),
                        SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 1.3f);
            }
        }
    }

    /**
     * Create flame deflection effect when destroying/reflecting projectiles
     */
    private void createFlameDeflectionEffect(Vec3 position) {
        if (!(world instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        // Flame burst from deflection
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                position.x, position.y, position.z,
                8, 0.3, 0.3, 0.3, 0.2);

        // Lava particles for impact
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA,
                position.x, position.y, position.z,
                3, 0.2, 0.2, 0.2, 0.1);

        // Spark effect
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                position.x, position.y, position.z,
                5, 0.4, 0.4, 0.4, 0.3);

        // Smoke from deflection
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                position.x, position.y, position.z,
                2, 0.2, 0.2, 0.2, 0.05);
    }

    /**
     * Create spinning flame effect around the user
     */
    private void createSpinningFlameEffect() {
        if (!(world instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Create multiple rings of flames at different heights and speeds
        for (int ring = 0; ring < 3; ring++) {
            float ringRadius = range * (0.5f + ring * 0.3f);
            float ringHeight = ring * 0.7f;
            int particlesInRing = 12 + ring * 4;

            for (int i = 0; i < particlesInRing; i++) {
                // Use thread-safe calculations with our local counter
                double baseAngle = (2 * Math.PI * i) / particlesInRing;
                double spinOffset = (spinTicks * 0.3) * (ring % 2 == 0 ? 1 : -1); // Alternate directions
                double angle = baseAngle + spinOffset;

                double x = userPos.x + Math.cos(angle) * ringRadius;
                double z = userPos.z + Math.sin(angle) * ringRadius;
                double y = userPos.y + ringHeight;

                // Main flame particles
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                        x, y, z, 2, 0.1, 0.1, 0.1, 0.05);

                // Soul fire for inner ring
                if (ring == 0) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                            x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
                }

                // Smoke trails
                if (i % 3 == 0) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                            x, y + 0.5, z, 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
        }

        // Central flame pillar
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                userPos.x, userPos.y, userPos.z, 8, 0.3, 1.0, 0.3, 0.1);
    }

    @Override
    public boolean isOmnidirectional() {
        return true; // This attack hits in all directions
    }

    @Override
    protected void onStop() {
        // Ensure invulnerability is removed
        user.setInvulnerable(false);

        // Final outward knockback burst to all nearby enemies
        if (!world.isClientSide) {
            Vec3 userPos = user.position();
            List<LivingEntity> finalTargets = getTargetsInCustomHitbox(
                    userPos.add(0, user.getBbHeight() / 2, 0),
                    range * 2, user.getBbHeight() + 1, range * 2);
            for (LivingEntity target : finalTargets) {
                Vec3 dir = target.position().subtract(userPos).normalize();
                target.push(dir.x * 0.8, 0.25, dir.z * 0.8);
            }
        }

        // Clear hit entities
        hitEntities.clear();

        // Reset spin counter
        spinTicks = 0;

        // Final flame burst
        createFlameExplosion(user.position().add(0, 1, 0), 1.5f);
        playFlameExplosionSound(user.position());

        // Recovery sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.8f, 1.0f);
    }
}