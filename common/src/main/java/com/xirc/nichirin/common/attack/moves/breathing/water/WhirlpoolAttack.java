package com.xirc.nichirin.common.attack.moves.breathing.water;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import com.xirc.nichirin.common.vfx.VfxIds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sixth Form: Whirlpool
 * Jumps up and creates an ascending whirlpool around the user
 * Rises 4 blocks up, multi-hit attack where whirlpool stays in place
 * Hit entities start spinning uncontrollably and moving around in a circle
 */
public class WhirlpoolAttack extends WaterBreathingAttackBase {

    private boolean whirlpoolStarted = false;
    private Vec3 whirlpoolCenter; // Whirlpool stays in place
    private final Map<LivingEntity, WhirlpoolData> spinningingEntities = new HashMap<>();
    private int whirlpoolTicks = 0;
    private static final float WHIRLPOOL_HEIGHT = 4.0f;
    private static final float SPIN_SPEED = 0.3f;

    // Data class to track spinning entities
    private static class WhirlpoolData {
        double angle;
        double radius;
        int spinTicks;

        WhirlpoolData(double initialAngle, double initialRadius) {
            this.angle = initialAngle;
            this.radius = initialRadius;
            this.spinTicks = 0;
        }
    }

    public WhirlpoolAttack() {
    }

    @Override
    protected void onStart() {
        whirlpoolStarted = false;
        spinningingEntities.clear();
        whirlpoolTicks = 0;
        whirlpoolCenter = null;
    }

    @Override
    protected void onActiveStart() {
        // Whirlpool startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Start whirlpool after windup
        if (!whirlpoolStarted && tickCount == windup + 1) {
            startWhirlpool();
            whirlpoolStarted = true;
        }

        // Continue whirlpool effects during duration
        if (whirlpoolStarted && tickCount > windup && tickCount < windup + duration) {
            whirlpoolTicks++;
            performWhirlpool();
        }
    }

    private void startWhirlpool() {
        // Record whirlpool center - it stays in place
        whirlpoolCenter = user.position();
        playWaterVfxAt(VfxIds.WHIRLPOOL, whirlpoolCenter, Vec3.ZERO, 1.0f);

        // Launch user upward 4 blocks
        user.setDeltaMovement(0, 0.8, 0);
        user.hurtMarked = true;
        user.hasImpulse = true;

        // Apply slow falling so the user lands safely after the whirlpool
        user.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration + 40, 0, false, false, false));

        // Sync to client
        if (user instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(user));
        }

        // Whirlpool start sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.2f, 0.7f);

        // Create initial whirlpool effect

        // Catch initial entities in whirlpool
        catchEntitiesInWhirlpool();
    }

    private void performWhirlpool() {
        // Create continuous whirlpool visual effect

        // Update spinning entities
        updateSpinningEntities();

        // Catch new entities every few ticks
        if (whirlpoolTicks % 5 == 0) {
            catchEntitiesInWhirlpool();
        }

        // Continuous whirlpool sound
        if (whirlpoolTicks % 10 == 0) {
            world.playSound(null, whirlpoolCenter.x, whirlpoolCenter.y + 2, whirlpoolCenter.z,
                    SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 0.8f, 0.8f + whirlpoolTicks * 0.02f);
        }
    }

    private void catchEntitiesInWhirlpool() {
        // Get entities in whirlpool area (fixed position)
        List<LivingEntity> nearbyEntities = getTargetsInCustomHitbox(
                whirlpoolCenter.add(0, WHIRLPOOL_HEIGHT / 2, 0),
                range * 2, // Full diameter
                WHIRLPOOL_HEIGHT + 1, // Full height
                range * 2  // Full diameter
        );

        for (LivingEntity entity : nearbyEntities) {
            if (!spinningingEntities.containsKey(entity)) {
                // Calculate initial position relative to whirlpool center
                Vec3 entityPos = entity.position();
                Vec3 relativePos = entityPos.subtract(whirlpoolCenter);

                double initialAngle = Math.atan2(relativePos.z, relativePos.x);
                double initialRadius = Math.min(Math.sqrt(relativePos.x * relativePos.x + relativePos.z * relativePos.z), range * 0.9);

                // Add to spinning entities
                spinningingEntities.put(entity, new WhirlpoolData(initialAngle, initialRadius));

                // Hit the entity when caught
                hitTarget(entity);

                // Catch sound effect
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.6f, 1.4f);

                // Create catch effect
            }
        }
    }

    private void updateSpinningEntities() {
        List<LivingEntity> toRemove = new ArrayList<>();

        for (Map.Entry<LivingEntity, WhirlpoolData> entry : spinningingEntities.entrySet()) {
            LivingEntity entity = entry.getKey();
            WhirlpoolData data = entry.getValue();

            if (!entity.isAlive()) {
                toRemove.add(entity);
                continue;
            }

            data.spinTicks++;

            // Update spinning angle
            data.angle += SPIN_SPEED;

            // Gradually pull entities toward center
            data.radius = Math.max(0.8, data.radius * 0.995);

            // Calculate new position
            double newX = whirlpoolCenter.x + Math.cos(data.angle) * data.radius;
            double newZ = whirlpoolCenter.z + Math.sin(data.angle) * data.radius;
            double newY = whirlpoolCenter.y + (Math.sin(data.spinTicks * 0.2) * 1.5) + 1; // Bobbing up and down

            // Move entity to new spinning position
            Vec3 targetPos = new Vec3(newX, newY, newZ);
            Vec3 currentPos = entity.position();
            Vec3 velocity = targetPos.subtract(currentPos).scale(0.4); // Smooth movement

            entity.setDeltaMovement(velocity);
            entity.hurtMarked = true;
            entity.hasImpulse = true;

            // Hit spinning entities periodically (halved from 10 after the double-tick dedup)
            if (data.spinTicks % 5 == 0) {
                hitTargetNoImmunity(entity);

                // Create spinning hit effect
            }

        }

        // Remove dead entities
        for (LivingEntity entity : toRemove) {
            spinningingEntities.remove(entity);
        }
    }


    @Override
    public boolean isWhirlpoolAttack() {
        return true; // This creates a whirlpool
    }

    @Override
    protected void onStop() {
        // Release all spinning entities
        for (Map.Entry<LivingEntity, WhirlpoolData> entry : spinningingEntities.entrySet()) {
            LivingEntity entity = entry.getKey();
            if (entity.isAlive()) {
                // Final spin damage
                hitTargetNoImmunity(entity);

                // Launch entities outward from whirlpool center
                Vec3 launchDirection = entity.position().subtract(whirlpoolCenter).normalize();
                entity.push(launchDirection.x * knockback * 1.5, 0.4, launchDirection.z * knockback * 1.5);

                // Reset entity velocity to prevent continued spinning
                entity.setDeltaMovement(launchDirection.scale(0.5));
                entity.hurtMarked = true;
            }
        }

        // User gentle descent
        user.setDeltaMovement(0, -0.2, 0); // Gentle downward velocity

        if (whirlpoolCenter == null) {
            spinningingEntities.clear();
            whirlpoolTicks = 0;
            whirlpoolStarted = false;
            return;
        }

        // Final whirlpool collapse sound
        world.playSound(null, whirlpoolCenter.x, whirlpoolCenter.y + 2, whirlpoolCenter.z,
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5f, 0.6f);

        // Clear state
        spinningingEntities.clear();
        whirlpoolTicks = 0;
        whirlpoolStarted = false;
        whirlpoolCenter = null;
    }
}
