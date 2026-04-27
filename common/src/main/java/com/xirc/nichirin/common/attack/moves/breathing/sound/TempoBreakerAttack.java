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

// Tempo Breaker. Wide-arc slash that plants delayed TNT explosions on hit enemies.
public class TempoBreakerAttack extends SoundBreathingAttackBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(TempoBreakerAttack.class);

    private boolean hasExecuted = false;
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

    @Override
    protected void onStart() {
        hasExecuted = false;
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.8f, 1.3f);
        createSoundParticles();
    }

    @Override
    protected void perform() {
        if (world == null || world.isClientSide) return;

        try {
            if (!hasExecuted && tickCount == windup + 1) {
                executeTempoBreaker();
                hasExecuted = true;
            }
            processPendingExplosions();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void executeTempoBreaker() {
        try {
            if (user == null) return;

            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
            Vec3 lookDir = user.getLookAngle();

            createTempoSweepEffect();

            List<LivingEntity> targets = getTargetsInCone(userPos, lookDir, range, 90);

            for (LivingEntity target : targets) {
                if (target == null) continue;

                hitTarget(target);
                applyDisorientedEffect(target);

                Vec3 knockbackDir = target.position().subtract(userPos).normalize();
                target.setDeltaMovement(knockbackDir.x * 2.5, 0.4, knockbackDir.z * 2.5);
                target.hurtMarked = true;
                target.hasImpulse = true;

                PENDING_EXPLOSIONS.put(target.getUUID(),
                        new PendingExplosion(target.position(), target, 40));

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

    private void processPendingExplosions() {
        if (PENDING_EXPLOSIONS.isEmpty()) return;

        try {
            PENDING_EXPLOSIONS.entrySet().removeIf(entry -> {
                try {
                    PendingExplosion explosion = entry.getValue();
                    if (explosion == null) return true;

                    explosion.ticksRemaining--;

                    if (explosion.ticksRemaining <= 20 && explosion.ticksRemaining > 0) {
                        addExplosionWarningEffects(explosion.position, explosion.ticksRemaining);
                    }

                    if (explosion.ticksRemaining <= 0) {
                        if (explosion.target != null && explosion.target.isAlive()) {
                            createTNTExplosion(explosion.target.position());
                        } else {
                            createTNTExplosion(explosion.position);
                        }
                        return true;
                    }

                    return false;
                } catch (Exception e) {
                    LOGGER.warn("Error processing pending explosion: {}", e.getMessage());
                    return true;
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error in processPendingExplosions", e);
        }
    }

    private void addExplosionWarningEffects(Vec3 position, int ticksRemaining) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        int intensity = Math.max(1, 21 - ticksRemaining);

        if (ticksRemaining % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    position.x, position.y + 1, position.z,
                    intensity / 2, 0.5, 0.5, 0.5, 0.1);
        }

        if (ticksRemaining == 20 || ticksRemaining == 10) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 0.8f, 1.5f);
        }
    }

    private void createTNTExplosion(Vec3 position) {
        if (world.isClientSide) return;

        world.explode(null, position.x, position.y, position.z, 3.0f, net.minecraft.world.level.Level.ExplosionInteraction.NONE);

        if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    position.x, position.y + 0.5, position.z,
                    1, 0.0, 0.0, 0.0, 0.0);

            for (int i = 0; i < 8; i++) {
                double angle = (i / 8.0) * 2 * Math.PI;
                double x = position.x + Math.cos(angle) * 3.0;
                double z = position.z + Math.sin(angle) * 3.0;
                serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                        x, position.y, z, 2, 0.1, 0.1, 0.1, 0.1);
            }

            serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                    position.x, position.y + 1, position.z, 4, 1.0, 1.0, 1.0, 0.2);
            serverLevel.sendParticles(NichirinParticleRegistry.FLASH2.get(),
                    position.x, position.y + 1, position.z, 4, 1.0, 1.0, 1.0, 0.2);

            for (int i = 0; i < 6; i++) {
                double angle = (i / 6.0) * 2 * Math.PI;
                double x = position.x + Math.cos(angle) * 2.5;
                double z = position.z + Math.sin(angle) * 2.5;
                serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                        x, position.y + 0.5, z, 1, 0.3, 0.3, 0.3, 0.1);
            }

            serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH1.get(),
                    position.x, position.y + 1, position.z, 3, 0.8, 0.8, 0.8, 0.15);
            serverLevel.sendParticles(NichirinParticleRegistry.BLUE_FLASH2.get(),
                    position.x, position.y + 1, position.z, 3, 0.8, 0.8, 0.8, 0.15);
        }

        world.playSound(null, position.x, position.y, position.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5f, 1.0f);
        world.playSound(null, position.x, position.y, position.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    private void createTempoSweepEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 slashCenter = userPos.add(lookDir.scale(2.5));

        for (int i = -45; i <= 45; i += 15) {
            double angle = Math.toRadians(i);
            Vec3 sweepDir = lookDir.scale(Math.cos(angle)).add(rightDir.scale(Math.sin(angle)));

            for (double r = 0.5; r <= range - 2.0; r += 1.2) {
                Vec3 sweepPos = slashCenter.add(sweepDir.scale(r));
                if (tickCount < windup + duration) {
                    serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                            sweepPos.x, sweepPos.y, sweepPos.z, 1, 0.1, 0.1, 0.1, 0.03);
                }
            }
        }

        for (double t = -2.0; t <= 2.0; t += 0.6) {
            Vec3 slashPos = slashCenter.add(rightDir.scale(t));
            serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                    slashPos.x, slashPos.y, slashPos.z, 1, 0.2, 0.2, 0.2, 0.1);
        }
    }

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

    // Call from a server tick handler to process pending explosions after the attack ends.
    public static void processPendingExplosionsGlobal(net.minecraft.server.MinecraftServer server) {
        if (PENDING_EXPLOSIONS.isEmpty()) return;

        PENDING_EXPLOSIONS.entrySet().removeIf(entry -> {
            PendingExplosion explosion = entry.getValue();
            explosion.ticksRemaining--;

            if (explosion.ticksRemaining <= 20 && explosion.ticksRemaining > 0) {
                if (explosion.target.level() instanceof ServerLevel serverLevel) {
                    addGlobalExplosionWarningEffects(serverLevel, explosion.position, explosion.ticksRemaining);
                }
            }

            if (explosion.ticksRemaining <= 0) {
                if (explosion.target.isAlive()) {
                    createGlobalTNTExplosion(explosion.target.level(), explosion.target.position());
                } else {
                    createGlobalTNTExplosion(explosion.target.level(), explosion.position);
                }
                return true;
            }

            return false;
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
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    position.x, position.y + 0.5, position.z, 1, 0.0, 0.0, 0.0, 0.0);

            for (int i = 0; i < 8; i++) {
                double angle = (i / 8.0) * 2 * Math.PI;
                double x = position.x + Math.cos(angle) * 3.0;
                double z = position.z + Math.sin(angle) * 3.0;
                serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                        x, position.y, z, 1, 0.1, 0.1, 0.1, 0.1);
            }
        }

        world.playSound(null, position.x, position.y, position.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5f, 1.0f);
    }
}
