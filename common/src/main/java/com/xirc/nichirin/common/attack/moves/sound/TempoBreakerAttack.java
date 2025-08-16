package com.xirc.nichirin.common.attack.moves.sound;

import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tempo Breaker (Right Click Attack)
 * Basic slash, knocks enemies back far and deals good damage.
 * Particle explosion at the hit entity's position 2 seconds after the attack.
 *
 * Mechanics:
 * - 6 block knockback
 * - Large sweep
 * - Delayed explosion effect
 *
 * All configuration comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class TempoBreakerAttack extends SoundBreathingAttackBase {

    private boolean hasExecuted = false;
    private final Map<LivingEntity, Long> delayedExplosions = new HashMap<>();
    private static final int EXPLOSION_DELAY = 40; // 2 seconds (40 ticks)

    public TempoBreakerAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        hasExecuted = false;
        delayedExplosions.clear();

        // Tempo buildup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.8f, 1.3f);

        // Create initial particle buildup
        createSoundParticles();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Execute the tempo breaking slash once after windup completes
        if (!hasExecuted && tickCount == windup + 1) {
            executeTempoBreaker();
            hasExecuted = true;
        }

        // ALWAYS check for delayed explosions every tick during the entire attack duration
        if (tickCount > windup) {
            checkDelayedExplosions();
        }
    }

    private void executeTempoBreaker() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create wide sweep effect
        createTempoSweepEffect();

        // Hit enemies in a wide arc in front of the user
        List<LivingEntity> targets = getTargetsInCone(userPos, lookDir, range, 90); // 90-degree cone

        for (LivingEntity target : targets) {
            hitTarget(target);

            applyDisorientedEffect(target);


            // Massive knockback (6 blocks) - this was missing proper implementation
            Vec3 knockbackDir = target.position().subtract(userPos).normalize();
            Vec3 massiveKnockback = knockbackDir.scale(2.5); // 6 block knockback
            target.setDeltaMovement(massiveKnockback.x, 0.4, massiveKnockback.z); // Set velocity directly
            target.hurtMarked = true;
            target.hasImpulse = true;

            // Schedule delayed explosion for this target ONLY
            long explosionTime = world.getGameTime() + EXPLOSION_DELAY;
            delayedExplosions.put(target, explosionTime);

            // Hit sound
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.2f);
        }

        // Only play sweep sounds if we actually hit something
        if (!targets.isEmpty()) {
            // Main slash sound
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.8f);

            // Tempo break sound effect
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 1.0f, 1.5f);
        }
    }

    private void checkDelayedExplosions() {
        long currentTime = world.getGameTime();

        // Check each scheduled explosion
        delayedExplosions.entrySet().removeIf(entry -> {
            LivingEntity target = entry.getKey();
            long explosionTime = entry.getValue();

            if (currentTime >= explosionTime) {
                if (target.isAlive() && target.getTags().contains("tempo_breaker_marked")) {
                    // Remove the tag
                    target.removeTag("tempo_breaker_marked");

                    // Use target's CURRENT position for explosion
                    Vec3 explosionPos = target.position();
                    executeDelayedExplosion(explosionPos);

                    // Deal additional explosion damage
                    target.hurt(world.damageSources().explosion(null, user), damage * 0.5f);
                }
                return true; // Remove this entry
            }
            return false; // Keep this entry
        });
    }

    private void executeDelayedExplosion(Vec3 position) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Delayed explosion effect - much fewer particles
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                position.x, position.y + 1, position.z,
                1, 0, 0, 0, 0);

        // Sound particle explosion - greatly reduced
        serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                position.x, position.y + 1, position.z,
                6, 2.0, 2.0, 2.0, 0.2);

        serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                position.x, position.y + 0.5, position.z,
                4, 1.5, 1.5, 1.5, 0.15);

        serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                position.x, position.y + 1, position.z,
                3, 1.0, 1.0, 1.0, 0.1);

        serverLevel.sendParticles(NichirinParticleRegistry.FLASH2.get(),
                position.x, position.y + 1, position.z,
                3, 1.0, 1.0, 1.0, 0.1);

        // Delayed explosion sound
        world.playSound(null, position.x, position.y, position.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.2f);

        world.playSound(null, position.x, position.y, position.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    /**
     * Create the wide tempo-breaking sweep effect - slash pattern in front of player
     */
    private void createTempoSweepEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();

        // Create slash effect starting in front of the player
        Vec3 slashCenter = userPos.add(lookDir.scale(2.5)); // Start 2.5 blocks in front of player

        // Create wide sweep arc - particles form a slash pattern
        for (int i = -45; i <= 45; i += 10) { // Every 10 degrees for good coverage
            double angle = Math.toRadians(i);
            Vec3 sweepDir = lookDir.scale(Math.cos(angle)).add(rightDir.scale(Math.sin(angle)));

            // Create particles along the slash arc at varying distances
            for (double r = 0.5; r <= range - 2.0; r += 0.8) { // Start from slash center, not player
                Vec3 sweepPos = slashCenter.add(sweepDir.scale(r));

                serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                        sweepPos.x, sweepPos.y, sweepPos.z,
                        1, 0.1, 0.1, 0.1, 0.03);
            }
        }

        // Create horizontal slash trail effect
        for (double t = -2.0; t <= 2.0; t += 0.4) { // Horizontal slash line
            Vec3 slashPos = slashCenter.add(rightDir.scale(t));

            serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                    slashPos.x, slashPos.y, slashPos.z,
                    2, 0.2, 0.2, 0.2, 0.1);
        }

        // Add some depth to the slash with a second layer
        Vec3 slashCenter2 = userPos.add(lookDir.scale(3.5)); // Slightly further out
        for (double t = -1.5; t <= 1.5; t += 0.5) {
            Vec3 slashPos = slashCenter2.add(rightDir.scale(t));

            serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                    slashPos.x, slashPos.y, slashPos.z,
                    1, 0.3, 0.3, 0.3, 0.05);
        }

        // Ground impact effect at the end of the slash
        Vec3 groundImpact = slashCenter.add(lookDir.scale(1.5)).add(0, -1, 0);
        serverLevel.sendParticles(ParticleTypes.POOF,
                groundImpact.x, groundImpact.y, groundImpact.z,
                3, 0.8, 0.1, 0.8, 0.15);
    }

    /**
     * Get targets in a cone shape
     */
    private List<LivingEntity> getTargetsInCone(Vec3 origin, Vec3 direction, double range, double angleDegrees) {
        double angleRadians = Math.toRadians(angleDegrees / 2);

        return world.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(origin.subtract(range, 2, range), origin.add(range, 2, range)),
                entity -> {
                    if (entity == user || !entity.isAlive()) return false;

                    Vec3 toEntity = entity.position().subtract(origin).normalize();
                    double dot = direction.dot(toEntity);
                    double angle = Math.acos(Math.max(-1.0, Math.min(1.0, dot)));

                    return angle <= angleRadians && origin.distanceTo(entity.position()) <= range;
                });
    }

    @Override
    protected void onStop() {
        // Reset state
        hasExecuted = false;

        // Note: We don't clear delayedExplosions here because they should continue
        // to trigger even after the attack ends

        // Final tempo echo
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.6f, 0.8f);
        }
    }
}