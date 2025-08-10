package com.xirc.nichirin.common.attack.moves.sound;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for Sound Breathing attacks
 * Extends AbstractBreathingAttack for unified interface
 *
 * Sound-specific functionality includes explosive particles, combo stacking, and stunning
 * All Sound Breathing attacks build up momentum with consecutive hits and create particle explosions
 */
@SuppressWarnings("rawtypes")
public abstract class SoundBreathingAttackBase extends AbstractBreathingAttack<SoundBreathingAttackBase, IBreathingAttacker> {

    // Sound-specific properties
    private static final int DEFAULT_STUN_DURATION = 10; // 10 ticks stun
    private static final int SOUND_PARTICLE_COUNT = 8; // Reduced from 20
    private static final float SOUND_PARTICLE_SPREAD = 3.0f; // Increased from 2.0f
    private static final float COMBO_DAMAGE_BOOST = 0.05f; // 5% per hit
    private static final float COMBO_SPEED_BOOST = 0.05f; // 5% faster per hit

    // Track combo count per player
    private static final Map<UUID, Integer> playerComboCount = new HashMap<>();
    private static final Map<UUID, Long> lastHitTime = new HashMap<>();
    private static final int COMBO_TIMEOUT = 100; // 5 seconds to maintain combo

    /**
     * Apply Sound Breathing specific damage and effects to a target
     * Overrides the base hitTarget to add Sound-specific effects
     */
    @Override
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;

        // Apply combo bonus before hitting
        applyComboBonus();

        // Apply base damage first
        net.minecraft.world.damagesource.DamageSource source = user.damageSources().playerAttack(user);
        boolean damaged = target.hurt(source, damage);

        // Apply knockback BEFORE stun to ensure it takes effect
        if (knockback > 0) {
            // Mark entity for knockback grace period
            com.xirc.nichirin.common.effect.StunnedStatusEffect.markRecentKnockback(target);

            Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
            target.push(knockbackDir.x * knockback, 0.3, knockbackDir.z * knockback);
            target.hurtMarked = true;
            target.hasImpulse = true;
        }

        // Track hit first
        if (!getHitEntities().contains(target.getUUID())) {
            getHitEntities().add(target.getUUID());
            setHitCount(getHitCount() + 1);
        }

        // Apply stun effect AFTER knockback with a slight delay
        world.getServer().execute(() -> {
            if (target.isAlive() && hitStun > 0) {
                target.invulnerableTime = hitStun;

                // Apply actual stun effect
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.xirc.nichirin.registry.NichirinEffectRegistry.STUNNED.get(),
                        hitStun, // Duration in ticks
                        0, // Amplifier
                        false, // Ambient
                        true, // Show particles
                        true // Show icon
                ));
            }
        });

        // Increment combo count
        incrementCombo();

        // Create sound particles at target location
        createSoundHitParticles(target.position());

        // Check for combo explosion (every 4th hit)
        checkComboExplosion(target.position());

        // Play sound hit effects
        playSoundHitEffects(target.position());
    }

    /**
     * Special hit method for attacks with delayed explosions (like Tempo Breaker)
     * Doesn't create immediate particles
     */
    protected void hitTargetWithDelayedExplosion(LivingEntity target) {
        if (world.isClientSide) return;

        // Apply combo bonus before hitting
        applyComboBonus();

        // Apply base damage first
        net.minecraft.world.damagesource.DamageSource source = user.damageSources().playerAttack(user);
        boolean damaged = target.hurt(source, damage);

        // Apply knockback BEFORE stun to ensure it takes effect
        if (knockback > 0) {
            // Mark entity for knockback grace period
            com.xirc.nichirin.common.effect.StunnedStatusEffect.markRecentKnockback(target);

            Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
            target.push(knockbackDir.x * knockback, 0.3, knockbackDir.z * knockback);
            target.hurtMarked = true;
            target.hasImpulse = true;
        }

        // Track hit first
        if (!getHitEntities().contains(target.getUUID())) {
            getHitEntities().add(target.getUUID());
            setHitCount(getHitCount() + 1);
        }

        // Apply stun effect AFTER knockback with a slight delay
        world.getServer().execute(() -> {
            if (target.isAlive() && hitStun > 0) {
                target.invulnerableTime = hitStun;

                // Apply actual stun effect
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.xirc.nichirin.registry.NichirinEffectRegistry.STUNNED.get(),
                        hitStun, // Duration in ticks
                        0, // Amplifier
                        false, // Ambient
                        true, // Show particles
                        true // Show icon
                ));
            }
        });

        // Increment combo count
        incrementCombo();

        // DON'T create immediate sound particles - wait for delayed explosion
        // createSoundHitParticles(target.position()); // REMOVED for delayed attacks

        // Check for combo explosion (every 4th hit)
        checkComboExplosion(target.position());

        // Play sound hit effects
        playSoundHitEffects(target.position());
    }

    /**
     * Special Sound Breathing hit method that removes immunity frames
     * Used for multi-hit attacks like String Performance
     */
    @Override
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;

        // Apply combo bonus before hitting
        applyComboBonus();

        // Reset invulnerability to allow immediate damage
        target.invulnerableTime = 0;
        target.hurtTime = 0;

        // Apply damage
        net.minecraft.world.damagesource.DamageSource source = user.damageSources().playerAttack(user);
        boolean damaged = target.hurt(source, damage);

        // Apply knockback BEFORE stun
        if (knockback > 0) {
            // Mark entity for knockback grace period
            com.xirc.nichirin.common.effect.StunnedStatusEffect.markRecentKnockback(target);

            Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
            target.push(knockbackDir.x * knockback, 0.3, knockbackDir.z * knockback);
            target.hurtMarked = true;
            target.hasImpulse = true;
        }

        // Apply stun effect AFTER knockback with delay
        world.getServer().execute(() -> {
            if (target.isAlive() && hitStun > 0) {
                target.invulnerableTime = hitStun;

                // Apply actual stun effect
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.xirc.nichirin.registry.NichirinEffectRegistry.STUNNED.get(),
                        hitStun, // Duration in ticks
                        0, // Amplifier
                        false, // Ambient
                        true, // Show particles
                        true // Show icon
                ));
            }
        });

        // Increment combo count
        incrementCombo();

        // Create sound particles at target location
        createSoundHitParticles(target.position());

        // Check for combo explosion (every 4th hit)
        checkComboExplosion(target.position());

        // Play sound hit effects
        playSoundHitEffects(target.position());

        // Track hit (allow multiple hits for no-immunity attacks)
        setHitCount(getHitCount() + 1);
    }

    /**
     * Apply combo bonus to damage and speed
     */
    private void applyComboBonus() {
        if (user == null) return;

        int comboCount = getComboCount();
        if (comboCount > 0) {
            // Apply damage bonus (but don't make it too overpowered)
            float damageMultiplier = 1.0f + Math.min(comboCount * COMBO_DAMAGE_BOOST, 1.0f); // Cap at 100% bonus
            damage = damage * damageMultiplier;

            // Apply speed bonus to attack timing (reduce windup/duration)
            float speedMultiplier = 1.0f + Math.min(comboCount * COMBO_SPEED_BOOST, 0.5f); // Cap at 50% faster
            windup = Math.max(1, (int)(windup / speedMultiplier));
            duration = Math.max(1, (int)(duration / speedMultiplier));
        }
    }

    /**
     * Apply stun effect to target with a delay to allow knockback
     */
    protected void applyStunEffect(LivingEntity target) {
        // Skip creative mode players
        if (target instanceof net.minecraft.world.entity.player.Player player && player.isCreative()) {
            return;
        }

        // Apply stun effect with a 5-tick delay to allow knockback to take effect first
        world.getServer().execute(() -> {
            if (target.isAlive()) {
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                        DEFAULT_STUN_DURATION,
                        5, // Very strong slowness
                        false,
                        false
                ));

                // Also apply mining fatigue to prevent actions
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN,
                        DEFAULT_STUN_DURATION,
                        5,
                        false,
                        false
                ));
            }
        });
    }

    /**
     * Increment combo count for the user
     */
    private void incrementCombo() {
        if (user == null) return;

        UUID playerId = user.getUUID();
        long currentTime = world.getGameTime();

        // Reset combo if too much time has passed
        Long lastHit = lastHitTime.get(playerId);
        if (lastHit != null && currentTime - lastHit > COMBO_TIMEOUT) {
            playerComboCount.put(playerId, 0);
        }

        // Increment combo
        int currentCombo = playerComboCount.getOrDefault(playerId, 0);
        playerComboCount.put(playerId, currentCombo + 1);
        lastHitTime.put(playerId, currentTime);
    }

    /**
     * Get current combo count for the user
     */
    private int getComboCount() {
        if (user == null) return 0;
        return playerComboCount.getOrDefault(user.getUUID(), 0);
    }

    /**
     * Check if this hit triggers a combo explosion (every 4th hit)
     */
    private void checkComboExplosion(Vec3 position) {
        int comboCount = getComboCount();
        if (comboCount % 4 == 0 && comboCount > 0) {
            createComboExplosion(position);
        }
    }

    /**
     * Create sound particles around the user
     * Called during attack startup and execution
     */
    protected void createSoundParticles() {
        if (!(world instanceof ServerLevel serverLevel) || user == null) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        net.minecraft.util.RandomSource random = serverLevel.getRandom();

        // Create all particle types except thunder, including blue flash particles
        for (int i = 0; i < SOUND_PARTICLE_COUNT; i++) {
            double offsetX = (random.nextDouble() - 0.5) * SOUND_PARTICLE_SPREAD;
            double offsetY = random.nextDouble() * SOUND_PARTICLE_SPREAD;
            double offsetZ = (random.nextDouble() - 0.5) * SOUND_PARTICLE_SPREAD;

            Vec3 particlePos = userPos.add(offsetX, offsetY, offsetZ);

            // Mix of different particles including blue flash
            if (i % 6 == 0) {
                serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.1, 0.1, 0.1, 0.05);
            } else if (i % 6 == 1) {
                serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.1, 0.1, 0.1, 0.05);
            } else if (i % 6 == 2) {
                serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.1, 0.1, 0.1, 0.05);
            } else if (i % 6 == 3) {
                serverLevel.sendParticles(NichirinParticleRegistry.FLASH2.get(),
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.1, 0.1, 0.1, 0.05);
            } else if (i % 6 == 4) {
                serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH1.get(),
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.1, 0.1, 0.1, 0.05);
            } else if (i % 6 == 5) {
                serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH2.get(),
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.1, 0.1, 0.1, 0.05);
            } else {
                // Mix in some vanilla particles
                serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    /**
     * Create sound particles at a specific hit location
     */
    protected void createSoundHitParticles(Vec3 hitPosition) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Much reduced particle explosion at hit location with blue flash
        serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                hitPosition.x, hitPosition.y + 1, hitPosition.z,
                2, 1.0, 1.0, 1.0, 0.2);

        // Shockwave particles
        serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                hitPosition.x, hitPosition.y + 0.5, hitPosition.z,
                2, 0.8, 0.8, 0.8, 0.1);

        // Blue flash particles for impact
        serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH1.get(),
                hitPosition.x, hitPosition.y + 0.5, hitPosition.z,
                2, 0.5, 0.5, 0.5, 0.1);
    }

    /**
     * Create a combo explosion with enhanced particles and damage
     */
    protected void createComboExplosion(Vec3 center) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Much more spread out and fewer particles with blue flash
        serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                center.x, center.y + 1, center.z,
                6, 6.0, 3.0, 6.0, 0.3); // Much wider spread (12 block radius)

        serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                center.x, center.y + 1, center.z,
                4, 5.0, 2.5, 5.0, 0.2); // Wider spread

        serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH1.get(),
                center.x, center.y + 1, center.z,
                4, 4.0, 2.0, 4.0, 0.15); // Blue flash for combo explosion

        serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH2.get(),
                center.x, center.y + 1, center.z,
                4, 4.0, 2.0, 4.0, 0.15); // Blue flash for combo explosion

        // Single vanilla explosion effect
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                center.x, center.y + 1, center.z,
                1, 0.5, 0.5, 0.5, 0);

        // Deal explosion damage to nearby entities
        java.util.List<LivingEntity> nearbyTargets = world.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(center.subtract(3, 3, 3), center.add(3, 3, 3)),
                entity -> entity != user && entity.isAlive());

        for (LivingEntity target : nearbyTargets) {
            // Moderate explosion damage
            target.hurt(world.damageSources().explosion(null, user), damage * 0.3f);

            // Apply stun
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                    DEFAULT_STUN_DURATION,
                    5,
                    false,
                    false
            ));
        }

        // Big explosion sound
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.8f);
    }

    /**
     * Create a trail of sound particles between two points
     */
    protected void createSoundTrail(Vec3 start, Vec3 end) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        Vec3 normalized = direction.normalize();

        // Create particles along the trail
        for (double d = 0; d <= distance; d += 0.3) {
            Vec3 particlePos = start.add(normalized.scale(d));

            serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                    particlePos.x, particlePos.y, particlePos.z,
                    2, 0.1, 0.1, 0.1, 0.05);

            if (d % 1.0 < 0.1) {
                serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.05, 0.05, 0.05, 0.02);
            }
        }
    }

    /**
     * Create a circular sound shockwave around a position
     */
    protected void createSoundShockwave(Vec3 center, float radius, int particleCount) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y;

            serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                    x, y, z, 3, 0.1, 0.1, 0.1, 0.1);

            if (i % 2 == 0) {
                serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                        x, y + 0.5, z, 1, 0.05, 0.1, 0.05, 0.05);
            }
        }
    }

    /**
     * Play sound-related effects
     */
    protected void playSoundEffects() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.0f);
        }
    }

    /**
     * Play sound hit effects
     */
    protected void playSoundHitEffects(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 0.6f, 1.2f);
        }
    }

    /**
     * Reset combo for a player (called on logout or death)
     */
    public static void resetCombo(UUID playerId) {
        playerComboCount.remove(playerId);
        lastHitTime.remove(playerId);
    }

    // Sound Breathing specific utility methods

    /**
     * Check if this attack creates shockwaves
     */
    public boolean isShockwaveAttack() {
        // Override in specific attacks that create shockwaves
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
        // Override in attacks like Constant Resounding Slashes
        return false;
    }

    /**
     * Get the stun duration this attack applies (in ticks)
     */
    public int getStunDuration() {
        return DEFAULT_STUN_DURATION;
    }

    // Abstract methods that Sound attacks must implement

    /**
     * Called when the Sound attack starts (after breath consumption)
     * Implement Sound-specific startup effects here (particles, sounds, positioning)
     */
    @Override
    protected abstract void onStart();

    /**
     * Called every tick during the attack (after windup period)
     * Implement the main Sound attack logic here (movement, damage, effects)
     */
    @Override
    protected abstract void perform();

    /**
     * Called when the Sound attack ends (optional override)
     * Implement Sound-specific cleanup logic here
     */
    @Override
    protected void onStop() {
        super.onStop();
        // Additional Sound-specific cleanup can be added here
    }
}