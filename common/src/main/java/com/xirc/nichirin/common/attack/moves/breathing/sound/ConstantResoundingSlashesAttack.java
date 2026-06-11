package com.xirc.nichirin.common.attack.moves.breathing.sound;

import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;

/**
 * Fourth Form: Constant Resounding Slashes
 * The user holds their twin swords apart and spins them rapidly around themselves,
 * enabling the blades to destroy anything it makes contact with for powerful defensive capabilities.
 *
 * Mechanics:
 * - 360° hitbox, 3.5 block radius
 * - Deals rapid multi-hit damage (small hits stacking into big DPS)
 * - Reflects or destroys projectiles during active frames
 * - 1.5s duration, movement speed slowed by ~20% while active
 */
public class ConstantResoundingSlashesAttack extends SoundBreathingAttackBase {

    private boolean isSpinning = false;
    private final Set<LivingEntity> hitThisCycle = new HashSet<>();
    private int spinCycle = 0;
    private final float originalSpeed = 0.1f; // Store original movement speed

    public ConstantResoundingSlashesAttack() {
    }

    @Override
    protected void onStart() {
        isSpinning = false;
        hitThisCycle.clear();
        spinCycle = 0;

        // Slow down user movement
        if (user != null) {
            user.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    duration,
                    0, // 20% slower
                    false,
                    false
            ));
        }
    }

    @Override
    protected void onActiveStart() {
        // Initial spinning sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.0f, 2.0f);

        // Create initial particle ring - only show SOUND particles during move duration
        createSoundParticles();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Start spinning after windup
        if (!isSpinning && tickCount == windup + 1) {
            isSpinning = true;
            startSpinSound();
        }

        // Continue spinning during duration
        if (isSpinning && tickCount > windup && tickCount < windup + duration) {
            performSpinningSlashes();
            deflectProjectiles();

            // Reset hit tracking every 2 ticks for more multi-hits
            hitThisCycle.clear();
            spinCycle++;
        }
    }

    private void startSpinSound() {
        // Continuous spinning sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.2f, 1.5f);
    }

    private void performSpinningSlashes() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Create spinning particle effect
        createSpinningBladeEffect(userPos);

        // Hit all enemies in 360° radius
        List<LivingEntity> targets = getTargetsInCustomHitbox(userPos, range, 2.0, range);

        for (LivingEntity target : targets) {
            // Only hit each target once per cycle to prevent spam
            if (!hitThisCycle.contains(target)) {
                // Reduced damage per hit since it's multi-hit
                float originalDamage = damage;
                damage = damage * 0.25f; // Reduced to 25% for more frequent hits

                hitTargetNoImmunity(target); // Use no immunity for multi-hit

                // Apply disoriented effect
                applyDisorientedEffect(target);

                damage = originalDamage; // Restore original damage

                // Light knockback to keep enemies in range
                Vec3 lightKnockback = target.position().subtract(userPos).normalize().scale(knockback * 0.2f);
                target.push(lightKnockback.x, 0.05, lightKnockback.z);

                hitThisCycle.add(target);

                // Individual hit sound
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4f, 1.8f);
            }
        }

        // Spinning blade sound every few ticks
        if (tickCount % 8 == 0) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 0.6f, 2.0f);
        }
    }

    private void deflectProjectiles() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Find all projectiles in range
        List<Projectile> projectiles = world.getEntitiesOfClass(Projectile.class,
                new AABB(userPos.subtract(range, 2, range), userPos.add(range, 2, range)),
                projectile -> projectile.isAlive() && projectile.getOwner() != user);

        for (Projectile projectile : projectiles) {
            // Destroy arrows and similar projectiles
            if (projectile instanceof AbstractArrow) {
                projectile.discard();

                // Create deflection effect
                createDeflectionEffect(projectile.position());

                // Deflection sound
                world.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.5f);
            } else {
                // For other projectiles, reflect them back
                Vec3 reflectDirection = projectile.position().subtract(userPos).normalize();
                projectile.setDeltaMovement(reflectDirection.scale(1.5));
                projectile.hurtMarked = true;

                // Change owner to user if possible
                projectile.getOwner();// Can't easily change owner, so just redirect

                createDeflectionEffect(projectile.position());
            }
        }
    }

    private void createSpinningBladeEffect(Vec3 center) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Calculate spin angle based on tick count for smooth rotation
        double spinAngle = (tickCount * 0.5) % (2 * Math.PI);

        // Create blade trails in a cross pattern - only show SOUND particles during move duration
        for (int blade = 0; blade < 4; blade++) { // 4 blades for twin swords
            double bladeAngle = spinAngle + (blade * Math.PI / 2);

            // Each blade extends from center to range
            for (double r = 2.0; r <= range; r += 1.5) {
                double x = center.x + Math.cos(bladeAngle) * r;
                double z = center.z + Math.sin(bladeAngle) * r;
                double y = center.y + Math.sin(r * 2) * 0.3; // Slight wave motion

                // Sound particles for blade trail - only during move duration
                if (r % 3.0 < 1.5 && tickCount < windup + duration) {
                    serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                            x, y, z, 1, 0.3, 0.3, 0.3, 0.05);
                }
            }
        }

        // Central spinning effect
        serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                center.x, center.y, center.z, 1, 0.4, 0.4, 0.4, 0.08);

        // Outer ring effect - only show SOUND particles during move duration
        int ringParticles = 6;
        for (int i = 0; i < ringParticles; i++) {
            double angle = (i / (double)ringParticles) * 2 * Math.PI + spinAngle;
            double x = center.x + Math.cos(angle) * range;
            double z = center.z + Math.sin(angle) * range;

            serverLevel.sendParticles(NichirinParticleRegistry.FLASH2.get(),
                    x, center.y, z, 1, 0.3, 0.3, 0.3, 0.05);
        }
    }

    private void createDeflectionEffect(Vec3 position) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Deflection burst
        serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                position.x, position.y, position.z,
                8, 0.3, 0.3, 0.3, 0.2);

        serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                position.x, position.y, position.z,
                5, 0.2, 0.2, 0.2, 0.1);

        // Spark effect
        serverLevel.sendParticles(ParticleTypes.CRIT,
                position.x, position.y, position.z,
                10, 0.5, 0.5, 0.5, 0.3);
    }

    @Override
    public boolean isOmnidirectional() {
        return true; // This hits 360 degrees
    }

    @Override
    protected void onStop() {
        // Reset state
        isSpinning = false;
        hitThisCycle.clear();
        spinCycle = 0;

        // Final spinning down sound
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.8f, 0.5f);
        }

        // Final particle burst - only show SOUND particles during move duration
        if (world instanceof ServerLevel serverLevel && user != null) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

            // Final shockwave ring
            createSoundShockwave(userPos, range * 1.2f, 8);

            // Upward particle burst - only show SOUND particles if within duration
            if (tickCount < windup + duration) {
                serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                        userPos.x, userPos.y, userPos.z,
                        4, range * 0.4, 1.5, range * 0.4, 0.15);
            }
        }
    }
}