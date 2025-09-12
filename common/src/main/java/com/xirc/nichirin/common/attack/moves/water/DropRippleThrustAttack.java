package com.xirc.nichirin.common.attack.moves.water;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
 * Seventh Form: Drop Ripple Thrust
 * Barrage-like attack where a single thrust creates a wall of ripples that damage opponents
 * Works as a shield - blocks all attacks during execution
 * Creates multiple hitboxes in front of the user like a defensive wall
 *
 * All configuration comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class DropRippleThrustAttack extends WaterBreathingAttackBase {

    private boolean thrustExecuted = false;
    private boolean shieldActive = false;
    private Set<LivingEntity> hitEntities = new HashSet<>();
    private int rippleTicks = 0;

    public DropRippleThrustAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        thrustExecuted = false;
        shieldActive = false;
        hitEntities.clear();
        rippleTicks = 0;

        // Drop ripple thrust startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.8f, 1.2f);

        // Initial water gathering particles
        createWaterParticles();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Execute thrust after windup
        if (!thrustExecuted && tickCount == windup + 1) {
            executeThrust();
            thrustExecuted = true;
        }

        // Maintain shield and ripples during duration
        if (thrustExecuted && tickCount > windup && tickCount < windup + duration) {
            rippleTicks++;
            maintainRipplesAndShield();
        }
    }

    private void executeThrust() {
        // Activate defensive shield
        activateShield();
        shieldActive = true;

        // Create initial thrust effect
        createThrustEffect();

        // Execute initial thrust damage
        performInitialThrust();

        // Thrust sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.2f, 0.9f);
    }

    private void activateShield() {
        // Grant damage resistance during active phase
        int shieldDuration = duration;
        user.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                shieldDuration,
                3, // Amplifier 3 (80% damage reduction)
                false, // Not ambient
                true   // Show particles
        ));

        // Also grant knockback resistance
        user.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                shieldDuration,
                255, // Max slowness (can't move but can't be knocked back)
                false, // Not ambient
                false  // Don't show particles (too much visual noise)
        ));
    }

    private void performInitialThrust() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Hit enemies in thrust line
        List<LivingEntity> thrustTargets = getTargetsInRangeLine(1.0f);

        for (LivingEntity target : thrustTargets) {
            hitTarget(target);
            hitEntities.add(target);

            // Forward knockback
            Vec3 thrustKnockback = lookDir.scale(knockback * 0.8);
            target.push(thrustKnockback.x, 0.2, thrustKnockback.z);

            // Individual thrust hit sound
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.8f, 1.3f);
        }
    }

    private void maintainRipplesAndShield() {
        // Deflect projectiles (shield function)
        deflectProjectiles();

        // Create continuous ripple wall effects
        createRippleWallEffect();

        // Hit enemies that enter the ripple wall every few ticks
        if (rippleTicks % 8 == 0) {
            hitEnemiesInRipples();
        }

        // Shield particle effects
        if (rippleTicks % 5 == 0) {
            createShieldEffect();
        }

        // Ripple sound every few ticks
        if (rippleTicks % 12 == 0) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 0.6f, 1.1f + rippleTicks * 0.02f);
        }
    }

    private void deflectProjectiles() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create shield area in front of user
        Vec3 shieldCenter = userPos.add(lookDir.scale(2.0));

        // Find all projectiles in shield area
        List<Projectile> projectiles = world.getEntitiesOfClass(Projectile.class,
                new net.minecraft.world.phys.AABB(
                        shieldCenter.subtract(2, 2, 2),
                        shieldCenter.add(2, 2, 2)
                ),
                projectile -> projectile.isAlive() && projectile.getOwner() != user);

        for (Projectile projectile : projectiles) {
            // Destroy arrows and similar projectiles with water
            if (projectile instanceof AbstractArrow) {
                projectile.discard();

                // Create water deflection effect
                createProjectileDeflectionEffect(projectile.position());

                // Deflection sound
                world.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(),
                        SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 1.0f, 1.4f);
            } else {
                // For other projectiles, reflect them back
                Vec3 deflectDirection = projectile.position().subtract(userPos).normalize().scale(-1);
                projectile.setDeltaMovement(deflectDirection.scale(1.5));
                projectile.hurtMarked = true;

                // Create water deflection effect
                createProjectileDeflectionEffect(projectile.position());

                // Deflection sound
                world.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(),
                        SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.8f, 1.3f);
            }
        }
    }

    private void createThrustEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create initial thrust line
        for (int i = 1; i <= 5; i++) {
            Vec3 thrustPos = userPos.add(lookDir.scale(i * (range / 5)));

            // Main thrust particles
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    thrustPos.x, thrustPos.y, thrustPos.z,
                    6, 0.3, 0.3, 0.3, 0.15);

            // Crit particles for thrust effect
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    thrustPos.x, thrustPos.y, thrustPos.z,
                    3, 0.15, 0.15, 0.15, 0.1);
        }
    }

    private void createRippleWallEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();

        // Create wall of ripples in front of user
        for (int depth = 1; depth <= 3; depth++) {
            for (int width = -2; width <= 2; width++) {
                for (int height = -1; height <= 2; height++) {
                    Vec3 ripplePos = userPos
                            .add(lookDir.scale(depth * 1.5))
                            .add(rightDir.scale(width * 0.8))
                            .add(0, height * 0.7, 0);

                    // Ripple wave effect
                    double wave = Math.sin(rippleTicks * 0.3 + depth + width + height) * 0.3;
                    Vec3 wavePos = ripplePos.add(0, wave, 0);

                    // Main ripple particles
                    serverLevel.sendParticles(ParticleTypes.SPLASH,
                            wavePos.x, wavePos.y, wavePos.z,
                            2, 0.1, 0.1, 0.1, 0.08);

                    // Occasional dripping water
                    if (rippleTicks % 6 == 0 && width % 2 == 0) {
                        serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                                wavePos.x, wavePos.y + 0.5, wavePos.z,
                                1, 0.05, 0, 0.05, 0.02);
                    }
                }
            }
        }
    }

    private void hitEnemiesInRipples() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Hit enemies in the ripple wall area
        List<LivingEntity> rippleTargets = getTargetsInCustomHitbox(
                userPos.add(user.getLookAngle().scale(range * 0.6)),
                hitboxSize, // width
                3.0, // height (tall wall)
                2.0  // depth (thick wall)
        );

        for (LivingEntity target : rippleTargets) {
            // Allow re-hitting but with spacing
            if (!hasRecentlyHit(target)) {
                hitTargetNoImmunity(target);
                trackRecentHit(target);

                // Light knockback to push through ripples
                Vec3 rippleKnockback = user.getLookAngle().scale(knockback * 0.4);
                target.push(rippleKnockback.x, 0.05, rippleKnockback.z);

                // Ripple hit sound
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.5f, 1.4f);

                // Create ripple impact effect
                createRippleImpactEffect(target.position());
            }
        }
    }

    private boolean hasRecentlyHit(LivingEntity target) {
        // Allow hitting the same target again after 16 ticks (0.8 seconds)
        return hitEntities.contains(target) && rippleTicks % 16 > 8;
    }

    private void trackRecentHit(LivingEntity target) {
        hitEntities.add(target);
        // Clear old hits periodically
        if (rippleTicks % 32 == 0) {
            hitEntities.clear();
        }
    }

    private void createShieldEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Defensive aura around user
        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * 2 * Math.PI + rippleTicks * 0.1;
            double radius = 1.5;

            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + Math.sin(angle * 2) * 0.3;

            // Shield particles
            serverLevel.sendParticles(ParticleTypes.BUBBLE,
                    x, y, z, 1, 0.05, 0.05, 0.05, 0.02);

            if (i % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    private void createProjectileDeflectionEffect(Vec3 position) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Water burst from deflection
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                position.x, position.y, position.z,
                12, 0.4, 0.4, 0.4, 0.2);

        // Bubble burst
        serverLevel.sendParticles(ParticleTypes.BUBBLE,
                position.x, position.y, position.z,
                8, 0.3, 0.3, 0.3, 0.15);

        // Crit effect for deflection
        serverLevel.sendParticles(ParticleTypes.CRIT,
                position.x, position.y, position.z,
                5, 0.2, 0.2, 0.2, 0.1);
    }

    private void createRippleImpactEffect(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 targetPos = impactPos.add(0, 1, 0);

        // Ripple impact burst
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                targetPos.x, targetPos.y, targetPos.z,
                10, 0.3, 0.3, 0.3, 0.15);

        // Ripple rings
        for (int ring = 1; ring <= 2; ring++) {
            float ringRadius = ring * 0.8f;
            int particlesInRing = 6 * ring;

            for (int i = 0; i < particlesInRing; i++) {
                double angle = (2 * Math.PI * i) / particlesInRing;
                double x = targetPos.x + Math.cos(angle) * ringRadius;
                double z = targetPos.z + Math.sin(angle) * ringRadius;
                double y = targetPos.y;

                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    @Override
    public boolean hasDefensiveProperties() {
        return true; // This attack blocks incoming damage
    }

    @Override
    protected void onStop() {
        // Remove shield effects
        user.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        user.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

        // Clear state
        hitEntities.clear();
        rippleTicks = 0;
        thrustExecuted = false;
        shieldActive = false;

        // Final ripple dissolution effect
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

            // Final ripple wall collapse
            createWaterExplosion(userPos.add(user.getLookAngle().scale(2)), 1.5f);

            // Peaceful water rain
            for (int i = 0; i < 25; i++) {
                double x = userPos.x + (Math.random() - 0.5) * 4;
                double z = userPos.z + (Math.random() - 0.5) * 4;
                double y = userPos.y + 3 + Math.random() * 2;

                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        x, y, z, 1, 0, -0.3, 0, 0.1);
            }
        }

        // Final shield dissolution sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
    }
}