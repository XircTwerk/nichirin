package com.xirc.nichirin.common.attack.moves.breathing.water;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

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
        createWhirlpoolFormation();

        // Catch initial entities in whirlpool
        catchEntitiesInWhirlpool();
    }

    private void performWhirlpool() {
        // Create continuous whirlpool visual effect
        createContinuousWhirlpoolEffect();

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
                createCatchEffect(entity.position());
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
                createSpinningHitEffect(entity.position());
            }

            // Create spinning trail
            if (data.spinTicks % 3 == 0) {
                createSpinningTrail(entity.position());
            }
        }

        // Remove dead entities
        for (LivingEntity entity : toRemove) {
            spinningingEntities.remove(entity);
        }
    }

    private void createWhirlpoolFormation() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Create ascending whirlpool formation
        for (int layer = 0; layer < 8; layer++) {
            float layerHeight = (WHIRLPOOL_HEIGHT / 8) * layer;
            float layerRadius = range * (0.2f + layer * 0.1f); // Wide at top, narrow at bottom (funnel)

            createWaterVortex(whirlpoolCenter.add(0, layerHeight, 0), layerRadius, 0.5f, 1);
        }

        // Central water spout
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                whirlpoolCenter.x, whirlpoolCenter.y, whirlpoolCenter.z,
                20, 0.3, WHIRLPOOL_HEIGHT * 0.3, 0.3, 0.25);
    }

    private void createContinuousWhirlpoolEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Multi-layer spinning whirlpool
        for (int layer = 0; layer < 6; layer++) {
            float layerHeight = (WHIRLPOOL_HEIGHT / 6) * layer;
            float layerRadius = range * (0.3f + layer * 0.12f); // Wide at top, narrow at bottom (funnel shape)
            double layerSpinSpeed = whirlpoolTicks * 0.2 * (1 + layer * 0.1); // Faster at higher layers
            int particlesInLayer = Math.max(6, 12 - layer);

            for (int i = 0; i < particlesInLayer; i++) {
                double baseAngle = (2 * Math.PI * i) / particlesInLayer;
                double angle = baseAngle + layerSpinSpeed;

                double x = whirlpoolCenter.x + Math.cos(angle) * layerRadius;
                double z = whirlpoolCenter.z + Math.sin(angle) * layerRadius;
                double y = whirlpoolCenter.y + layerHeight;

                // Main whirlpool particles
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        x, y, z, 2, 0.1, 0.1, 0.1, 0.08);

                // Bubbles for underwater effect
                if (layer < 3) {
                    serverLevel.sendParticles(ParticleTypes.BUBBLE,
                            x, y, z, 1, 0.05, 0.05, 0.05, 0.03);
                }
            }
        }

        // Central ascending water column
        if (whirlpoolTicks % 3 == 0) {
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    whirlpoolCenter.x, whirlpoolCenter.y, whirlpoolCenter.z,
                    8, 0.2, WHIRLPOOL_HEIGHT * 0.4, 0.2, 0.15);
        }
    }

    private void createCatchEffect(Vec3 entityPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Whirlpool catch effect
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                entityPos.x, entityPos.y + 1, entityPos.z,
                15, 0.5, 0.5, 0.5, 0.25);

        // Swirling water around caught entity
        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * 2 * Math.PI;
            double radius = 1.2;

            double x = entityPos.x + Math.cos(angle) * radius;
            double z = entityPos.z + Math.sin(angle) * radius;
            double y = entityPos.y + 0.5;

            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    x, y, z, 2, 0.1, 0.1, 0.1, 0.08);
        }
    }

    private void createSpinningHitEffect(Vec3 entityPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Hit effect while spinning
        serverLevel.sendParticles(ParticleTypes.CRIT,
                entityPos.x, entityPos.y + 1, entityPos.z,
                8, 0.3, 0.3, 0.3, 0.15);

        // Water splash from spinning hit
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                entityPos.x, entityPos.y + 1, entityPos.z,
                12, 0.4, 0.4, 0.4, 0.2);
    }

    private void createSpinningTrail(Vec3 entityPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Trail behind spinning entities
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                entityPos.x, entityPos.y + 0.5, entityPos.z,
                3, 0.2, 0.2, 0.2, 0.1);

        // Occasional bubbles in trail
        if (whirlpoolTicks % 6 == 0) {
            serverLevel.sendParticles(ParticleTypes.BUBBLE,
                    entityPos.x, entityPos.y, entityPos.z,
                    1, 0.1, 0.1, 0.1, 0.05);
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

        // Final whirlpool dissolution effect
        if (world instanceof ServerLevel serverLevel) {
            // Whirlpool collapsing effect
            for (int layer = 0; layer < 8; layer++) {
                float layerHeight = WHIRLPOOL_HEIGHT - (WHIRLPOOL_HEIGHT / 8) * layer; // From top down
                float layerRadius = range * (0.5f + layer * 0.1f); // Expanding outward

                createWaterCircle(whirlpoolCenter.add(0, layerHeight, 0), layerRadius, 12);
            }

            // Final water explosion at center
            createWaterExplosion(whirlpoolCenter.add(0, WHIRLPOOL_HEIGHT / 2, 0), 2.0f);

            // Rain effect from collapsed whirlpool
            for (int i = 0; i < 30; i++) {
                double x = whirlpoolCenter.x + (Math.random() - 0.5) * range * 2;
                double z = whirlpoolCenter.z + (Math.random() - 0.5) * range * 2;
                double y = whirlpoolCenter.y + WHIRLPOOL_HEIGHT + Math.random() * 2;

                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        x, y, z, 1, 0, -0.4, 0, 0.1);
            }
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