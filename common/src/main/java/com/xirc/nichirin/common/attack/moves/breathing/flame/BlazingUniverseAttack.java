package com.xirc.nichirin.common.attack.moves.breathing.flame;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Third Form: Blazing Universe
 * The user performs an arcing downwards vertical slash, often brought down from a high guard.
 *
 * Mechanics:
 * - Slash that goes from up to down
 * - Heavy attack with 2s windup
 * - Creates a fiery explosion when move ends
 * - Large AOE
 */
public class BlazingUniverseAttack extends FlameBreathingAttackBase {

    private boolean hasExecuted = false;
    private boolean explosionTriggered = false;
    private Set<LivingEntity> hitEntities = new HashSet<>();

    public BlazingUniverseAttack() {
    }

    @Override
    protected void onStart() {
        hasExecuted = false;
        explosionTriggered = false;
        hitEntities.clear();

        // Heavy windup sound - sword being raised
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 0.5f);

        // Create charging flame aura during windup
        createChargingEffect();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // During windup, create charging effects
        if (tickCount <= windup) {
            if (tickCount % 5 == 0) {
                createChargingEffect();
            }
            return;
        }

        // Execute the downward slash once after windup completes
        if (!hasExecuted && tickCount == windup + 1) {
            executeDownwardSlash();
            hasExecuted = true;
        }

        // Trigger explosion at the end of the attack
        if (!explosionTriggered && tickCount >= windup + duration - 5) {
            createFinalExplosion();
            explosionTriggered = true;
        }
    }

    private void createChargingEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Create upward flame spiral during charge
        for (int i = 0; i < 10; i++) {
            double angle = (tickCount * 0.3 + i * 36) * Math.PI / 180;
            double height = 3 + Math.sin(tickCount * 0.2) * 0.5;
            double radius = 1.5 + Math.sin(tickCount * 0.1) * 0.5;

            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + height;

            serverLevel.sendParticles(ParticleTypes.FLAME,
                    x, y, z, 1, 0.1, 0.1, 0.1, 0.02);
        }

        // Intensifying flame aura around user
        serverLevel.sendParticles(ParticleTypes.FLAME,
                userPos.x, userPos.y + 1, userPos.z,
                5, 0.5, 0.5, 0.5, 0.1);
    }

    private void executeDownwardSlash() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create downward arc effect
        createDownwardArcEffect();

        // Hit enemies in the downward slash path
        for (int i = 0; i <= 8; i++) {
            double progress = i / 8.0;
            // Downward arc - starts high, goes low
            double height = (1.0 - progress) * 3; // Starts 3 blocks high, ends at ground
            double forward = progress * range;

            Vec3 slashPoint = userPos.add(lookDir.scale(forward)).add(0, height, 0);
            List<LivingEntity> targets = getTargetsInCustomHitbox(slashPoint, hitboxSize, hitboxSize * 0.67, hitboxSize);

            for (LivingEntity target : targets) {
                if (!hitEntities.contains(target)) {
                    hitTarget(target);
                    hitEntities.add(target);

                    // Strong downward knockback
                    Vec3 downwardKnockback = new Vec3(
                            target.getDeltaMovement().x * 0.8,
                            -knockback * 1.5, // Strong downward slam
                            target.getDeltaMovement().z * 0.8
                    );
                    target.setDeltaMovement(downwardKnockback);
                    target.hurtMarked = true;
                    target.hasImpulse = true;

                    // Heavy hit sound
                    world.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.8f);
                }
            }
        }

        // Heavy sword slam sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.5f, 0.6f);

        // Ground impact sound
        world.playSound(null, user.getX(), user.getY() - 1, user.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 0.7f);
    }

    private void createDownwardArcEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create the downward arc with intense flame particles
        for (int i = 0; i <= 25; i++) {
            double progress = i / 25.0;
            // Downward arc trajectory
            double height = (1.0 - progress) * 3; // 3 blocks high to ground level
            double forward = progress * range;

            Vec3 arcPos = userPos.add(lookDir.scale(forward)).add(0, height, 0);

            // Intense flame trail
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    arcPos.x, arcPos.y, arcPos.z,
                    6, 0.3, 0.3, 0.3, 0.15);

            // Regular flames for width
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    arcPos.x, arcPos.y, arcPos.z,
                    8, 0.5, 0.3, 0.5, 0.2);

            // Create ground impact effects as we get closer to the ground
            if (height < 1.0) {
                serverLevel.sendParticles(ParticleTypes.LAVA,
                        arcPos.x, arcPos.y - 0.5, arcPos.z,
                        3, 0.3, 0.1, 0.3, 0.1);
            }
        }

        // Create wide slash effect
        Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();
        for (int side = -4; side <= 4; side++) {
            Vec3 sidePos = userPos.add(lookDir.scale(range * 0.7)).add(rightDir.scale(side * 0.5));

            // Wide flame slash
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    sidePos.x, sidePos.y, sidePos.z,
                    4, 0.2, 0.5, 0.2, 0.1);
        }
    }

    private void createFinalExplosion() {
        Vec3 explosionCenter = user.position().add(user.getLookAngle().scale(range * 0.8));

        // Create massive flame explosion
        createFlameExplosion(explosionCenter.add(0, 1, 0), 3.0f);

        // Hit all enemies in explosion radius
        List<LivingEntity> explosionTargets = getTargetsInCustomHitbox(
                explosionCenter.add(0, 1, 0),
                hitboxSize * 2, // Large explosion radius
                4.0, // Height
                hitboxSize * 2
        );

        for (LivingEntity target : explosionTargets) {
            if (!hitEntities.contains(target)) {
                // Explosion does bonus damage
                float originalDamage = damage;
                damage = damage * 1.3f; // 30% bonus explosion damage
                hitTarget(target);
                damage = originalDamage;

                hitEntities.add(target);

                // Explosion knockback
                Vec3 explosionKnockback = target.position().subtract(explosionCenter).normalize();
                target.push(
                        explosionKnockback.x * knockback * 1.5,
                        0.8, // Upward component
                        explosionKnockback.z * knockback * 1.5
                );

                // Extended fire duration for explosion hits
                target.setSecondsOnFire(getFireDuration() + 8);
            }
        }

        // Explosion sounds
        playFlameExplosionSound(explosionCenter);
        world.playSound(null, explosionCenter.x, explosionCenter.y, explosionCenter.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.6f);

        // Ground cracking effect
        createGroundCrackingEffect(explosionCenter);
    }

    private void createGroundCrackingEffect(Vec3 center) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Create radiating cracks with flame
        for (int angle = 0; angle < 360; angle += 30) {
            double rad = Math.toRadians(angle);
            for (int dist = 1; dist <= 8; dist++) {
                Vec3 crackPos = center.add(
                        Math.cos(rad) * dist,
                        0,
                        Math.sin(rad) * dist
                );

                // Flame cracks
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        crackPos.x, crackPos.y, crackPos.z,
                        2, 0.1, 0.3, 0.1, 0.05);

                // Lava seeping through cracks
                if (dist % 2 == 0) {
                    serverLevel.sendParticles(ParticleTypes.LAVA,
                            crackPos.x, crackPos.y, crackPos.z,
                            1, 0.1, 0.1, 0.1, 0.02);
                }
            }
        }
    }

    @Override
    public boolean isExplosiveAttack() {
        return true; // This attack creates a big explosion
    }

    @Override
    protected void onStop() {
        // Reset state
        hasExecuted = false;
        explosionTriggered = false;
        hitEntities.clear();

        // Final ground smoldering effect
        if (world instanceof ServerLevel serverLevel) {
            Vec3 groundPos = user.position().add(user.getLookAngle().scale(range * 0.8));

            // Smoldering ground
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    groundPos.x, groundPos.y + 0.5, groundPos.z,
                    15, 2.0, 1.0, 2.0, 0.05);
        }
    }
}