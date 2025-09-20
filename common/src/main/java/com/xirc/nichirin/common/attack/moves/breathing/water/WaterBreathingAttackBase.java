package com.xirc.nichirin.common.attack.moves.breathing.water;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Base class for Water Breathing attacks
 * Extends AbstractBreathingAttack for unified interface
 *
 * Water-specific functionality includes water effects, pressure attacks, and fluid movement
 * All Water Breathing attacks create flowing water effects and excel at close-range pressure
 */
@SuppressWarnings("rawtypes")
public abstract class WaterBreathingAttackBase extends AbstractBreathingAttack<WaterBreathingAttackBase, IBreathingAttacker> {

    // Water-specific properties
    private static final int WATER_PARTICLE_COUNT = 20;
    private static final float WATER_PARTICLE_SPREAD = 1.2f;

    /**
     * Apply Water Breathing specific damage and effects to a target
     * Overrides the base hitTarget to add Water-specific effects
     */
    @Override
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;

        // Apply base damage and knockback
        super.hitTarget(target);

        // Apply Water Breathing specific effects
        applyWaterEffect(target);

        // Create water particles at target location
        createWaterHitParticles(target.position());

        // Play water hit sound
        playWaterHitSound(target.position());
    }

    /**
     * Special Water Breathing hit method that removes immunity frames
     * Used for multi-hit attacks like Water Wheel and Waterfall Basin
     */
    @Override
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;

        // Apply base no-immunity hit
        super.hitTargetNoImmunity(target);

        // Apply Water Breathing specific effects
        applyWaterEffect(target);

        // Create water particles at target location
        createWaterHitParticles(target.position());

        // Play water hit sound
        playWaterHitSound(target.position());
    }

    /**
     * Apply water effect to target
     * Water breathing creates pressure and momentum effects
     */
    protected void applyWaterEffect(LivingEntity target) {
        // Skip creative mode players
        if (target instanceof net.minecraft.world.entity.player.Player player && player.isCreative()) {
            return;
        }

        // Apply slowness effect to represent water pressure
        int slownessDurationTicks = Math.max(40, (int)(damage * 2)); // 2-8+ seconds based on damage
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                slownessDurationTicks,
                0, // Amplifier 0 (slight slowness)
                false, // Not ambient
                true   // Show particles
        ));
    }

    /**
     * Create water particles at a specific hit location
     */
    protected void createWaterHitParticles(Vec3 hitPosition) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Explosion of water particles at hit location
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                hitPosition.x, hitPosition.y + 1, hitPosition.z,
                25, 0.6, 0.6, 0.6, 0.3);

        // Dripping water particles
        serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                hitPosition.x, hitPosition.y + 1, hitPosition.z,
                12, 0.4, 0.4, 0.4, 0.1);

        // Bubble particles for underwater effect
        serverLevel.sendParticles(ParticleTypes.BUBBLE,
                hitPosition.x, hitPosition.y + 0.5, hitPosition.z,
                8, 0.3, 0.3, 0.3, 0.1);
    }

    /**
     * Create a line of water particles between two points
     * Used for slash attacks and water streams
     */
    protected void createWaterTrail(Vec3 start, Vec3 end) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        Vec3 normalized = direction.normalize();

        // Create particles along the trail
        for (double d = 0; d <= distance; d += 0.5) {
            Vec3 particlePos = start.add(normalized.scale(d));

            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    particlePos.x, particlePos.y, particlePos.z,
                    3, 0.2, 0.2, 0.2, 0.1);

            if (d % 1.0 < 0.1) { // Every other particle position
                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    /**
     * Create a circular water effect around a position
     * Used for AOE attacks and water wheels
     */
    protected void createWaterCircle(Vec3 center, float radius, int particleCount) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Use ServerLevel's random instead of world.random to avoid threading issues
        net.minecraft.util.RandomSource random = serverLevel.getRandom();

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y + random.nextDouble() * 2;

            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    x, y, z, 2, 0.2, 0.2, 0.2, 0.1);

            // Add some bubbles for variety
            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.BUBBLE,
                        x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    /**
     * Create a water vortex/whirlpool effect
     * Used for whirlpool attacks and spinning water effects
     */
    protected void createWaterVortex(Vec3 center, float radius, float height, int layers) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Use ServerLevel's random instead of world.random to avoid threading issues
        net.minecraft.util.RandomSource random = serverLevel.getRandom();

        for (int layer = 0; layer < layers; layer++) {
            float layerHeight = (height / layers) * layer;
            float layerRadius = radius * (1.0f - (float)layer / layers * 0.3f); // Spiral inward
            int particlesInLayer = Math.max(8, 16 - layer * 2);

            for (int i = 0; i < particlesInLayer; i++) {
                double baseAngle = (2 * Math.PI * i) / particlesInLayer;
                double spiralOffset = layer * 0.8; // Create spiral effect
                double angle = baseAngle + spiralOffset;

                double x = center.x + Math.cos(angle) * layerRadius;
                double z = center.z + Math.sin(angle) * layerRadius;
                double y = center.y + layerHeight;

                // Main water particles
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        x, y, z, 2, 0.1, 0.1, 0.1, 0.05);

                // Bubbles for inner layers
                if (layer < layers / 2) {
                    serverLevel.sendParticles(ParticleTypes.BUBBLE,
                            x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
                }
            }
        }

        // Central water spout
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                center.x, center.y, center.z, 12, 0.2, height * 0.5, 0.2, 0.2);
    }

    /**
     * Create a water explosion effect
     * Used for powerful water attacks and finishers
     */
    protected void createWaterExplosion(Vec3 center, float intensity) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Use ServerLevel's random instead of world.random to avoid threading issues
        net.minecraft.util.RandomSource random = serverLevel.getRandom();

        int baseParticles = (int)(30 * intensity);

        // Central water explosion
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                center.x, center.y + 1, center.z,
                1, 0, 0, 0, 0);

        // Ring of water splashes
        for (int ring = 1; ring <= 3; ring++) {
            float ringRadius = ring * intensity;
            int ringParticles = baseParticles / ring;

            for (int i = 0; i < ringParticles; i++) {
                double angle = (2 * Math.PI * i) / ringParticles;
                double x = center.x + Math.cos(angle) * ringRadius;
                double z = center.z + Math.sin(angle) * ringRadius;
                double y = center.y + random.nextDouble() * 3;

                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        x, y, z, 3, 0.3, 0.3, 0.3, 0.2);
            }
        }

        // Upward water burst
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                center.x, center.y + 1, center.z,
                (int)(60 * intensity), 1.5, 3.0, 1.5, 0.4);

        // Rain effect around explosion
        for (int i = 0; i < (int)(40 * intensity); i++) {
            double x = center.x + (random.nextDouble() - 0.5) * intensity * 4;
            double z = center.z + (random.nextDouble() - 0.5) * intensity * 4;
            double y = center.y + 2 + random.nextDouble() * 2;

            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                    x, y, z, 1, 0, -0.5, 0, 0.1);
        }
    }

    /**
     * Create a waterfall effect
     * Used for Waterfall Basin and vertical water attacks
     */
    protected void createWaterfall(Vec3 start, float width, float height, int streams) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Use ServerLevel's random instead of world.random to avoid threading issues
        net.minecraft.util.RandomSource random = serverLevel.getRandom();

        for (int stream = 0; stream < streams; stream++) {
            // Calculate stream position across the width
            double streamOffset = (stream - streams / 2.0) * (width / streams);
            Vec3 streamStart = start.add(streamOffset, height, 0);

            // Create falling water particles
            for (int h = 0; h <= (int)height; h++) {
                Vec3 particlePos = streamStart.add(0, -h, 0);

                // Main waterfall particles
                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        particlePos.x, particlePos.y, particlePos.z,
                        2, 0.1, 0, 0.1, 0);

                // Splash particles every few blocks
                if (h % 3 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SPLASH,
                            particlePos.x, particlePos.y, particlePos.z,
                            4, 0.3, 0.2, 0.3, 0.1);
                }
            }

            // Pool at the bottom
            Vec3 poolCenter = start.add(streamOffset, 0, 0);
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    poolCenter.x, poolCenter.y, poolCenter.z,
                    8, width * 0.2, 0.1, width * 0.2, 0.2);
        }
    }

    /**
     * Play water-related sound effects
     */
    protected void playWaterSound() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
    }

    /**
     * Play water hit sound
     */
    protected void playWaterHitSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.8f, 1.3f);
        }
    }

    /**
     * Play water slash sound
     */
    protected void playWaterSlashSound() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 1.1f);
        }
    }

    /**
     * Play water flow sound
     */
    protected void playWaterFlowSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }

    /**
     * Play water explosion sound
     */
    protected void playWaterExplosionSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.2f, 0.9f);
        }
    }

    // Water Breathing specific utility methods

    /**
     * Check if this attack creates a whirlpool
     */
    public boolean isWhirlpoolAttack() {
        // Override in specific attacks that create whirlpools
        return false;
    }

    /**
     * Check if this attack is a dash/mobility attack
     */
    public boolean isDashAttack() {
        return hasDash() || hasTeleport();
    }

    /**
     * Check if this attack hits in a 360-degree area
     */
    public boolean isOmnidirectional() {
        // Override in attacks like Striking Tide
        return false;
    }

    /**
     * Check if this attack creates a persistent area effect
     */
    public boolean isPersistentArea() {
        // Override in attacks like Dead Calm
        return false;
    }

    /**
     * Check if this attack provides defensive capabilities
     */
    public boolean hasDefensiveProperties() {
        // Override in attacks like Drop Ripple Thrust
        return false;
    }

    /**
     * Get the water pressure effect duration this attack applies (in ticks)
     */
    public int getPressureDuration() {
        return Math.max(40, (int)(damage * 2));
    }

    // Abstract methods that Water attacks must implement
    // These are inherited from AbstractBreathingAttack but documented here for clarity

    /**
     * Called when the Water attack starts (after breath consumption)
     * Implement Water-specific startup effects here (particles, sounds, positioning)
     */
    @Override
    protected abstract void onStart();

    /**
     * Called every tick during the attack (after windup period)
     * Implement the main Water attack logic here (movement, damage, effects)
     */
    @Override
    protected abstract void perform();

    /**
     * Called when the Water attack ends (optional override)
     * Implement Water-specific cleanup logic here
     */
    @Override
    protected void onStop() {
        super.onStop();
        // Additional Water-specific cleanup can be added here
    }
}