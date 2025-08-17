/**
 * Create a TNT-like explosion at the target position
 */package com.xirc.nichirin.common.attack.moves.sound;

import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Tempo Breaker (Right Click Attack)
 * Creates TNT-like explosions at hit entities 2 seconds after impact
 */
public class TempoBreakerAttack extends SoundBreathingAttackBase {

    private boolean hasExecuted = false;

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
        if (world.isClientSide) return;

        // Execute the tempo breaking slash once after windup completes
        if (!hasExecuted && tickCount == windup + 1) {
            executeTempoBreaker();
            hasExecuted = true;
        }
    }

    private void executeTempoBreaker() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        createTempoSweepEffect();

        // Hit enemies in a wide arc
        List<LivingEntity> targets = getTargetsInCone(userPos, lookDir, range, 90);

        for (LivingEntity target : targets) {
            hitTarget(target);
            applyDisorientedEffect(target);

            // Massive knockback
            Vec3 knockbackDir = target.position().subtract(userPos).normalize();
            Vec3 massiveKnockback = knockbackDir.scale(2.5);
            target.setDeltaMovement(massiveKnockback.x, 0.4, massiveKnockback.z);
            target.hurtMarked = true;
            target.hasImpulse = true;

            // Schedule explosion using server scheduler with proper delay
            world.getServer().execute(() -> {
                // Schedule a task to run after 40 ticks
                scheduleDelayedExplosion(target, 40);
            });

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
    }

    /**
     * Schedule a delayed explosion using a simple countdown
     */
    private void scheduleDelayedExplosion(LivingEntity target, int delay) {
        if (delay <= 0) {
            // Time to explode!
            if (target.isAlive()) {
                createTNTExplosion(target.position());
            }
        } else {
            // Wait one more tick and schedule again with delay-1
            world.getServer().execute(() -> {
                try {
                    Thread.sleep(50); // Wait 1 tick (50ms)
                    scheduleDelayedExplosion(target, delay - 1);
                } catch (InterruptedException e) {
                    // If interrupted, just explode now
                    if (target.isAlive()) {
                        createTNTExplosion(target.position());
                    }
                }
            });
        }
    }
    private void createTNTExplosion(Vec3 position) {
        if (world.isClientSide) return;

        // Create actual explosion like TNT
        world.explode(null, position.x, position.y, position.z, 3.0f, net.minecraft.world.level.Level.ExplosionInteraction.NONE);

        // Additional visual effects with mod particles
        if (world instanceof ServerLevel serverLevel) {
            // Large explosion emitter
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    position.x, position.y + 0.5, position.z,
                    1, 0.0, 0.0, 0.0, 0.0);

            // Shockwave particles in a ring
            for (int i = 0; i < 16; i++) {
                double angle = (i / 16.0) * 2 * Math.PI;
                double radius = 3.0;
                double x = position.x + Math.cos(angle) * radius;
                double z = position.z + Math.sin(angle) * radius;
                double y = position.y;

                serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                        x, y, z, 3, 0.1, 0.1, 0.1, 0.1);
            }

            // Flash particles for the explosion burst
            serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                    position.x, position.y + 1, position.z,
                    8, 1.0, 1.0, 1.0, 0.2);

            serverLevel.sendParticles(NichirinParticleRegistry.FLASH2.get(),
                    position.x, position.y + 1, position.z,
                    8, 1.0, 1.0, 1.0, 0.2);

            // Sound particles radiating outward
            for (int i = 0; i < 12; i++) {
                double angle = (i / 12.0) * 2 * Math.PI;
                double radius = 2.5;
                double x = position.x + Math.cos(angle) * radius;
                double z = position.z + Math.sin(angle) * radius;
                double y = position.y + 0.5;

                serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                        x, y, z, 2, 0.3, 0.3, 0.3, 0.1);
            }

            // Blue flash particles for extra impact
            serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH1.get(),
                    position.x, position.y + 1, position.z,
                    6, 0.8, 0.8, 0.8, 0.15);

            serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH2.get(),
                    position.x, position.y + 1, position.z,
                    6, 0.8, 0.8, 0.8, 0.15);
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

        // Create sweep arc
        for (int i = -45; i <= 45; i += 10) {
            double angle = Math.toRadians(i);
            Vec3 sweepDir = lookDir.scale(Math.cos(angle)).add(rightDir.scale(Math.sin(angle)));

            for (double r = 0.5; r <= range - 2.0; r += 0.8) {
                Vec3 sweepPos = slashCenter.add(sweepDir.scale(r));

                if (tickCount < windup + duration) {
                    serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                            sweepPos.x, sweepPos.y, sweepPos.z,
                            1, 0.1, 0.1, 0.1, 0.03);
                }
            }
        }

        // Slash trail
        for (double t = -2.0; t <= 2.0; t += 0.4) {
            Vec3 slashPos = slashCenter.add(rightDir.scale(t));
            serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                    slashPos.x, slashPos.y, slashPos.z,
                    2, 0.2, 0.2, 0.2, 0.1);
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

        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.6f, 0.8f);
        }
    }
}