package com.xirc.nichirin.common.attack.moves.breathing.water;

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
 * Fourth Form: Striking Tide
 * The user performs big omnidirectional slashes after a short windup
 * 360° omnidirectional multiple slashes with particles during windup
 */
public class StrikingTideAttack extends WaterBreathingAttackBase {

    private boolean tideStarted = false;
    private Set<LivingEntity> hitEntities = new HashSet<>();
    private int tideTicks = 0;
    private int slashCount = 0;
    private static final int TOTAL_SLASHES = 4; // 4 big omnidirectional slashes

    public StrikingTideAttack() {
    }

    @Override
    protected void onStart() {
        tideStarted = false;
        hitEntities.clear();
        tideTicks = 0;
        slashCount = 0;
    }

    @Override
    protected void onActiveStart() {
        // Striking tide startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // During windup, show telegraph particles
        if (tickCount <= windup) {
            createWindupTelegraph();
        }

        // Start tide slashes after windup
        if (!tideStarted && tickCount == windup + 1) {
            startTide();
            tideStarted = true;
        }

        // Continue omnidirectional slashes during duration
        if (tideStarted && tickCount > windup && tickCount < windup + duration) {
            tideTicks++;
            performOmnidirectionalSlashes();
        }
    }

    private void createWindupTelegraph() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Expanding water rings to show the incoming area of effect
        int windupProgress = tickCount;
        float telegraphRadius = (windupProgress / (float)windup) * range;

        // Create expanding rings
        for (int ring = 1; ring <= 3; ring++) {
            float ringRadius = telegraphRadius * (ring / 3.0f);
            int particlesInRing = Math.max(8, (int)(ringRadius * 4));

            for (int i = 0; i < particlesInRing; i++) {
                double angle = (2 * Math.PI * i) / particlesInRing;
                double x = userPos.x + Math.cos(angle) * ringRadius;
                double z = userPos.z + Math.sin(angle) * ringRadius;
                double y = userPos.y + Math.sin(angle * 4) * 0.2; // Wavy effect

                // Telegraph particles - lighter and more transparent
                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        x, y, z, 1, 0.05, 0.05, 0.05, 0.02);

                if (ring == 1 && windupProgress > windup * 0.7) {
                    // Inner ring gets more intense near end of windup
                    serverLevel.sendParticles(ParticleTypes.SPLASH,
                            x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
                }
            }
        }

        // Central buildup effect
        if (windupProgress % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.BUBBLE,
                    userPos.x, userPos.y, userPos.z,
                    (int)(windupProgress * 0.2), 0.3, 0.3, 0.3, 0.1);
        }
    }

    private void startTide() {
        // Tide start sound - powerful water rush
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5f, 0.6f);

        // Initial tide explosion
        createTideExplosion();
    }

    private void performOmnidirectionalSlashes() {
        // Perform slashes every 5 ticks (halved from 10 after the double-tick dedup)
        if (tideTicks % 5 == 0 && slashCount < TOTAL_SLASHES) {
            executeOmnidirectionalSlash();
            slashCount++;
        }

        // Continuous water swirling effect
        if (tideTicks % 3 == 0) {
            createSwirlingWaterEffect();
        }

        // Hit all enemies in 360° range continuously but with spacing (halved from 6 after the
        // double-tick dedup)
        if (tideTicks % 3 == 0) {
            List<LivingEntity> targets = getTargetsInCustomHitbox(
                    user.position().add(0, user.getBbHeight() / 2, 0),
                    range * 2, // Full diameter
                    user.getBbHeight() + 2, // Height
                    range * 2  // Full diameter
            );

            for (LivingEntity target : targets) {
                // Allow re-hitting but with spacing
                if (!hasRecentlyHit(target)) {
                    hitTargetNoImmunity(target);
                    trackRecentHit(target);

                    // Outward knockback from center
                    Vec3 outwardKnockback = target.position().subtract(user.position()).normalize();
                    target.push(outwardKnockback.x * knockback * 0.6, 0.1, outwardKnockback.z * knockback * 0.6);
                }
            }
        }
    }

    private boolean hasRecentlyHit(LivingEntity target) {
        // Allow hitting the same target again after 12 ticks (0.6 seconds)
        return hitEntities.contains(target) && tideTicks % 12 > 6;
    }

    private void trackRecentHit(LivingEntity target) {
        hitEntities.add(target);
        // Clear old hits periodically
        if (tideTicks % 24 == 0) {
            hitEntities.clear();
        }
    }

    private void executeOmnidirectionalSlash() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Create 360° slash effect
        int slashParticles = 24;
        for (int i = 0; i < slashParticles; i++) {
            double angle = (2 * Math.PI * i) / slashParticles;
            double slashRadius = range * 0.9;

            double x = userPos.x + Math.cos(angle) * slashRadius;
            double z = userPos.z + Math.sin(angle) * slashRadius;
            double y = userPos.y + Math.sin(angle * 3) * 0.4; // Wavy slash height

            // Main slash particles
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    x, y, z, 4, 0.3, 0.3, 0.3, 0.15);

            // Crit particles for cutting effect
            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        x, y, z, 2, 0.1, 0.1, 0.1, 0.08);
            }
        }

        // Central water explosion
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                userPos.x, userPos.y, userPos.z,
                2, 0.2, 0.2, 0.2, 0);

        // Slash sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.8f + slashCount * 0.1f);
    }

    private void createTideExplosion() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Massive water explosion at start
        createWaterExplosion(userPos, 2.0f);

        // Initial 360° wave
        for (int i = 0; i < 32; i++) {
            double angle = (2 * Math.PI * i) / 32;
            double waveRadius = range * 0.8;

            double x = userPos.x + Math.cos(angle) * waveRadius;
            double z = userPos.z + Math.sin(angle) * waveRadius;
            double y = userPos.y;

            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    x, y, z, 6, 0.4, 0.4, 0.4, 0.2);
        }
    }

    private void createSwirlingWaterEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Swirling water around user
        double swirlSpeed = tideTicks * 0.3;
        int swirlingParticles = 16;

        for (int i = 0; i < swirlingParticles; i++) {
            double baseAngle = (2 * Math.PI * i) / swirlingParticles;
            double angle = baseAngle + swirlSpeed;
            double spiralRadius = range * 0.6 + Math.sin(swirlSpeed + i) * 0.5;

            double x = userPos.x + Math.cos(angle) * spiralRadius;
            double z = userPos.z + Math.sin(angle) * spiralRadius;
            double y = userPos.y + Math.cos(angle * 2) * 0.8;

            // Swirling water particles
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    x, y, z, 2, 0.2, 0.2, 0.2, 0.1);

            // Bubbles for underwater effect
            if (i % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.BUBBLE,
                        x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    @Override
    public boolean isOmnidirectional() {
        return true; // This attack hits in all directions
    }

    @Override
    protected void onStop() {
        // Clear state
        hitEntities.clear();
        tideTicks = 0;
        tideStarted = false;
        slashCount = 0;

        // Final massive tide effect
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

            // Final omnidirectional explosion
            createWaterExplosion(userPos, 3.0f);

            // Final 360° wave
            for (int ring = 1; ring <= 3; ring++) {
                float ringRadius = range * ring * 0.4f;
                int particlesInRing = 12 * ring;

                for (int i = 0; i < particlesInRing; i++) {
                    double angle = (2 * Math.PI * i) / particlesInRing;
                    double x = userPos.x + Math.cos(angle) * ringRadius;
                    double z = userPos.z + Math.sin(angle) * ringRadius;
                    double y = userPos.y;

                    serverLevel.sendParticles(ParticleTypes.SPLASH,
                            x, y, z, 4, 0.5, 0.5, 0.5, 0.2);
                }
            }
        }

        // Final tide sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5f, 0.5f);
    }
}