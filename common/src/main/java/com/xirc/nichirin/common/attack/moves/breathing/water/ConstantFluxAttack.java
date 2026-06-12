package com.xirc.nichirin.common.attack.moves.breathing.water;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tenth Form: Constant Flux
 * Fast combo ending in a powerful aoe finisher with the appearance of a water dragon
 * 5 piece combo + finisher that drags opponents forward into the final big hit
 * Weakest ultimate but still very strong
 */
public class ConstantFluxAttack extends WaterBreathingAttackBase {

    private static final int COMBO_HITS = 5; // 5-piece combo
    private static final int HIT_INTERVAL = 8; // Ticks between each combo hit

    private int comboHitsExecuted = 0;
    private int nextHitTick = 0;
    private boolean finisherExecuted = false;
    private final Map<LivingEntity, DragData> draggedEntities = new HashMap<>();
    private final List<LivingEntity> comboTargets = new ArrayList<>();
    private int fluxTicks = 0;

    // Data class to track dragged entities
    private static class DragData {
        Vec3 startPosition;
        int dragTicks;

        DragData(Vec3 startPos) {
            this.startPosition = startPos;
            this.dragTicks = 0;
        }
    }

    public ConstantFluxAttack() {
    }

    @Override
    protected void onStart() {
        comboHitsExecuted = 0;
        nextHitTick = 0;
        finisherExecuted = false;
        draggedEntities.clear();
        comboTargets.clear();
        fluxTicks = 0;
    }

    @Override
    protected void onActiveStart() {
        // Constant flux startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.1f);

        // Create initial flux buildup
        createFluxStartEffect();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        fluxTicks++;

        // Execute 5-piece combo
        if (comboHitsExecuted < COMBO_HITS && tickCount >= windup + nextHitTick) {
            executeComboHit();
            comboHitsExecuted++;
            nextHitTick += HIT_INTERVAL;
        }

        // Maintain drag effects during combo
        if (comboHitsExecuted > 0 && !finisherExecuted) {
            maintainDragEffects();
        }

        // Execute water dragon finisher after all combo hits
        if (!finisherExecuted && comboHitsExecuted >= COMBO_HITS &&
                tickCount >= windup + (COMBO_HITS * HIT_INTERVAL) + 10) {
            executeWaterDragonFinisher();
            finisherExecuted = true;
        }
    }

    private void executeComboHit() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Hit enemies in combo range
        List<LivingEntity> hitTargets = getTargetsAtRange();

        for (LivingEntity target : hitTargets) {
            // Combo damage - moderate per hit
            float originalDamage = damage;
            damage = damage * 0.6f; // 60% damage per combo hit
            hitTarget(target);
            damage = originalDamage;

            // Add to combo targets and start dragging
            if (!comboTargets.contains(target)) {
                comboTargets.add(target);
                draggedEntities.put(target, new DragData(target.position()));
            }

            // Very light knockback - we want to drag them closer
            Vec3 comboKnockback = user.getLookAngle().scale(knockback * 0.1);
            target.push(comboKnockback.x, 0.02, comboKnockback.z);

            createComboHitEffect(target.position());
        }

        // Create combo slash visual
        createComboSlashEffect(userPos, comboHitsExecuted);

// Combo hit sounds with increasing intensity
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                0.8f, 1.0f + comboHitsExecuted * 0.15f);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS,
                0.6f, 1.2f + comboHitsExecuted * 0.1f);

// Small forward boost when grounded
        if (user.onGround()) {
            Vec3 lookDir = user.getLookAngle().normalize();
            double boostStrength = 1;
            user.setDeltaMovement(user.getDeltaMovement().add(
                    lookDir.x * boostStrength,
                    0,
                    lookDir.z * boostStrength
            ));
            user.hurtMarked = true;
        }

    }

    private void maintainDragEffects() {
        Vec3 userPos = user.position();
        List<LivingEntity> toRemove = new ArrayList<>();

        for (Map.Entry<LivingEntity, DragData> entry : draggedEntities.entrySet()) {
            LivingEntity entity = entry.getKey();
            DragData dragData = entry.getValue();

            if (!entity.isAlive()) {
                toRemove.add(entity);
                continue;
            }

            dragData.dragTicks++;

            // Pull entity toward user progressively
            Vec3 targetPos = userPos.add(user.getLookAngle().scale(2.0)); // 2 blocks in front of user
            Vec3 currentPos = entity.position();
            Vec3 dragVelocity = targetPos.subtract(currentPos).scale(0.3); // Smooth dragging

            entity.setDeltaMovement(dragVelocity);
            entity.hurtMarked = true;
            entity.hasImpulse = true;

            // Create drag trail effect
            createDragTrailEffect(entity.position());

            // Hit dragged entities periodically for constant damage
            if (dragData.dragTicks % 10 == 0) {
                float originalDamage = damage;
                damage = damage * 0.3f; // Light continuous damage while dragged
                hitTargetNoImmunity(entity);
                damage = originalDamage;
            }
        }

        // Remove dead entities
        for (LivingEntity entity : toRemove) {
            draggedEntities.remove(entity);
            comboTargets.remove(entity);
        }
    }

    private void executeWaterDragonFinisher() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create massive water dragon
        createWaterDragonEffect();

        // Hit all enemies in large finisher area
        List<LivingEntity> finisherTargets = getTargetsInCustomHitbox(
                userPos.add(lookDir.scale(range * 0.7)),
                hitboxSize * 1.5, 4.0, hitboxSize * 1.5);

        for (LivingEntity target : finisherTargets) {
            // Full finisher damage
            hitTarget(target);

            // Strong knockback from dragon
            Vec3 dragonKnockback = lookDir.scale(knockback * 1.2);
            target.push(dragonKnockback.x, 0.6, dragonKnockback.z);

            createDragonImpactEffect(target.position());
        }

        // Extra damage for dragged enemies (they were set up for this)
        for (LivingEntity draggedEnemy : draggedEntities.keySet()) {
            if (draggedEnemy.isAlive()) {
                // Massive bonus damage for dragged enemies
                float originalDamage = damage;
                damage = damage * 1.8f; // 180% damage for successful drag combo
                hitTarget(draggedEnemy);
                damage = originalDamage;

                // Epic knockback
                Vec3 epicKnockback = lookDir.scale(knockback * 2.0);
                draggedEnemy.push(epicKnockback.x, 0.8, epicKnockback.z);

                createDragonImpactEffect(draggedEnemy.position());
            }
        }

        // Water dragon finisher sounds
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0f, 0.8f);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.4f, 0.7f);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5f, 0.7f);
    }

    private void createFluxStartEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Constant flux energy buildup
        for (int i = 0; i < 24; i++) {
            double angle = (i / 24.0) * 2 * Math.PI;
            double radius = 2.5;
            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + Math.sin(angle * 2) * 0.8;

            // Flux energy particles
            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                    x, y, z, 2, 0.1, 0.1, 0.1, 0.05);

            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.BUBBLE,
                        x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }
    }

    private void createComboSlashEffect(Vec3 userPos, int hitNumber) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 lookDir = user.getLookAngle();

        // Different slash patterns for each combo hit
        for (int i = -3; i <= 3; i++) {
            double angle = i * 20 + (hitNumber * 30); // Vary angle based on hit number
            double radians = Math.toRadians(angle);

            Vec3 slashDir = lookDir.yRot((float)radians);
            Vec3 slashPos = userPos.add(slashDir.scale(range * 0.8));

            // Combo slash particles
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    slashPos.x, slashPos.y, slashPos.z,
                    3, 0.2, 0.2, 0.2, 0.1);

            if (Math.abs(i) <= 1) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        slashPos.x, slashPos.y, slashPos.z,
                        2, 0.1, 0.1, 0.1, 0.08);
            }
        }
    }

    private void createComboHitEffect(Vec3 hitPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 targetPos = hitPos.add(0, 1, 0);

        // Combo hit particles
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                targetPos.x, targetPos.y, targetPos.z,
                8, 0.3, 0.3, 0.3, 0.15);

        serverLevel.sendParticles(ParticleTypes.CRIT,
                targetPos.x, targetPos.y, targetPos.z,
                5, 0.2, 0.2, 0.2, 0.1);
    }

    private void createDragTrailEffect(Vec3 entityPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Trail behind dragged entities
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                entityPos.x, entityPos.y + 0.5, entityPos.z,
                2, 0.2, 0.2, 0.2, 0.08);

        // Occasional bubbles
        if (fluxTicks % 4 == 0) {
            serverLevel.sendParticles(ParticleTypes.BUBBLE,
                    entityPos.x, entityPos.y, entityPos.z,
                    1, 0.1, 0.1, 0.1, 0.05);
        }
    }

    private void createWaterDragonEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create water dragon body extending forward
        for (int segment = 0; segment < 12; segment++) {
            Vec3 dragonPos = userPos.add(lookDir.scale(segment * 0.8));

            // Dragon body undulation
            double undulation = Math.sin(segment * 0.5) * 1.2;
            Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();
            dragonPos = dragonPos.add(rightDir.scale(undulation)).add(0, Math.abs(undulation) * 0.5, 0);

            // Main dragon body
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    dragonPos.x, dragonPos.y, dragonPos.z,
                    8, 0.4, 0.4, 0.4, 0.2);

            // Dragon details
            if (segment % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.BUBBLE,
                        dragonPos.x, dragonPos.y, dragonPos.z,
                        4, 0.3, 0.3, 0.3, 0.15);
            }

            // Dragon head (first segment)
            if (segment == 0) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        dragonPos.x, dragonPos.y, dragonPos.z,
                        2, 0.2, 0.2, 0.2, 0);
            }
        }

        // Dragon roar effect
        Vec3 dragonHead = userPos.add(lookDir.scale(range));
        createWaterExplosion(dragonHead, 3.0f);
    }

    private void createDragonImpactEffect(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 targetPos = impactPos.add(0, 1, 0);

        // Dragon impact - very dramatic
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                targetPos.x, targetPos.y, targetPos.z,
                3, 0.3, 0.3, 0.3, 0);

        serverLevel.sendParticles(ParticleTypes.SPLASH,
                targetPos.x, targetPos.y, targetPos.z,
                25, 0.8, 0.8, 0.8, 0.4);

        serverLevel.sendParticles(ParticleTypes.CRIT,
                targetPos.x, targetPos.y, targetPos.z,
                20, 0.6, 0.6, 0.6, 0.3);

        // Water burst around impact
        createWaterCircle(targetPos, 3.0f, 16);
    }

    @Override
    protected void onStop() {
        // Release all dragged entities
        for (LivingEntity entity : draggedEntities.keySet()) {
            if (entity.isAlive()) {
                // Reset velocity
                entity.setDeltaMovement(Vec3.ZERO);
            }
        }

        // Final dragon dissolution
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

            // Dragon dissolving into water
            createWaterExplosion(userPos, 3.5f);

            // Rain from dissolved dragon
            for (int i = 0; i < 60; i++) {
                double x = userPos.x + (Math.random() - 0.5) * range * 1.5;
                double z = userPos.z + (Math.random() - 0.5) * range * 1.5;
                double y = userPos.y + 5 + Math.random() * 3;

                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        x, y, z, 1, 0, -0.4, 0, 0.15);
            }
        }



        // Clear state
        draggedEntities.clear();
        comboTargets.clear();
        comboHitsExecuted = 0;
        nextHitTick = 0;
        finisherExecuted = false;
        fluxTicks = 0;
    }
}