package com.xirc.nichirin.common.attack.moves.breathing.flame;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;

// Base for Flame Breathing attacks. All hits apply fire and burning effects.
@SuppressWarnings("rawtypes")
public abstract class FlameBreathingAttackBase extends AbstractBreathingAttack<FlameBreathingAttackBase, IBreathingAttacker> {

    // Flame-specific properties
    private static final int DEFAULT_FIRE_DURATION = 3; // 3 seconds of fire
    private static final int FLAME_PARTICLE_COUNT = 15;
    private static final float FLAME_PARTICLE_SPREAD = 1.5f;

    @Override
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;
        super.hitTarget(target);
        applyFireEffect(target);
        createFlameHitParticles(target.position());
        playFlameHitSound(target.position());
    }

    @Override
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;
        super.hitTargetNoImmunity(target);
        applyFireEffect(target);
        createFlameHitParticles(target.position());
        playFlameHitSound(target.position());
    }

    /**
     * Apply fire effect to target
     * Uses both direct fire application AND Burning status effect
     */
    protected void applyFireEffect(LivingEntity target) {
        // Skip fire immune entities (Blazes, Ghasts, etc.)
        if (target.fireImmune()) {
            return;
        }

        // Skip creative mode players - they shouldn't be affected by fire
        if (target instanceof Player player && player.isCreative()) {
            return;
        }

        // Calculate fire duration based on damage - but keep it reasonable
        // Max 8 seconds even for high damage attacks
        int fireSeconds = Math.min(8, Math.max(DEFAULT_FIRE_DURATION, (int)(damage / 4.0f)));

        // Apply direct fire (we know this works from debug)
        target.igniteForSeconds(fireSeconds);
        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), fireSeconds * 20));

        int burningDurationTicks = fireSeconds * 20;
        target.addEffect(new MobEffectInstance(
                NichirinEffectRegistry.burning(),
                burningDurationTicks,
                0, false, true
        ));
    }

    protected void createFlameParticles() {
        if (!(world instanceof ServerLevel serverLevel) || user == null) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();
        RandomSource random = serverLevel.getRandom();

        for (int i = 0; i < FLAME_PARTICLE_COUNT; i++) {
            double offsetX = (random.nextDouble() - 0.5) * FLAME_PARTICLE_SPREAD;
            double offsetY = random.nextDouble() * FLAME_PARTICLE_SPREAD;
            double offsetZ = (random.nextDouble() - 0.5) * FLAME_PARTICLE_SPREAD;

            Vec3 particlePos = userPos.add(offsetX, offsetY, offsetZ);

            serverLevel.sendParticles(ParticleTypes.FLAME,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.1, 0.1, 0.1, 0.05);

            if (random.nextBoolean()) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.1, 0.1, 0.1, 0.02);
            }
        }

        for (int i = 1; i <= 5; i++) {
            Vec3 trailPos = userPos.add(lookDir.scale(i * 0.8));
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    trailPos.x, trailPos.y, trailPos.z,
                    2, 0.2, 0.2, 0.2, 0.1);
        }
    }

    protected void createFlameHitParticles(Vec3 hitPosition) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        serverLevel.sendParticles(ParticleTypes.FLAME,
                hitPosition.x, hitPosition.y + 1, hitPosition.z,
                20, 0.5, 0.5, 0.5, 0.2);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                hitPosition.x, hitPosition.y + 1, hitPosition.z,
                8, 0.3, 0.3, 0.3, 0.1);
        serverLevel.sendParticles(ParticleTypes.LAVA,
                hitPosition.x, hitPosition.y + 0.5, hitPosition.z,
                5, 0.2, 0.2, 0.2, 0.1);
    }

    /**
     * Create a line of flame particles between two points
     * Used for slash attacks and dashes
     */
    protected void createFlameTrail(Vec3 start, Vec3 end) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        Vec3 normalized = direction.normalize();

        // Create particles along the trail
        for (double d = 0; d <= distance; d += 0.5) {
            Vec3 particlePos = start.add(normalized.scale(d));

            serverLevel.sendParticles(ParticleTypes.FLAME,
                    particlePos.x, particlePos.y, particlePos.z,
                    3, 0.2, 0.2, 0.2, 0.1);

            if (d % 1.0 < 0.1) { // Every other particle position
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    /**
     * Create a circular flame effect around a position
     * Used for AOE attacks and explosions
     */
    protected void createFlameCircle(Vec3 center, float radius, int particleCount) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Use ServerLevel's random instead of world.random to avoid threading issues
        RandomSource random = serverLevel.getRandom();

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y + random.nextDouble() * 2;

            serverLevel.sendParticles(ParticleTypes.FLAME,
                    x, y, z, 2, 0.2, 0.2, 0.2, 0.1);

            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    /**
     * Create a flame explosion effect
     * Used for finishing moves and high-impact attacks
     */
    protected void createFlameExplosion(Vec3 center, float intensity) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Use ServerLevel's random instead of world.random to avoid threading issues
        RandomSource random = serverLevel.getRandom();

        int baseParticles = (int)(20 * intensity);

        // Central explosion
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                center.x, center.y + 1, center.z,
                1, 0, 0, 0, 0);

        // Ring of flames
        for (int ring = 1; ring <= 3; ring++) {
            float ringRadius = ring * intensity;
            int ringParticles = baseParticles / ring;

            for (int i = 0; i < ringParticles; i++) {
                double angle = (2 * Math.PI * i) / ringParticles;
                double x = center.x + Math.cos(angle) * ringRadius;
                double z = center.z + Math.sin(angle) * ringRadius;
                double y = center.y + random.nextDouble() * 3;

                serverLevel.sendParticles(ParticleTypes.FLAME,
                        x, y, z, 3, 0.3, 0.3, 0.3, 0.2);
            }
        }

        // Upward flame burst
        serverLevel.sendParticles(ParticleTypes.FLAME,
                center.x, center.y + 1, center.z,
                (int)(50 * intensity), 1.0, 2.0, 1.0, 0.3);

        // Smoke cloud
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                center.x, center.y + 2, center.z,
                (int)(30 * intensity), 2.0, 1.0, 2.0, 0.1);
    }

    /**
     * Play flame-related sound effects
     */
    protected void playFlameSound() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 1.0f);
        }
    }

    /**
     * Play flame hit sound
     */
    protected void playFlameHitSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 1.5f);
        }
    }

    /**
     * Play flame explosion sound
     */
    protected void playFlameExplosionSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }

    // Flame Breathing specific utility methods

    /**
     * Check if this attack creates an explosion
     */
    public boolean isExplosiveAttack() {
        // Override in specific attacks that create explosions
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
        // Override in attacks like Blooming Flame Undulation
        return false;
    }

    /**
     * Get the fire duration this attack applies (in seconds)
     */
    public int getFireDuration() {
        return Math.max(DEFAULT_FIRE_DURATION, (int)(damage / 2.0f));
    }

    // Abstract methods that Flame attacks must implement
    // These are inherited from AbstractBreathingAttack but documented here for clarity

    /**
     * Called when the Flame attack starts (after breath consumption)
     * Implement Flame-specific startup effects here (particles, sounds, positioning)
     */
    @Override
    protected abstract void onStart();

    /**
     * Called every tick during the attack (after windup period)
     * Implement the main Flame attack logic here (movement, damage, effects)
     */
    @Override
    protected abstract void perform();

    /**
     * Called when the Flame attack ends (optional override)
     * Implement Flame-specific cleanup logic here
     */
    @Override
    protected void onStop() {
        super.onStop();
        // Additional Flame-specific cleanup can be added here
    }
}