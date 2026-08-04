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
 * Second Form: Water Wheel (Wheel Version)
 * The user jumps forward and creates a vertical wheel around them
 * Multi-hit lingering attack that lunges forward slightly
 */
public class WaterWheelAttack extends WaterBreathingAttackBase {

    private boolean wheelStarted = false;
    private final Map<LivingEntity, Integer> hitTickMap = new HashMap<>();
    private final List<LivingEntity> draggedEnemies = new ArrayList<>();
    private int wheelTicks = 0;
    private Vec3 lastWheelPos;

    public WaterWheelAttack() {
    }

    @Override
    protected void onStart() {
        wheelStarted = false;
        hitTickMap.clear();
        draggedEnemies.clear();
        wheelTicks = 0;
        lastWheelPos = null;
    }

    @Override
    protected void onActiveStart() {
        // Water wheel startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Start wheel after windup
        if (!wheelStarted && tickCount == windup + 1) {
            startWheel();
            wheelStarted = true;
        }

        // Continue wheel rotation during duration
        if (wheelStarted && tickCount > windup && tickCount < windup + duration) {
            wheelTicks++;
            performWheel();
        }
    }

    private void startWheel() {
        playWaterVfxAt(VfxIds.WATER_WHEEL,
                user.position().add(0, user.getBbHeight() * 0.15, 0), user.getLookAngle(), 1.0f);
        // Small forward lunge using dash speed
        if (dashSpeed != null && dashSpeed > 0) {
            Vec3 lungeDirection = user.getLookAngle().normalize();
            Vec3 lungeVelocity = lungeDirection.scale(dashSpeed * 0.5); // Half dash speed for lunge
            user.setDeltaMovement(lungeVelocity.x, 0.3, lungeVelocity.z); // Small upward component
            user.hurtMarked = true;
            user.hasImpulse = true;
        }

        // Wheel start sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.9f);

        // Create initial wheel effect
    }

    private void performWheel() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Create continuous vertical wheel effect
        dragCaughtEnemies();

        // Hit enemies in wheel radius - allow multiple hits but with timing
        if (wheelTicks % 3 == 0) {
            List<LivingEntity> targets = getTargetsInCustomHitbox(
                    userPos,
                    hitboxSize, // width
                    hitboxSize + 1, // height (taller for vertical wheel)
                    hitboxSize  // depth
            );

            for (LivingEntity target : targets) {
                // Use no immunity for multi-hit but track recently hit entities
                if (!hasRecentlyHit(target)) {
                    catchAndHitTarget(target);

                    // Individual hit sound
                    world.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.6f, 1.2f + wheelTicks * 0.05f);
                }
            }
        }

        if (lastWheelPos != null && lastWheelPos.distanceToSqr(userPos) > 0.01) {
            for (LivingEntity target : getTargetsInLine(lastWheelPos, userPos, hitboxSize * 0.75)) {
                if (!hasRecentlyHit(target)) {
                    catchAndHitTarget(target);
                }
            }
        }
        lastWheelPos = userPos;

        // Wheel rotation sound every few ticks
        if (wheelTicks % 10 == 0) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 0.8f, 1.0f + wheelTicks * 0.02f);
        }
    }

    private boolean hasRecentlyHit(LivingEntity target) {
        Integer lastHitTick = hitTickMap.get(target);
        if (lastHitTick == null) return false;
        return (wheelTicks - lastHitTick) < 12;
    }

    private void trackRecentHit(LivingEntity target) {
        hitTickMap.put(target, wheelTicks);
    }

    private void catchAndHitTarget(LivingEntity target) {
        hitTargetNoImmunity(target);
        trackRecentHit(target);
        if (!draggedEnemies.contains(target)) {
            draggedEnemies.add(target);
        }
    }

    private void dragCaughtEnemies() {
        Vec3 dragAnchor = user.position().add(0, user.getBbHeight() / 4, 0);
        for (LivingEntity draggedEnemy : new ArrayList<>(draggedEnemies)) {
            if (!draggedEnemy.isAlive()) {
                draggedEnemies.remove(draggedEnemy);
                continue;
            }

            Vec3 toDrag = dragAnchor.subtract(draggedEnemy.position());
            double dist = toDrag.length();
            if (dist > 0.5) {
                Vec3 dragVelocity = toDrag.normalize().scale(Math.min(dist, 3.5));
                draggedEnemy.setDeltaMovement(dragVelocity);
            } else {
                Vec3 userVelocity = user.getDeltaMovement();
                draggedEnemy.setDeltaMovement(
                        userVelocity.x,
                        draggedEnemy.getDeltaMovement().y,
                        userVelocity.z
                );
            }
            draggedEnemy.hurtMarked = true;
            draggedEnemy.hasImpulse = true;
        }
    }

    @Override
    protected void onStop() {
        // Reset user velocity
        user.setDeltaMovement(Vec3.ZERO);

        // Clear hit tracking
        hitTickMap.clear();
        draggedEnemies.clear();
        wheelTicks = 0;
        wheelStarted = false;
        lastWheelPos = null;

        // Final water sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0f, 0.8f);
    }
}
