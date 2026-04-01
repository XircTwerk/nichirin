/**
 * Create a TNT-like explosion at the target position - PERFORMANCE FIXED
 */
package com.xirc.nichirin.common.attack.moves.breathing.sound;

import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tempo Breaker (Right Click Attack)
 * Creates TNT-like explosions at hit entities 2 seconds after impact
 */
public class TempoBreakerAttack extends SoundBreathingAttackBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(TempoBreakerAttack.class);

    private boolean hasExecuted = false;

    // Track pending explosions - much more efficient than recursive scheduling
    private static final Map<UUID, PendingExplosion> PENDING_EXPLOSIONS = new HashMap<>();

    private static class PendingExplosion {
        final Vec3 position;
        final LivingEntity target;
        int ticksRemaining;

        PendingExplosion(Vec3 position, LivingEntity target, int delay) {
            this.position = position;
            this.target = target;
            this.ticksRemaining = delay;
        }
    }

    public TempoBreakerAttack() {
        // Configuration comes from moveset
    }

    @Override
    protected void onStart() {
        hasExecuted = false;

        // Tempo buildup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.8f, 1.3f);

        createSoundParticles();
    }

    @Override
    protected void perform() {
        if (world == null || world.isClientSide) return;

        try {
            // Execute the tempo breaking slash once after windup completes
            if (!hasExecuted && tickCount == windup + 1) {
                executeTempoBreaker();
                hasExecuted = true;
            }

            // Handle pending explosions efficiently
            processPendingExplosions();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void executeTempoBreaker() {
        try {
            if (user == null) {
                return;
            }

            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
            Vec3 lookDir = user.getLookAngle();

            createTempoSweepEffect();

            // Hit enemies in a wide arc
            List<LivingEntity> targets = getTargetsInCone(userPos, lookDir, range, 90);

            for (LivingEntity target : targets) {
                if (target == null) continue; // Skip null targets

                hitTarget(target);
                applyDisorientedEffect(target);

                // Massive knockback
                Vec3 knockbackDir = target.position().subtract(userPos).normalize();
                Vec3 massiveKnockback = knockbackDir.scale(2.5);
                target.setDeltaMovement(massiveKnockback.x, 0.4, massiveKnockback.z);
                target.hurtMarked = true;
                target.hasImpulse = true;

                // Schedule explosion using efficient tracking system
                PENDING_EXPLOSIONS.put(target.getUUID(),
                        new PendingExplosion(target.position(), target, 40));

                // Hit sound
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.2f);
            }

            if (!targets.isEmpty()) {
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.8f);
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 1.0f, 1.5f);
            }
        } catch (Exception e) {
            LOGGER.error("TempoBreakerAttack error in executeTempoBreaker()", e);
        }
    }

    /**
     * Process all pending explosions each tick - NO BLOCKING!
     */
    private void processPendingExplosions() {
        if (PENDING_EXPLOSIONS.isEmpty()) return;

        try {
            // Process all pending explosions this tick
            PENDING_EXPLOSIONS.entrySet().removeIf(entry -> {
                try {
                    PendingExplosion explosion = entry.getValue();
                    if (explosion == null) return true; // Remove null entries

                    explosion.ticksRemaining--;

                    // Add warning particles as explosion approaches
                    if (explosion.ticksRemaining <= 20 && explosion.ticksRemaining > 0) {
                        addExplosionWarningEffects(explosion.position, explosion.ticksRemaining);
                    }

                    // Time to explode!
                    if (explosion.ticksRemaining <= 0) {
                        if (explosion.target != null && explosion.target.isAlive()) {
                            // Update position to where target is now
                            createTNTExplosion(explosion.target.position());
                        } else {
                            // Target died or is null, explode at original position
                            createTNTExplosion(explosion.position);
                        }
                        return true; // Remove from map
                    }

                    return false; // Keep in map
                } catch (Exception e) {
                    LOGGER.warn("Error processing pending explosion: {}", e.getMessage());
                    return true; // Remove problematic entry
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error in processPendingExplosions", e);
        }
    }

    /**
     * Add warning effects before explosion
     */
    private void addExplosionWarningEffects(Vec3 position, int ticksRemaining) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Intensifying warning particles
        int intensity = Math.max(1, 21 - ticksRemaining); // 1-20 intensity

        // Pulsing red particles
        if (ticksRemaining % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    position.x, position.y + 1, position.z,
                    intensity / 2, 0.5, 0.5, 0.5, 0.1);
        }

        // Warning sound at 1 second and 0.5 seconds
        if (ticksRemaining == 20 || ticksRemaining == 10) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 0.8f, 1.5f);
        }
    }

    private void createTNTExplosion(Vec3 position) {
        if (world.isClientSide) return;

        // Create actual explosion like TNT
        world.explode(null, position.x, position.y, position.z, 3.0f, net.minecraft.world.level.Level.ExplosionInteraction.NONE);

        // OPTIMIZED: Reduce particle spam by limiting particles
        if (world instanceof ServerLevel serverLevel) {
            // Single large explosion emitter
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    position.x, position.y + 0.5, position.z,
                    1, 0.0, 0.0, 0.0, 0.0);

            // Reduced shockwave particles (was 16, now 8)
            for (int i = 0; i < 8; i++) {
                double angle = (i / 8.0) * 2 * Math.PI;
                double radius = 3.0;
                double x = position.x + Math.cos(angle) * radius;
                double z = position.z + Math.sin(angle) * radius;
                double y = position.y;

                serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                        x, y, z, 2, 0.1, 0.1, 0.1, 0.1); // Reduced count
            }

            // Reduced flash particles (was 8+8, now 4+4)
            serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                    position.x, position.y + 1, position.z,
                    4, 1.0, 1.0, 1.0, 0.2);

            serverLevel.sendParticles(NichirinParticleRegistry.FLASH2.get(),
                    position.x, position.y + 1, position.z,
                    4, 1.0, 1.0, 1.0, 0.2);

            // Reduced sound particles (was 12, now 6)
            for (int i = 0; i < 6; i++) {
                double angle = (i / 6.0) * 2 * Math.PI;
                double radius = 2.5;
                double x = position.x + Math.cos(angle) * radius;
                double z = position.z + Math.sin(angle) * radius;
                double y = position.y + 0.5;

                serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                        x, y, z, 1, 0.3, 0.3, 0.3, 0.1); // Reduced count
            }

            // Reduced blue flash particles (was 6+6, now 3+3)
            serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH1.get(),
                    position.x, position.y + 1, position.z,
                    3, 0.8, 0.8, 0.8, 0.15);

            serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH2.get(),
                    position.x, position.y + 1, position.z,
                    3, 0.8, 0.8, 0.8, 0.15);
        }

        // Explosion sounds
        world.playSound(null, position.x, position.y, position.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5f, 1.0f);

        world.playSound(null, position.x, position.y, position.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    /**
     * Create the wide tempo-breaking sweep effect
     */
    private void createTempoSweepEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 slashCenter = userPos.add(lookDir.scale(2.5));

        // Create sweep arc - OPTIMIZED: Reduced iterations
        for (int i = -45; i <= 45; i += 15) { // Was 10, now 15 - fewer particles
            double angle = Math.toRadians(i);
            Vec3 sweepDir = lookDir.scale(Math.cos(angle)).add(rightDir.scale(Math.sin(angle)));

            for (double r = 0.5; r <= range - 2.0; r += 1.2) { // Was 0.8, now 1.2 - fewer particles
                Vec3 sweepPos = slashCenter.add(sweepDir.scale(r));

                if (tickCount < windup + duration) {
                    serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                            sweepPos.x, sweepPos.y, sweepPos.z,
                            1, 0.1, 0.1, 0.1, 0.03);
                }
            }
        }

        // Slash trail - OPTIMIZED: Reduced spacing
        for (double t = -2.0; t <= 2.0; t += 0.6) { // Was 0.4, now 0.6 - fewer particles
            Vec3 slashPos = slashCenter.add(rightDir.scale(t));
            serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                    slashPos.x, slashPos.y, slashPos.z,
                    1, 0.2, 0.2, 0.2, 0.1); // Reduced from 2 to 1 particle
        }
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
        hasExecuted = false;
        // Don't clear pending explosions - let them finish naturally

        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.6f, 0.8f);
        }
    }

    /**
     * IMPORTANT: Call this method from your main mod's tick handler
     * to process pending explosions even when the attack is no longer active.
     *
     * Add this to your main mod class or tick event handler:
     *
     * @SubscribeEvent
     * public void onServerTick(TickEvent.ServerTickEvent event) {
     *     if (event.phase == TickEvent.Phase.END) {
     *         TempoBreakerAttack.processPendingExplosionsGlobal(server);
     *     }
     * }
     */
    public static void processPendingExplosionsGlobal(net.minecraft.server.MinecraftServer server) {
        if (PENDING_EXPLOSIONS.isEmpty()) return;

        // Process all pending explosions across all worlds
        PENDING_EXPLOSIONS.entrySet().removeIf(entry -> {
            PendingExplosion explosion = entry.getValue();
            explosion.ticksRemaining--;

            // Add warning particles as explosion approaches
            if (explosion.ticksRemaining <= 20 && explosion.ticksRemaining > 0) {
                if (explosion.target.level() instanceof ServerLevel serverLevel) {
                    addGlobalExplosionWarningEffects(serverLevel, explosion.position, explosion.ticksRemaining);
                }
            }

            // Time to explode!
            if (explosion.ticksRemaining <= 0) {
                if (explosion.target.isAlive()) {
                    createGlobalTNTExplosion(explosion.target.level(), explosion.target.position());
                } else {
                    createGlobalTNTExplosion(explosion.target.level(), explosion.position);
                }
                return true; // Remove from map
            }

            return false; // Keep in map
        });
    }

    private static void addGlobalExplosionWarningEffects(ServerLevel world, Vec3 position, int ticksRemaining) {
        int intensity = Math.max(1, 21 - ticksRemaining);

        if (ticksRemaining % 5 == 0) {
            world.sendParticles(ParticleTypes.FLAME,
                    position.x, position.y + 1, position.z,
                    intensity / 2, 0.5, 0.5, 0.5, 0.1);
        }

        if (ticksRemaining == 20 || ticksRemaining == 10) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 0.8f, 1.5f);
        }
    }

    private static void createGlobalTNTExplosion(net.minecraft.world.level.Level world, Vec3 position) {
        if (world.isClientSide) return;

        world.explode(null, position.x, position.y, position.z, 3.0f,
                net.minecraft.world.level.Level.ExplosionInteraction.NONE);

        if (world instanceof ServerLevel serverLevel) {
            // Reduced particle effects to prevent lag
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    position.x, position.y + 0.5, position.z, 1, 0.0, 0.0, 0.0, 0.0);

            // Simple shockwave
            for (int i = 0; i < 8; i++) {
                double angle = (i / 8.0) * 2 * Math.PI;
                double radius = 3.0;
                double x = position.x + Math.cos(angle) * radius;
                double z = position.z + Math.sin(angle) * radius;

                serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                        x, position.y, z, 1, 0.1, 0.1, 0.1, 0.1);
            }
        }

        world.playSound(null, position.x, position.y, position.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5f, 1.0f);
    }
}