package com.xirc.nichirin.common.attack.moves.water;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * First Form: Water Surface Slash
 * Single quick horizontal slash - as fast as M1 with low breath cost
 * Bound to right-click (M2)
 *
 * All configuration comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class WaterSurfaceSlashAttack extends WaterBreathingAttackBase {

    private boolean slashExecuted = false;

    public WaterSurfaceSlashAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        slashExecuted = false;

        // Quick water slash sound
        playWaterSlashSound();

        // Instant water particles around user
        createWaterParticles();

        // Light startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 0.6f, 1.5f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Execute slash immediately (no windup)
        if (!slashExecuted && tickCount == windup + 1) {
            executeSlash();
            slashExecuted = true;
        }
    }

    private void executeSlash() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create horizontal slash effect
        createHorizontalSlashEffect(userPos, lookDir);

        // Hit enemies in front with lenient hitbox
        List<LivingEntity> targets = getTargetsAtRange();

        for (LivingEntity target : targets) {
            hitTarget(target);

            // Light knockback
            Vec3 knockbackDir = target.position().subtract(userPos).normalize();
            target.push(knockbackDir.x * knockback, 0.05, knockbackDir.z * knockback);

            // Individual hit sound
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.8f, 1.4f);
        }

        // Main slash sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    private void createHorizontalSlashEffect(Vec3 userPos, Vec3 lookDir) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();

        // Create wide horizontal arc of water particles
        for (int i = -6; i <= 6; i++) {
            double angle = i * 12; // 12-degree increments for 144-degree arc
            double radians = Math.toRadians(angle);

            Vec3 slashDir = lookDir.yRot((float)radians);
            Vec3 particlePos = userPos.add(slashDir.scale(range * 0.9));

            // Water slash particles
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    particlePos.x, particlePos.y, particlePos.z,
                    4, 0.2, 0.2, 0.2, 0.1);

            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                    particlePos.x, particlePos.y, particlePos.z,
                    2, 0.1, 0.1, 0.1, 0.05);
        }

        // Central water burst
        Vec3 centerPos = userPos.add(lookDir.scale(range * 0.7));
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                centerPos.x, centerPos.y, centerPos.z,
                12, 0.4, 0.4, 0.4, 0.15);

        // Water trail effect
        createWaterTrail(userPos, centerPos);
    }

    @Override
    protected void onStop() {
        // Reset state
        slashExecuted = false;

        // Final water splash
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    userPos.x, userPos.y, userPos.z,
                    8, 0.3, 0.3, 0.3, 0.1);
        }
    }
}