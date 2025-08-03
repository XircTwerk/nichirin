package com.xirc.nichirin.common.attack.moves.insect;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Base class for Insect Breathing attacks
 * Extends AbstractBreathingAttack for unified interface
 *
 * Insect-specific functionality includes poison effects, precise strikes, and agile movement
 * All Insect Breathing attacks apply poison and excel at single-target precision
 * Users are immune to damage during attack execution for tactical positioning
 */
@SuppressWarnings("rawtypes")
public abstract class InsectBreathingAttackBase extends AbstractBreathingAttack<InsectBreathingAttackBase, IBreathingAttacker> {

    // Insect-specific properties
    private static final int DEFAULT_POISON_DURATION = 60; // 3 seconds of poison (60 ticks)
    private static final int INSECT_PARTICLE_COUNT = 12;
    private static final float INSECT_PARTICLE_SPREAD = 1.0f;

    // Invulnerability tracking
    private int originalInvulnerableTime = 0;

    @Override
    protected void onStart() {
        // Store original invulnerable time
        originalInvulnerableTime = user.invulnerableTime;

        // Grant invulnerability for the full attack duration (windup + duration)
        int totalAttackTime = windup + duration;
        user.invulnerableTime = totalAttackTime;

        // Call the default onStart method for specific attacks
        onStartInsectAttack();
    }

    @Override
    protected void onStop() {
        // Restore original invulnerable time (usually 0)
        user.invulnerableTime = originalInvulnerableTime;

        // Call the default onStop method for specific attacks
        onStopInsectAttack();

        // Call parent cleanup
        super.onStop();
    }

    /**
     * Default start behavior for Insect Breathing attacks
     * Called after invulnerability is granted
     * Can be overridden by specific attacks if needed
     */
    protected void onStartInsectAttack() {
        // Default implementation - attacks can override if needed
    }

    /**
     * Default stop behavior for Insect Breathing attacks
     * Called before invulnerability is restored
     * Can be overridden by specific attacks if needed
     */
    protected void onStopInsectAttack() {
        // Default implementation - attacks can override if needed
    }

    /**
     * Apply Insect Breathing specific damage and effects to a target
     * Overrides the base hitTarget to add Insect-specific effects
     */
    @Override
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;

        // Apply base damage and knockback
        super.hitTarget(target);

        // Apply Insect Breathing specific poison effect
        applyPoisonEffect(target);

        // Create insect particles at target location
        createInsectHitParticles(target.position());

        // Play insect hit sound
        playInsectHitSound(target.position());
    }

    /**
     * Special Insect Breathing hit method that removes immunity frames
     * Used for multi-hit attacks like Dragonfly
     */
    @Override
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;

        // Apply base no-immunity hit
        super.hitTargetNoImmunity(target);

        // Apply Insect Breathing specific poison effect
        applyPoisonEffect(target);

        // Create insect particles at target location
        createInsectHitParticles(target.position());

        // Play insect hit sound
        playInsectHitSound(target.position());
    }

    /**
     * Apply stackable poison effect to target
     * Each hit increases venom stacks up to a maximum of 10
     */
    protected void applyPoisonEffect(LivingEntity target) {
        // Skip creative mode players
        if (target instanceof net.minecraft.world.entity.player.Player player && player.isCreative()) {
            return;
        }

        // Apply custom stackable Venom effect
        net.minecraft.world.effect.MobEffect venomEffect =
                com.xirc.nichirin.registry.NichirinEffectRegistry.VENOM.get();

        // Check if target already has venom
        MobEffectInstance existingVenom = target.getEffect(venomEffect);

        int newAmplifier = 0; // Start at 0 (1 stack)
        int baseDuration = Math.max(DEFAULT_POISON_DURATION, (int)(damage * 3));
        int newDuration = baseDuration;

        if (existingVenom != null) {
            // Stack the effect - increase amplifier up to max of 9 (10 stacks total)
            newAmplifier = Math.min(existingVenom.getAmplifier() + 1, 9);
            // Refresh duration to full duration
            newDuration = baseDuration;
        }

        // Apply the stacked venom
        target.addEffect(new MobEffectInstance(
                venomEffect,
                newDuration,
                newAmplifier,
                false, // Not ambient
                true   // Show particles
        ));
    }

    /**
     * Create insect particles around the user
     * Called during attack startup and execution
     */
    protected void createInsectParticles() {
        if (!(world instanceof ServerLevel serverLevel) || user == null) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create butterfly/insect particles around the user
        for (int i = 0; i < INSECT_PARTICLE_COUNT; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * INSECT_PARTICLE_SPREAD;
            double offsetY = world.random.nextDouble() * INSECT_PARTICLE_SPREAD;
            double offsetZ = (world.random.nextDouble() - 0.5) * INSECT_PARTICLE_SPREAD;

            Vec3 particlePos = userPos.add(offsetX, offsetY, offsetZ);

            // Mix of purple/pink particles for insect theme
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.1, 0.1, 0.1, 0.05);

            if (world.random.nextBoolean()) {
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.1, 0.1, 0.1, 0.02);
            }
        }

        // Create insect trail in look direction
        for (int i = 1; i <= 4; i++) {
            Vec3 trailPos = userPos.add(lookDir.scale(i * 0.6));
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    trailPos.x, trailPos.y, trailPos.z,
                    2, 0.1, 0.1, 0.1, 0.08);
        }
    }

    /**
     * Create insect particles at a specific hit location
     */
    protected void createInsectHitParticles(Vec3 hitPosition) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Burst of poison/witch particles at hit location
        serverLevel.sendParticles(ParticleTypes.WITCH,
                hitPosition.x, hitPosition.y + 1, hitPosition.z,
                15, 0.4, 0.4, 0.4, 0.2);

        // Portal particles for venom effect
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                hitPosition.x, hitPosition.y + 1, hitPosition.z,
                10, 0.3, 0.3, 0.3, 0.1);

        // Crit particles for precision
        serverLevel.sendParticles(ParticleTypes.CRIT,
                hitPosition.x, hitPosition.y + 0.5, hitPosition.z,
                8, 0.2, 0.2, 0.2, 0.1);
    }

    /**
     * Create a line of insect particles between two points
     * Used for dash attacks like Bee Sting
     */
    protected void createInsectTrail(Vec3 start, Vec3 end) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        Vec3 normalized = direction.normalize();

        // Create particles along the trail
        for (double d = 0; d <= distance; d += 0.4) {
            Vec3 particlePos = start.add(normalized.scale(d));

            serverLevel.sendParticles(ParticleTypes.WITCH,
                    particlePos.x, particlePos.y, particlePos.z,
                    2, 0.1, 0.1, 0.1, 0.08);

            if (d % 0.8 < 0.1) { // Every other particle position
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.05, 0.05, 0.05, 0.03);
            }
        }
    }

    /**
     * Create a circular insect swarm effect around a position
     * Used for area attacks and special effects
     */
    protected void createInsectSwarm(Vec3 center, float radius, int particleCount) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y + world.random.nextDouble() * 1.5;

            serverLevel.sendParticles(ParticleTypes.WITCH,
                    x, y, z, 1, 0.1, 0.1, 0.1, 0.05);

            // Add butterfly-like movement
            if (i % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }
    }

    /**
     * Create a poison burst effect
     * Used for venom attacks and poison clouds
     */
    protected void createPoisonBurst(Vec3 center, float intensity) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        int baseParticles = (int)(15 * intensity);

        // Central poison cloud
        serverLevel.sendParticles(ParticleTypes.WITCH,
                center.x, center.y + 1, center.z,
                baseParticles, 0.5, 0.5, 0.5, 0.2);

        // Ring of venom particles
        for (int ring = 1; ring <= 2; ring++) {
            float ringRadius = ring * intensity;
            int ringParticles = baseParticles / ring;

            for (int i = 0; i < ringParticles; i++) {
                double angle = (2 * Math.PI * i) / ringParticles;
                double x = center.x + Math.cos(angle) * ringRadius;
                double z = center.z + Math.sin(angle) * ringRadius;
                double y = center.y + world.random.nextDouble() * 2;

                serverLevel.sendParticles(ParticleTypes.WITCH,
                        x, y, z, 2, 0.2, 0.2, 0.2, 0.15);

                if (i % 3 == 0) {
                    serverLevel.sendParticles(ParticleTypes.PORTAL,
                            x, y, z, 1, 0.1, 0.1, 0.1, 0.08);
                }
            }
        }

        // Upward venom cloud
        serverLevel.sendParticles(ParticleTypes.WITCH,
                center.x, center.y + 1, center.z,
                (int)(25 * intensity), 0.8, 1.5, 0.8, 0.25);
    }

    /**
     * Create butterfly flutter effect
     * Used for agile movement and dash attacks
     */
    protected void createButterflyFlutter(Vec3 position) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Create fluttering butterfly particles
        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * 2 * Math.PI;
            double radius = 0.8 + Math.sin(angle * 3) * 0.2; // Wavy pattern

            double x = position.x + Math.cos(angle) * radius;
            double z = position.z + Math.sin(angle) * radius;
            double y = position.y + 0.5 + Math.sin(angle * 2) * 0.3; // Flutter up and down

            serverLevel.sendParticles(ParticleTypes.WITCH,
                    x, y, z, 1, 0.05, 0.05, 0.05, 0.02);

            // Add some sparkle
            if (i % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }
    }

    /**
     * Play insect-related sound effects
     */
    protected void playInsectSound() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BEE_LOOP, SoundSource.PLAYERS, 0.6f, 1.5f);
        }
    }

    /**
     * Play insect hit sound
     */
    protected void playInsectHitSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.SPIDER_HURT, SoundSource.PLAYERS, 0.8f, 1.8f);
        }
    }

    /**
     * Play poison effect sound
     */
    protected void playPoisonSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.7f, 1.3f);
        }
    }

    /**
     * Play dash sound for agile movements
     */
    protected void playDashSound() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 2.0f);
        }
    }

    // Insect Breathing specific utility methods
    /**
     * Check if this attack creates a poison cloud
     */
    public boolean isPoisonAttack() {
        // Override in specific attacks that create poison effects
        return true; // Most insect attacks apply poison
    }

    /**
     * Check if this attack is a precision/lock-on attack
     */
    public boolean isPrecisionAttack() {
        // Override in attacks like Butterfly and Dragonfly
        return false;
    }

    /**
     * Check if this attack is a dash/mobility attack
     */
    public boolean isDashAttack() {
        return hasDash() || hasTeleport();
    }

    /**
     * Check if this attack hits multiple targets in a line
     */
    public boolean isPiercingAttack() {
        // Override in attacks like Bee Sting
        return false;
    }

    /**
     * Check if this attack provides invincibility frames
     */
    public boolean hasInvincibilityFrames() {
        // All Insect attacks now have invincibility frames
        return true;
    }

    /**
     * Get the poison duration this attack applies (in ticks)
     */
    public int getPoisonDuration() {
        return Math.max(DEFAULT_POISON_DURATION, (int)(damage * 3));
    }

    /**
     * Get the poison amplifier (0-2) based on damage
     */
    public int getPoisonAmplifier() {
        return Math.min(2, Math.max(0, (int)(damage / 8.0f)));
    }

    /**
     * Called every tick during the attack (after windup period)
     * Implement the main Insect attack logic here (movement, damage, effects)
     */
    @Override
    protected abstract void perform();
}