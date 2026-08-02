package com.xirc.nichirin.common.attack.moves.breathing.water;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import com.xirc.nichirin.common.vfx.VfxIds;

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
        if (comboHitsExecuted == 0) {
            playWaterVfx(VfxIds.CONSTANT_FLUX, user.position(), user.getLookAngle(), 0.85f);
        }

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

        }

        // Create combo slash visual

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
        playWaterVfx(VfxIds.CONSTANT_FLUX, user.position(), lookDir, 1.35f);

        // Create massive water dragon

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


    @Override
    protected void onStop() {
        // Release all dragged entities
        for (LivingEntity entity : draggedEntities.keySet()) {
            if (entity.isAlive()) {
                // Reset velocity
                entity.setDeltaMovement(Vec3.ZERO);
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
