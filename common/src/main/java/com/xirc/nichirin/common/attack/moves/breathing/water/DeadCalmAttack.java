package com.xirc.nichirin.common.attack.moves.breathing.water;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Eleventh Form: Dead Calm
 * Plain multihit auto-target AoE
 * Larger AoE that procs a series of slashes whenever someone steps into it
 * Creates a persistent area effect field that triggers on entity entry
 *
 * All configuration comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class DeadCalmAttack extends WaterBreathingAttackBase {

    private boolean fieldActive = false;
    private Vec3 fieldCenter;
    private final Set<LivingEntity> entitiesInField = new HashSet<>();
    private final Set<LivingEntity> recentlyTriggered = new HashSet<>();
    private int calmTicks = 0;

    public DeadCalmAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        fieldActive = false;
        fieldCenter = null;
        entitiesInField.clear();
        recentlyTriggered.clear();
        calmTicks = 0;

        // Dead calm startup sound - very quiet and ominous
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 0.6f, 0.5f);

        // Create initial calm water effect
        createCalmWaterGathering();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Establish the calm field after windup
        if (!fieldActive && tickCount == windup + 1) {
            establishCalmField();
            fieldActive = true;
        }

        // Maintain the persistent area effect during duration
        if (fieldActive && tickCount > windup && tickCount < windup + duration) {
            calmTicks++;
            maintainCalmField();
        }
    }

    private void createCalmWaterGathering() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Calm, still water gathering around user
        for (int ring = 1; ring <= 12; ring++) {
            float radius = ring * 1.2f;
            int particlesInRing = 8 * ring;

            for (int i = 0; i < particlesInRing; i++) {
                double angle = (2 * Math.PI * i) / particlesInRing;
                double x = userPos.x + Math.cos(angle) * radius;
                double z = userPos.z + Math.sin(angle) * radius;
                double y = userPos.y + Math.sin(angle * 4) * 0.1; // Very subtle waves

                // Calm water particles
                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        x, y, z, 1, 0.05, 0.05, 0.05, 0.01);

                if (ring <= 2 && i % 3 == 0) {
                    serverLevel.sendParticles(ParticleTypes.BUBBLE,
                            x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
                }
            }
        }
    }

    private void establishCalmField() {
        applySlowdown();

        // Set field center slightly in front of user
        Vec3 lookDir = user.getLookAngle();
        fieldCenter = user.position().add(lookDir.scale(2.0));

        // Create initial field establishment effect
        createFieldEstablishmentEffect();

        // Calm field activation sound
        world.playSound(null, fieldCenter.x, fieldCenter.y, fieldCenter.z,
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 0.8f);
    }
    private void applySlowdown() {
        int slowDuration = 100;
        user.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                slowDuration,
                255, // Max slowness (can't move but can't be knocked back)
                false, // Not ambient
                false  // Don't show particles (too much visual noise)
        ));
    }

    private void maintainCalmField() {
        if (fieldCenter == null) return;

        // Create persistent calm water field visual
        createPersistentFieldEffect();

        // Check for entities entering/leaving the field
        updateEntitiesInField();

        // Trigger slashes for entities that enter the field
        triggerAutoSlashes();

        // Ambient field sound occasionally
        if (calmTicks % 40 == 0) {
            world.playSound(null, fieldCenter.x, fieldCenter.y, fieldCenter.z,
                    SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 0.4f, 0.7f);
        }
    }

    private void updateEntitiesInField() {
        // Get all entities currently in the field area
        AABB fieldArea = new AABB(
                fieldCenter.x - range, fieldCenter.y - 2, fieldCenter.z - range,
                fieldCenter.x + range, fieldCenter.y + 4, fieldCenter.z + range
        );

        List<LivingEntity> currentEntities = world.getEntitiesOfClass(LivingEntity.class, fieldArea,
                entity -> entity != user && entity.isAlive() && !entity.isSpectator());

        // Find entities that just entered the field
        Set<LivingEntity> newEntrants = new HashSet<>(currentEntities);
        newEntrants.removeAll(entitiesInField);

        // Trigger slashes for new entrants
        for (LivingEntity newEntrant : newEntrants) {
            if (!recentlyTriggered.contains(newEntrant)) {
                triggerSlashSequence(newEntrant);
                recentlyTriggered.add(newEntrant);
            }
        }

        // Update the set of entities in field
        entitiesInField.clear();
        entitiesInField.addAll(currentEntities);

        // Clear recently triggered entities periodically to allow re-triggering
        if (calmTicks % 10 == 0) {
            recentlyTriggered.clear();
        }
    }

    private void triggerAutoSlashes() {
        // Also trigger slashes for entities that remain in the field (every 20 ticks = 1 second)
        if (calmTicks % 2 == 0) {
            for (LivingEntity entity : entitiesInField) {
                if (!recentlyTriggered.contains(entity)) {
                    triggerSlashSequence(entity);
                    recentlyTriggered.add(entity);
                }
            }
        }
    }

    private void triggerSlashSequence(LivingEntity target) {
        // Auto-target slash sequence - 3 rapid slashes
        Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);

        for (int slash = 0; slash < 3; slash++) {
            // Schedule slashes with small delays
            scheduleSlash(target, targetPos, slash * 4); // 4 tick intervals
        }

        // Trigger sound
        world.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    private void scheduleSlash(LivingEntity target, Vec3 targetPos, int delay) {
        // Since we can't actually schedule delayed actions easily, we'll execute all slashes immediately
        // but with different visual patterns

        // Hit the target immediately
        hitTargetNoImmunity(target);

        // Light knockback toward field center to keep them in the danger zone
        Vec3 centerDirection = fieldCenter.subtract(target.position()).normalize();
        target.push(centerDirection.x * knockback * 0.2, 0.01, centerDirection.z * knockback * 0.2);

        // Create slash effect at target position
        createAutoSlashEffect(targetPos, delay);

        // Individual slash sound
        world.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.6f, 1.5f + delay * 0.1f);
    }

    private void createFieldEstablishmentEffect() {
        if (!(world instanceof ServerLevel serverLevel) || fieldCenter == null) return;

        // Large expanding ring to show field creation
        for (int ring = 1; ring <= 5; ring++) {
            float ringRadius = ring * (range / 5);
            int particlesInRing = 12 * ring;

            for (int i = 0; i < particlesInRing; i++) {
                double angle = (2 * Math.PI * i) / particlesInRing;
                double x = fieldCenter.x + Math.cos(angle) * ringRadius;
                double z = fieldCenter.z + Math.sin(angle) * ringRadius;
                double y = fieldCenter.y + 0.5;

                // Field boundary particles
                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        x, y, z, 2, 0.1, 0.1, 0.1, 0.05);

                if (ring == 5) { // Outer ring gets special effect
                    serverLevel.sendParticles(ParticleTypes.BUBBLE,
                            x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
                }
            }
        }

        // Central field pillar
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                fieldCenter.x, fieldCenter.y + 1, fieldCenter.z,
                20, 0.5, 2.0, 0.5, 0.2);
    }

    private void createPersistentFieldEffect() {
        if (!(world instanceof ServerLevel serverLevel) || fieldCenter == null) return;

        // Gentle, continuous field visualization
        if (calmTicks % 8 == 0) {
            // Field boundary ring
            int boundaryParticles = 24;
            for (int i = 0; i < boundaryParticles; i++) {
                double angle = (2 * Math.PI * i) / boundaryParticles + calmTicks * 0.02;
                double x = fieldCenter.x + Math.cos(angle) * range * 0.9;
                double z = fieldCenter.z + Math.sin(angle) * range * 0.9;
                double y = fieldCenter.y + Math.sin(angle * 3) * 0.2;

                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }

        // Occasional bubbles rising in the field
        if (calmTicks % 12 == 0) {
            for (int i = 0; i < 6; i++) {
                double x = fieldCenter.x + (Math.random() - 0.5) * range * 1.5;
                double z = fieldCenter.z + (Math.random() - 0.5) * range * 1.5;
                double y = fieldCenter.y;

                serverLevel.sendParticles(ParticleTypes.BUBBLE,
                        x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
            }
        }

        // Central calm effect
        if (calmTicks % 20 == 0) {
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    fieldCenter.x, fieldCenter.y + 0.5, fieldCenter.z,
                    4, 0.3, 0.3, 0.3, 0.08);
        }
    }

    private void createAutoSlashEffect(Vec3 slashPos, int slashIndex) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Auto-slash visual effect - different angles for each slash
        for (int i = -2; i <= 2; i++) {
            double angle = i * 30 + slashIndex * 60; // Different angle per slash
            double radians = Math.toRadians(angle);

            Vec3 slashDir = new Vec3(Math.cos(radians), 0, Math.sin(radians));
            Vec3 particlePos = slashPos.add(slashDir.scale(1.5));

            // Auto-slash particles
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    particlePos.x, particlePos.y, particlePos.z,
                    4, 0.3, 0.3, 0.3, 0.15);

            if (Math.abs(i) <= 1) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        particlePos.x, particlePos.y, particlePos.z,
                        2, 0.15, 0.15, 0.15, 0.1);
            }
        }

        // Center impact
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                slashPos.x, slashPos.y, slashPos.z,
                8, 0.4, 0.4, 0.4, 0.2);
    }

    @Override
    public boolean isPersistentArea() {
        return true; // This creates a persistent area effect
    }

    @Override
    protected void onStop() {
        // Field dissolution effect
        if (world instanceof ServerLevel serverLevel && fieldCenter != null) {
            // Final field collapse
            for (int ring = 5; ring >= 1; ring--) {
                float ringRadius = ring * (range / 5);
                int particlesInRing = 8 * ring;

                for (int i = 0; i < particlesInRing; i++) {
                    double angle = (2 * Math.PI * i) / particlesInRing;
                    double x = fieldCenter.x + Math.cos(angle) * ringRadius;
                    double z = fieldCenter.z + Math.sin(angle) * ringRadius;
                    double y = fieldCenter.y + 0.5;

                    serverLevel.sendParticles(ParticleTypes.SPLASH,
                            x, y, z, 3, 0.3, 0.3, 0.3, 0.15);
                }
            }

            // Final calm center effect
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    fieldCenter.x, fieldCenter.y + 1, fieldCenter.z,
                    30, 1.0, 2.0, 1.0, 0.3);
        }

        // Final calm dissolution sound
        world.playSound(null, fieldCenter != null ? fieldCenter.x : user.getX(),
                fieldCenter != null ? fieldCenter.y : user.getY(),
                fieldCenter != null ? fieldCenter.z : user.getZ(),
                SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.6f);

        // Clear state
        entitiesInField.clear();
        recentlyTriggered.clear();
        fieldActive = false;
        fieldCenter = null;
        calmTicks = 0;
    }
}