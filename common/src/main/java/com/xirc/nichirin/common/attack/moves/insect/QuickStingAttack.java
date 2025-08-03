package com.xirc.nichirin.common.attack.moves.insect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Quick Sting - Right-click attack for Insect Breathing
 * A rapid thrust with poison application.
 *
 * Mechanics:
 * - Very fast execution
 * - Moderate damage + poison
 * - Close range precision strike
 * - No cooldown for frequent use
 * - NO IMMUNITY FRAMES - can be spammed rapidly
 *
 * All configuration comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class QuickStingAttack extends InsectBreathingAttackBase {

    private boolean stingExecuted = false;

    public QuickStingAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        stingExecuted = false;

        // Quick sting startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BEE_STING, SoundSource.PLAYERS, 0.8f, 1.8f);

        // Create quick insect particles around user
        createQuickStingAura();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Execute sting very quickly after minimal windup
        if (!stingExecuted && tickCount == windup + 1) {
            executeQuickSting();
            stingExecuted = true;
        }
    }

    private void createQuickStingAura() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Quick swirl of insect particles around user
        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * 2 * Math.PI;
            double radius = 0.8;

            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + Math.sin(angle * 2) * 0.3;

            serverLevel.sendParticles(ParticleTypes.WITCH,
                    x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
        }

        // Forward thrust indicator
        for (int i = 1; i <= 3; i++) {
            Vec3 thrustPos = userPos.add(lookDir.scale(i * 0.8));
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    thrustPos.x, thrustPos.y, thrustPos.z,
                    1, 0.05, 0.05, 0.05, 0.02);
        }
    }

    private void executeQuickSting() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create thrust effect
        createThrustEffect();

        // Hit enemies in front of user
        List<LivingEntity> targets = getTargetsInCustomHitbox(
                userPos.add(lookDir.scale(range / 2)),
                hitboxSize, 2.0, range);

        for (LivingEntity target : targets) {
            // Apply sting damage and poison - NO IMMUNITY FRAMES
            hitTargetNoImmunity(target);

            // Light forward knockback
            Vec3 stingKnockback = lookDir.scale(knockback);
            target.push(stingKnockback.x, 0.1, stingKnockback.z);

            // Create sting impact
            createStingImpactEffect(target.position());

            // Sting hit sound
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.8f, 2.0f);
        }

        // Thrust sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 1.8f);
    }

    private void createThrustEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create quick thrust line
        for (int i = 0; i <= 8; i++) {
            double progress = i / 8.0;
            Vec3 thrustPos = userPos.add(lookDir.scale(progress * range));

            serverLevel.sendParticles(ParticleTypes.CRIT,
                    thrustPos.x, thrustPos.y, thrustPos.z,
                    2, 0.1, 0.1, 0.1, 0.05);

            if (i % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.WITCH,
                        thrustPos.x, thrustPos.y, thrustPos.z,
                        1, 0.05, 0.05, 0.05, 0.02);
            }
        }

        // Thrust tip effect
        Vec3 tipPos = userPos.add(lookDir.scale(range));
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                tipPos.x, tipPos.y, tipPos.z,
                5, 0.2, 0.2, 0.2, 0.1);
    }

    private void createStingImpactEffect(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 targetPos = impactPos.add(0, 1, 0);

        // Quick sting impact
        serverLevel.sendParticles(ParticleTypes.CRIT,
                targetPos.x, targetPos.y, targetPos.z,
                8, 0.2, 0.2, 0.2, 0.1);

        // Poison effect
        serverLevel.sendParticles(ParticleTypes.WITCH,
                targetPos.x, targetPos.y, targetPos.z,
                10, 0.3, 0.3, 0.3, 0.12);

        // Small insect flutter
        for (int i = 0; i < 4; i++) {
            double angle = (i / 4.0) * 2 * Math.PI;
            double distance = 0.8;

            double x = targetPos.x + Math.cos(angle) * distance;
            double z = targetPos.z + Math.sin(angle) * distance;
            double y = targetPos.y;

            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
        }
    }

    @Override
    protected void onStop() {
        // Final sting sparkle
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

            serverLevel.sendParticles(ParticleTypes.WITCH,
                    userPos.x, userPos.y, userPos.z,
                    8, 0.5, 0.5, 0.5, 0.1);
        }

        // Clear state
        stingExecuted = false;
    }
}