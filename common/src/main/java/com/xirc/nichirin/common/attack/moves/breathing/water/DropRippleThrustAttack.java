package com.xirc.nichirin.common.attack.moves.breathing.water;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import com.xirc.nichirin.common.vfx.VfxIds;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.phys.AABB;

/**
 * Seventh Form: Drop Ripple Thrust
 * Barrage-like attack where a single thrust creates a wall of ripples that damage opponents
 * Works as a shield - blocks all attacks during execution
 * Creates multiple hitboxes in front of the user like a defensive wall
 */
public class DropRippleThrustAttack extends WaterBreathingAttackBase {

    private boolean thrustExecuted = false;
    private boolean shieldActive = false;
    private Set<LivingEntity> hitEntities = new HashSet<>();
    private int rippleTicks = 0;

    public DropRippleThrustAttack() {
    }

    @Override
    protected void onStart() {
        thrustExecuted = false;
        shieldActive = false;
        hitEntities.clear();
        rippleTicks = 0;
    }

    @Override
    protected void onActiveStart() {
        // Drop ripple thrust startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.8f, 1.2f);
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
        playWaterVfx(VfxIds.DROP_RIPPLE_THRUST,
                user.position().add(user.getLookAngle().normalize().scale(1.15))
                        .add(0, user.getBbHeight() * 0.45, 0), user.getLookAngle(), 1.0f);
        // Activate defensive shield
        activateShield();
        shieldActive = true;

        // Create initial thrust effect

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
                false  // VFX are rendered by the water VFX engine
        ));

        // Also grant knockback resistance
        user.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                shieldDuration,
                255, // Max slowness (can't move but can't be knocked back)
                false, // Not ambient
                false  // VFX are rendered by the water VFX engine
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

            // Minimal knockback on first hit so subsequent ripple hits still connect
            Vec3 thrustKnockback = lookDir.scale(0.05f);
            target.push(thrustKnockback.x, 0.05, thrustKnockback.z);

            // Individual thrust hit sound
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.8f, 1.3f);
        }
    }

    private void maintainRipplesAndShield() {
        // Deflect projectiles (shield function)
        deflectProjectiles();

        // Create continuous ripple wall effects

        // Hit enemies that enter the ripple wall every few ticks
        if (rippleTicks % 8 == 0) {
            hitEnemiesInRipples();
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
                new AABB(
                        shieldCenter.subtract(2, 2, 2),
                        shieldCenter.add(2, 2, 2)
                ),
                projectile -> projectile.isAlive() && projectile.getOwner() != user);

        for (Projectile projectile : projectiles) {
            // Destroy arrows and similar projectiles with water
            if (projectile instanceof AbstractArrow) {
                projectile.discard();

                // Create water deflection effect

                // Deflection sound
                world.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(),
                        SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 1.0f, 1.4f);
            } else {
                // For other projectiles, reflect them back
                Vec3 deflectDirection = projectile.position().subtract(userPos).normalize().scale(-1);
                projectile.setDeltaMovement(deflectDirection.scale(1.5));
                projectile.hurtMarked = true;

                // Create water deflection effect

                // Deflection sound
                world.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(),
                        SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.8f, 1.3f);
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

                // Ripple hit sound
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.5f, 1.4f);

                // Create ripple impact effect
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

        // Final shield dissolution sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
    }
}
