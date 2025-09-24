package com.xirc.nichirin.common.attack.moves.demon.basic;

import com.xirc.nichirin.common.attack.component.AbstractDemonAttack;
import com.xirc.nichirin.common.attack.component.IDemonAttacker;
import com.xirc.nichirin.common.system.DemonManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Demon bite attack - powerful bite that steals blood from enemies
 * High damage, close range, high stun, steals blood on hit
 * No knockback to keep enemies in bite range
 */
public class DemonBiteAttack extends AbstractDemonAttack<DemonBiteAttack, IDemonAttacker> {

    private boolean biteExecuted = false;
    private boolean biteConnected = false;

    public DemonBiteAttack() {
        setBloodOnKill(5);
        setHitsForBlood(1);
    }

    @Override
    protected void onStart() {
        biteExecuted = false;
        biteConnected = false;

        // Menacing bite windup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WOLF_GROWL, SoundSource.PLAYERS, 1.2f, 0.8f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Execute bite after windup
        if (!biteExecuted && tickCount >= windup) {
            executeBite();
            biteExecuted = true;
        }
    }

    private void executeBite() {
        if (user == null) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create bite effect
        createBiteEffect(userPos, lookDir);

        // Hit enemies in close range (very precise hitbox)
        List<LivingEntity> targets = getTargetsAtRange();

        if (!targets.isEmpty()) {
            biteConnected = true;

            for (LivingEntity target : targets) {
                // High damage bite
                hitTarget(target);

                // No knockback - keep them close for blood drain
                // Apply extended stun instead
                applyExtendedStun(target);

                // Blood steal effect - user is already a Player (demon)
                stealBlood(user, target);

                // Bite impact sound
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.8f, 0.6f);

                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0f, 0.7f);

                // Blood drain particles
                createBloodDrainEffect(target.position().add(0, target.getBbHeight() / 2, 0), userPos);
            }
        }

        // Main bite sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.6f);

        if (biteConnected) {
            // Successful bite sound
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.6f, 1.3f);
        }
    }

    private void applyExtendedStun(LivingEntity target) {
        // Apply extra stun effect beyond base hit stun
        // This simulates the target being held in the bite
        // The base hitStun from configuration handles the main stun duration
    }

    private void stealBlood(Player demonPlayer, LivingEntity target) {
        // Check if target has blood (not undead, not construct, etc.)
        if (targetHasBlood(target)) {
            // Call DemonManager to handle blood stealing
            DemonManager.onBiteHit(demonPlayer, target);

            // Additional blood steal visual feedback
            if (world instanceof ServerLevel serverLevel) {
                Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);

                // Red particles rising from target (blood being drained)
                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        targetPos.x, targetPos.y, targetPos.z,
                        8, 0.3, 0.2, 0.3, 0.1);
            }
        }
    }

    private boolean targetHasBlood(LivingEntity target) {
        // Check if entity has blood that can be stolen
        // Exclude undead, constructs, etc.
        String entityType = target.getType().toString().toLowerCase();

        // No blood for undead creatures
        if (entityType.contains("zombie") || entityType.contains("skeleton") ||
                entityType.contains("wither") || entityType.contains("ghost") ||
                entityType.contains("phantom") || entityType.contains("vex")) {
            return false;
        }

        // No blood for constructs
        if (entityType.contains("golem") || entityType.contains("blaze") ||
                entityType.contains("magma_cube") || entityType.contains("slime")) {
            return false;
        }

        // Players and most living mobs have blood
        return true;
    }

    private void createBiteEffect(Vec3 userPos, Vec3 lookDir) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 bitePos = userPos.add(lookDir.scale(range * 0.8));

        // Jaw snap effect
        serverLevel.sendParticles(ParticleTypes.CRIT,
                bitePos.x, bitePos.y, bitePos.z,
                8, 0.2, 0.2, 0.2, 0.1);

        // Menacing aura around bite
        for (int i = 0; i < 12; i++) {
            double angle = (i / 12.0) * 2 * Math.PI;
            double radius = 0.8;

            double x = bitePos.x + Math.cos(angle) * radius;
            double z = bitePos.z + Math.sin(angle) * radius;

            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    x, bitePos.y, z, 1, 0.1, 0.1, 0.1, 0.05);
        }

        // Bite impact burst
        serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                bitePos.x, bitePos.y, bitePos.z,
                6, 0.3, 0.2, 0.3, 0.1);
    }

    private void createBloodDrainEffect(Vec3 targetPos, Vec3 userPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Create blood stream from target to demon
        Vec3 direction = userPos.subtract(targetPos).normalize();
        int particles = 15;

        for (int i = 0; i < particles; i++) {
            double progress = i / (double)(particles - 1);
            Vec3 particlePos = targetPos.add(direction.scale(progress * targetPos.distanceTo(userPos)));

            // Delayed blood particles flowing to demon
            int delay = i * 2; // 2 ticks between each particle
            java.util.concurrent.CompletableFuture.delayedExecutor(delay * 50L, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        if (world instanceof ServerLevel level) {
                            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                                    particlePos.x, particlePos.y, particlePos.z,
                                    1, 0.05, 0.05, 0.05, 0.02);
                        }
                    });
        }

        // Blood absorption effect at demon's position
        serverLevel.sendParticles(ParticleTypes.HEART,
                userPos.x, userPos.y, userPos.z,
                3, 0.2, 0.3, 0.2, 0.05);
    }

    @Override
    protected void onStop() {
        biteExecuted = false;
        biteConnected = false;

        // Final effect based on success
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

            if (biteConnected) {
                // Successful bite - satisfied demon effect
                serverLevel.sendParticles(ParticleTypes.HEART,
                        userPos.x, userPos.y + 0.5, userPos.z,
                        5, 0.3, 0.2, 0.3, 0.1);
            } else {
                // Missed bite - frustrated demon effect
                serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                        userPos.x, userPos.y + 0.5, userPos.z,
                        3, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }
}