package com.xirc.nichirin.common.attack.moves.water;

import com.xirc.nichirin.common.attack.moves.thunder.ThunderBreathingAttackBase;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fifth Form: Heat Lightning
 * Performs an upward slash in the direction the user is looking
 * Then strikes airborne targets with lightning
 *
 * All configuration now comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class WaterSurfaceSlashAttack extends ThunderBreathingAttackBase {

    private final Set<LivingEntity> hitEntities = new HashSet<>();
    private float launchPower = 1.125f; // 75% of original 1.5f
    private boolean lightningStruck = false; // Track if lightning has been summoned

    public WaterSurfaceSlashAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        hitEntities.clear();
        lightningStruck = false;

        // Rising slash sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Perform the rising slash on first tick
        if (tickCount == windup + 1) {
            performRisingSlash();
        }

        // ✅ FIXED: Use range instead of exact tick to account for thread timing differences
        if (tickCount >= windup + 20 && tickCount <= windup + 30 && !lightningStruck && !hitEntities.isEmpty()) {
            strikeAllTargetsWithLightning();
            lightningStruck = true;
        }

        // Add debug info to see what's happening
        if (tickCount % 5 == 0) {
        }

        // Stop attack after lightning has struck or if no targets were hit
        if ((lightningStruck && tickCount > windup + 30) || (hitEntities.isEmpty() && tickCount > windup + 30)) {
            stop();
        }
    }

    private void performRisingSlash() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create slash effect in the direction the user is looking
        Vec3 slashDirection = lookDir;
        Vec3 slashBase = userPos.add(lookDir.scale(2.0));

        // Create visual effect - vertical slash
        if (world instanceof ServerLevel serverLevel) {
            float slashHeight = 4.0f;

            // Vertical line of particles in look direction
            for (int i = 0; i <= 10; i++) {
                float progress = i / 10.0f;
                Vec3 particlePos = slashBase.add(0, progress * slashHeight, 0);

                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0, 0, 0, 0);

                if (i % 2 == 0) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            particlePos.x, particlePos.y, particlePos.z,
                            5, 0.2, 0.2, 0.2, 0.05);
                }
            }
        }

        // Thunder sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4f, 2.5f);

        // Hit entities in the attack area
        hitEntitiesInSlashArea(slashBase, slashDirection);
    }

    private void hitEntitiesInSlashArea(Vec3 slashBase, Vec3 slashDirection) {
        // Define the attack area
        AABB attackArea = new AABB(
                slashBase.x - range, slashBase.y, slashBase.z - range,
                slashBase.x + range, slashBase.y + 4.0, slashBase.z + range
        );

        List<LivingEntity> entitiesInArea = world.getEntitiesOfClass(LivingEntity.class, attackArea,
                entity -> entity != user && entity.isAlive() && !entity.isSpectator());

        for (LivingEntity target : entitiesInArea) {
            // Check if entity is within the slash area and in front of the user
            Vec3 toEntity = target.position().subtract(user.position()).normalize();
            double dot = slashDirection.dot(toEntity);

            // Only hit entities that are reasonably in front of the user
            if (dot > 0.3) {
                // Create armor-bypassing damage source (using configured damage)
                DamageSource armorPiercingSource = user.damageSources().magic();
                target.hurt(armorPiercingSource, damage);

                // Launch the target
                launchTarget(target);

                hitEntities.add(target);

            }
        }
    }

    private void launchTarget(LivingEntity target) {
        // Clear existing velocity
        target.setDeltaMovement(Vec3.ZERO);

        // Lift off ground
        if (target.onGround()) {
            target.setPos(target.getX(), target.getY() + 0.1, target.getZ());
        }

        // Apply upward velocity
        Vec3 launchVelocity = new Vec3(0, launchPower, 0);
        target.setDeltaMovement(launchVelocity);
        target.hurtMarked = true;
        target.hasImpulse = true;

        // Force sync for players
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));
        }

    }

    /**
     * ✅ NEW METHOD: Strike all hit targets with lightning at once
     */
    private void strikeAllTargetsWithLightning() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        for (LivingEntity target : hitEntities) {
            // Get target position (even if dead, we can still get last known position)
            Vec3 targetPos = target.position();

            // Create real lightning bolt at target position
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
            if (lightning != null) {
                lightning.moveTo(targetPos);
                lightning.setCause((ServerPlayer) user);

                // ✅ FIXED: Don't set visual only - let it be a real lightning bolt
                // lightning.setVisualOnly(true);  // ← REMOVED THIS LINE

                serverLevel.addFreshEntity(lightning);
            }

            // Apply our own controlled lightning damage (if still alive)
            if (target.isAlive()) {
                applyLightningDamage(target);

                // Apply the shocked effect after lightning hits
                target.addEffect(new MobEffectInstance(
                        NichirinEffectRegistry.SHOCKED.get(),
                        hitStun,
                        0,
                        false,
                        true
                ));
            } else {
            }
        }
    }

    /**
     * ✅ NEW METHOD: Applies controlled lightning damage to only the intended target
     */
    private void applyLightningDamage(LivingEntity target) {
        // Lightning damage (magic damage to bypass armor - using configured damage)
        DamageSource lightningSource = user.damageSources().magic();
        target.hurt(lightningSource, damage * 0.5f);

        // Additional lightning particles at impact
        if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    40, 0.8, 0.8, 0.8, 0.3);
        }

        // Set the target on fire briefly (like real lightning)
        target.setSecondsOnFire(3);

    }

    @Override
    protected void onStop() {
        hitEntities.clear();
        lightningStruck = false;
    }
}