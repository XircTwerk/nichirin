package com.xirc.nichirin.common.attack.moves.breathing.sound;

import com.xirc.nichirin.common.util.NichirinArmorDamage;
import com.xirc.nichirin.common.util.NichirinDamageSources;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Fifth Form: String Performance
 * The user holds the chain of their twin swords and rotates the blades at high speed
 * while charging forward. The spin of the blades without changing grip allows for
 * continual slashes while still being in motion.
 *
 * Mechanics:
 * - Forward dash (18 blocks) in 6 separate 3-block dashes
 * - Each dash is spaced 10 ticks apart
 * - Pulls enemies hit along with the user's movement
 * - Each spin hits multiple times (high DPS)
 * - User stays grounded throughout
 * - Finisher does a big explosion with a huge shockwave of damage
 */
public class StringPerformanceAttack extends SoundBreathingAttackBase {

    private boolean dashStarted = false;
    private Vec3 dashDirection;
    private Vec3 startPosition;
    private final Set<LivingEntity> caughtEnemies = new HashSet<>();
    private final List<LivingEntity> draggedEnemies = new ArrayList<>();
    private int dashSegment = 0;
    private final int TOTAL_SEGMENTS = 6; // 6 segments of 3 blocks each = 18 blocks total
    private final int SEGMENT_LENGTH = 3;
    private final int DASH_SPACING = 10; // 10 ticks between dashes
    private int hitCounter = 0;
    private int lastDashTick = 0;
    private boolean finaleExecuted = false;

    public StringPerformanceAttack() {
    }

    @Override
    protected void onStart() {
        dashStarted = false;
        caughtEnemies.clear();
        draggedEnemies.clear();
        dashSegment = 0;
        hitCounter = 0;
        lastDashTick = 0;
        finaleExecuted = false;

        dashDirection = user.getLookAngle().normalize();
        startPosition = user.position();

        // Initial chain spinning sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 1.0f, 1.5f);

        // Create initial spinning effect
        createStringFormationEffect();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Start first dash after windup
        if (!dashStarted && tickCount == windup + 1) {
            performDash();
            dashStarted = true;
            lastDashTick = tickCount;
        }

        // Perform subsequent dashes every 10 ticks
        if (dashStarted && tickCount > windup) {
            int ticksSinceLastDash = tickCount - lastDashTick;

            if (ticksSinceLastDash >= DASH_SPACING && dashSegment < TOTAL_SEGMENTS) {
                performDash();
                lastDashTick = tickCount;
            }
        }

        // Always keep user grounded and perform attacks
        if (dashStarted) {
            keepUserGrounded();
            performSpinningSlashes();
            catchAndDragEnemies();

            // Only create trail effect if within move duration
            if (tickCount < windup + duration) {
                createStringTrailEffect();
            }
        }

        // Final explosion when all dashes are complete
        if (dashSegment >= TOTAL_SEGMENTS && !finaleExecuted && tickCount >= lastDashTick + 10) {
            executeFinishingExplosion();
            finaleExecuted = true;
        }
    }

    private void performDash() {
        // Calculate dash target position
        Vec3 currentPos = user.position();
        Vec3 dashTarget = currentPos.add(dashDirection.scale(SEGMENT_LENGTH));

        // Set dash velocity instead of teleporting
        Vec3 dashVelocity = dashDirection.scale(SEGMENT_LENGTH * 0.8f); // Dash speed
        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;
        user.hasImpulse = true;

        dashSegment++;

        // Dash sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.0f, 1.0f + (dashSegment * 0.1f));

        // Create dash effect
        createQuickDashEffect(currentPos, dashTarget);

        if (dashSegment > 1) {
            createSegmentAdvancementEffect();
        }
    }

    private void keepUserGrounded() {
        // Force user to stay on the ground
        Vec3 currentVelocity = user.getDeltaMovement();
        user.setDeltaMovement(currentVelocity.x, Math.min(0, currentVelocity.y), currentVelocity.z);
        user.setOnGround(true);
    }

    private void performSpinningSlashes() {
        // Perform slashes every 3 ticks for high DPS
        if (tickCount % 3 == 0) {
            executeSpinningSpin();
            hitCounter++;

            // Spinning blade sound
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5f, 1.5f + (hitCounter * 0.05f));
        }
    }

    private void executeSpinningSpin() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Hit enemies around the user (spinning chain effect)
        List<LivingEntity> nearbyTargets = getTargetsInCustomHitbox(userPos, hitboxSize, hitboxSize * 0.83, hitboxSize);

        for (LivingEntity target : nearbyTargets) {
            // Reduced damage per hit since it's multi-hit
            float originalDamage = damage;
            damage = damage * 0.3f; // 30% damage per spin

            // Use direct damage application instead of hitTarget methods
            applyDirectDamage(target);

            damage = originalDamage; // Restore

            // Create chain strike effect
            createChainStrikeEffect(target.position());
        }

        // Also hit dragged enemies
        for (LivingEntity draggedEnemy : draggedEnemies) {
            if (draggedEnemy.isAlive()) {
                float originalDamage = damage;
                damage = damage * 0.3f;

                // Use direct damage application
                applyDirectDamage(draggedEnemy);

                damage = originalDamage;
                createChainStrikeEffect(draggedEnemy.position());
            }
        }
    }

    // Add this helper method to apply damage without triggering attack stop conditions
    private void applyDirectDamage(LivingEntity target) {
        if (target == null || target == user || !target.isAlive()) {
            return;
        }

        // Apply damage directly using DamageSource
        NichirinArmorDamage.hurt(target, NichirinDamageSources.breathing(user), damage);

        // Apply knockback manually
        Vec3 knockbackDirection = target.position().subtract(user.position()).normalize();
        Vec3 knockbackVelocity = knockbackDirection.scale(knockback * 0.3); // Reduced knockback for multi-hit
        target.push(knockbackVelocity.x, knockbackVelocity.y * 0.5, knockbackVelocity.z);
        target.hurtMarked = true;

        // Add any status effects if configured
        applyDisorientedEffect(target);
    }

    private void catchAndDragEnemies() {
        Vec3 userPos = user.position();

        // Find enemies around current position
        List<LivingEntity> nearbyEnemies = getTargetsInCustomHitbox(userPos, hitboxSize, hitboxSize * 0.83, hitboxSize);

        for (LivingEntity enemy : nearbyEnemies) {
            if (!caughtEnemies.contains(enemy)) {
                caughtEnemies.add(enemy);
                draggedEnemies.add(enemy);

                world.playSound(null, enemy.getX(), enemy.getY(), enemy.getZ(),
                        SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 1.0f, 1.8f);

                createChainCatchEffect(enemy.position());
            }
        }

        // Pull dragged enemies toward player via velocity
        Vec3 dragAnchor = userPos.add(0, user.getBbHeight() / 4, 0);
        for (LivingEntity draggedEnemy : new ArrayList<>(draggedEnemies)) {
            if (draggedEnemy.isAlive()) {
                Vec3 toDrag = dragAnchor.subtract(draggedEnemy.position());
                double dist = toDrag.length();
                if (dist > 0.5) {
                    draggedEnemy.setDeltaMovement(toDrag.normalize().scale(Math.min(dist, 3.5)));
                    draggedEnemy.hurtMarked = true;
                } else {
                    draggedEnemy.setDeltaMovement(
                            user.getDeltaMovement().x,
                            draggedEnemy.getDeltaMovement().y,
                            user.getDeltaMovement().z
                    );
                }
                createChainDragEffect(draggedEnemy.position(), userPos);
            } else {
                draggedEnemies.remove(draggedEnemy);
            }
        }
    }

    private void executeFinishingExplosion() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Massive finale explosion
        createStringFinaleExplosion(userPos);

        // Hit all enemies in large radius with massive damage
        List<LivingEntity> finaleTargets = getTargetsInCustomHitbox(userPos, range * 1.5f, 3.0, range * 1.5f);

        for (LivingEntity target : finaleTargets) {
            // Full damage for finale - use direct damage to avoid stopping
            applyFinaleDamage(target);

            // Massive knockback
            Vec3 explosiveKnockback = target.position().subtract(userPos).normalize().scale(knockback * 3.0);
            target.push(explosiveKnockback.x, 0.8, explosiveKnockback.z);
            target.hurtMarked = true;

            // Extended stun
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    40, // 2 seconds
                    5,
                    false,
                    false
            ));
        }

        // Release dragged enemies with finale force
        for (LivingEntity draggedEnemy : draggedEnemies) {
            if (draggedEnemy.isAlive()) {
                // Extreme finale knockback
                Vec3 finaleKnockback = dashDirection.scale(knockback * 4.0);
                draggedEnemy.push(finaleKnockback.x, 1.0, finaleKnockback.z);
                draggedEnemy.hurtMarked = true;
            }
        }

        // Finale explosion sound
        world.playSound(null, userPos.x, userPos.y, userPos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.5f, 0.6f);

        world.playSound(null, userPos.x, userPos.y, userPos.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0f, 0.8f);
    }

    // Separate method for finale damage to ensure it uses full damage
    private void applyFinaleDamage(LivingEntity target) {
        if (target == null || target == user || !target.isAlive()) {
            return;
        }

        // Apply full damage for finale
        NichirinArmorDamage.hurt(target, NichirinDamageSources.breathing(user), damage);

        // Apply status effects if configured
        applyDisorientedEffect(target);
    }

    private void createQuickDashEffect(Vec3 startPos, Vec3 endPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Create dash trail between start and end positions
        Vec3 direction = endPos.subtract(startPos);
        double distance = direction.length();
        Vec3 normalized = direction.normalize();

        // Create particles along the dash trail
        for (double d = 0; d <= distance; d += 0.5) {
            Vec3 trailPos = startPos.add(normalized.scale(d));

            // Main dash trail - only show SOUND particles during move duration
            if (tickCount < windup + duration) {
                serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                        trailPos.x, trailPos.y + 0.5, trailPos.z,
                        2, 0.3, 0.3, 0.3, 0.1);
            }

            serverLevel.sendParticles(NichirinParticleRegistry.FLASH2.get(),
                    trailPos.x, trailPos.y + 1, trailPos.z,
                    1, 0.2, 0.2, 0.2, 0.08);
        }

        // End position impact
        serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                endPos.x, endPos.y + 1, endPos.z,
                5, 1.0, 1.0, 1.0, 0.2);
    }

    private void createStringFormationEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Chain formation particles - only show SOUND particles during move duration
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * 2 * Math.PI;
            double radius = 2.0;
            Vec3 chainPos = userPos.add(
                    Math.cos(angle) * radius,
                    Math.sin(angle * 3) * 0.5,
                    Math.sin(angle) * radius
            );

            if (tickCount < windup + duration) {
                serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                        chainPos.x, chainPos.y, chainPos.z,
                        1, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    private void createStringTrailEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        double spinAngle = (tickCount * 0.8) % (2 * Math.PI);

        // Spinning chain trails - only show SOUND particles during move duration
        for (int chain = 0; chain < 2; chain++) { // Twin chains
            double chainAngle = spinAngle + (chain * Math.PI);

            for (double r = 1.0; r <= 2.5; r += 0.6) {
                double x = userPos.x + Math.cos(chainAngle) * r;
                double z = userPos.z + Math.sin(chainAngle) * r;
                double y = userPos.y + Math.sin(r + tickCount * 0.2) * 0.3;

                serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                        x, y, z, 1, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    private void createChainStrikeEffect(Vec3 targetPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Chain impact effect - only show SOUND particles during move duration
        if (tickCount < windup + duration) {
            serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                    targetPos.x, targetPos.y + 0.5, targetPos.z,
                    3, 0.3, 0.3, 0.3, 0.1);
        }

        serverLevel.sendParticles(ParticleTypes.CRIT,
                targetPos.x, targetPos.y + 0.5, targetPos.z,
                2, 0.2, 0.2, 0.2, 0.1);
    }

    private void createSegmentAdvancementEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Segment burst effect
        serverLevel.sendParticles(NichirinParticleRegistry.FLASH2.get(),
                userPos.x, userPos.y, userPos.z,
                8, 1.0, 1.0, 1.0, 0.2);

        serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                userPos.x, userPos.y, userPos.z,
                5, 0.8, 0.8, 0.8, 0.15);
    }

    private void createChainCatchEffect(Vec3 enemyPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Chain wrapping effect - only show SOUND particles during move duration
        if (tickCount < windup + duration) {
            serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                    enemyPos.x, enemyPos.y + 1, enemyPos.z,
                    5, 0.5, 0.5, 0.5, 0.2);
        }

        serverLevel.sendParticles(ParticleTypes.CRIT,
                enemyPos.x, enemyPos.y + 1, enemyPos.z,
                4, 0.4, 0.4, 0.4, 0.2);
    }

    private void createChainDragEffect(Vec3 enemyPos, Vec3 userPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Chain connection line - only show SOUND particles during move duration
        Vec3 midPoint = enemyPos.add(userPos).scale(0.5);

        if (tickCount < windup + duration) {
            serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                    midPoint.x, midPoint.y, midPoint.z,
                    1, 0.1, 0.1, 0.1, 0.02);
        }
    }

    private void createStringFinaleExplosion(Vec3 center) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Massive central explosion
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                center.x, center.y, center.z,
                2, 1.0, 1.0, 1.0, 0);

        // Huge shockwave
        createSoundShockwave(center, range * 2.0f, 16);

        // Massive particle burst - only show SOUND particles during move duration
        if (tickCount < windup + duration) {
            serverLevel.sendParticles(NichirinParticleRegistry.SOUND.get(),
                    center.x, center.y, center.z,
                    20, range * 1.5, 5.0, range * 1.5, 0.4);
        }

        serverLevel.sendParticles(NichirinParticleRegistry.SHOCKWAVE.get(),
                center.x, center.y, center.z,
                15, range * 1.2, 4.0, range * 1.2, 0.3);

        serverLevel.sendParticles(NichirinParticleRegistry.FLASH1.get(),
                center.x, center.y, center.z,
                10, range * 0.8, 3.0, range * 0.8, 0.25);

        serverLevel.sendParticles(NichirinParticleRegistry.FLASH2.get(),
                center.x, center.y, center.z,
                10, range * 0.8, 3.0, range * 0.8, 0.25);

        // Sonic boom finale
        serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                center.x, center.y, center.z,
                3, 3.0, 3.0, 3.0, 0);
    }

    @Override
    public boolean isDashAttack() {
        return true; // This is a dash attack
    }

    @Override
    protected void onStop() {
        // Reset user velocity to ensure they stay grounded
        user.setDeltaMovement(Vec3.ZERO);
        user.setOnGround(true);

        // Clear all state
        dashStarted = false;
        caughtEnemies.clear();
        draggedEnemies.clear();
        dashSegment = 0;
        hitCounter = 0;
        lastDashTick = 0;
        finaleExecuted = false;

        // Final chain settling sound
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 0.8f, 0.6f);
        }
    }
}
