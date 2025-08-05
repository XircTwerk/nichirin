package com.xirc.nichirin.common.attack.moves.thunder;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Sixth Form: Rumble and Flash
 * Summons lightning at the original positions where enemies were when the attack started
 * Enemies must move away from their starting positions to avoid the strikes
 *
 * All configuration now comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class RumbleFlashAttack extends ThunderBreathingAttackBase {

    private final Set<LivingEntity> struckTargets = new HashSet<>();
    private final Map<Vec3, Integer> pendingStrikes = new HashMap<>(); // Fixed position -> delay until strike
    private static final int WARNING_DURATION = 35; // 1.75 seconds warning (35 ticks)

    public RumbleFlashAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        struckTargets.clear();
        pendingStrikes.clear();

        // Charge-up sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 0.5f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Mark new lightning strikes every 5 ticks
        if (tickCount % 5 == 0) {
            markLightningStrikes();
        }

        // Update pending strikes and execute when ready
        updatePendingStrikes();
    }

    private void markLightningStrikes() {
        ServerLevel serverLevel = (ServerLevel) world;
        Vec3 userPos = user.position();

        // Find all targets in range (using configured range)
        AABB searchArea = new AABB(
                userPos.x - range, userPos.y - range, userPos.z - range,
                userPos.x + range, userPos.y + range, userPos.z + range
        );

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity != user && entity.isAlive() &&
                        entity.position().distanceTo(userPos) <= range &&
                        !struckTargets.contains(entity));

        // Mark up to 3 targets per marking
        int strikeCount = Math.min(3, targets.size());
        for (int i = 0; i < strikeCount; i++) {
            if (i < targets.size()) {
                LivingEntity target = targets.get(i);
                Vec3 targetCurrentPos = target.position(); // Capture current position when attack starts

                net.minecraft.util.RandomSource random = ((ServerLevel) world).getRandom();

                // Mark multiple lightning strikes around target's CURRENT position (won't move)
                for (int j = 0; j < 3; j++) {
                    double offsetX = (random.nextDouble() - 0.5) * 3;
                    double offsetZ = (random.nextDouble() - 0.5) * 3;
                    Vec3 strikePos = targetCurrentPos.add(offsetX, 0, offsetZ);

                    // Add to pending strikes at this FIXED position
                    pendingStrikes.put(strikePos, WARNING_DURATION);

                    // Warning sound at the fixed strike position
                    world.playSound(null, strikePos.x, strikePos.y, strikePos.z,
                            SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.3f, 1.5f);
                }

                struckTargets.add(target);
            }
        }

        // Mark environmental lightning for atmosphere (these also stay fixed)
        for (int i = 0; i < 2; i++) {
            double angle = Math.random() * Math.PI * 2;
            double distance = 5 + Math.random() * (range - 5);
            Vec3 envStrike = userPos.add(
                    Math.cos(angle) * distance,
                    0,
                    Math.sin(angle) * distance
            );

            pendingStrikes.put(envStrike, WARNING_DURATION);
        }
    }

    private void updatePendingStrikes() {
        ServerLevel serverLevel = (ServerLevel) world;
        Iterator<Map.Entry<Vec3, Integer>> iterator = pendingStrikes.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Vec3, Integer> entry = iterator.next();
            Vec3 strikePos = entry.getKey(); // Fixed position - never changes
            int timeLeft = entry.getValue();

            // Show warning particles at the FIXED position
            showWarningParticles(serverLevel, strikePos, timeLeft);

            // Countdown
            timeLeft--;
            entry.setValue(timeLeft);

            // Execute strike when timer reaches 0
            if (timeLeft <= 0) {
                executeLightningStrike(serverLevel, strikePos);
                iterator.remove();
            }
        }
    }

    private void showWarningParticles(ServerLevel serverLevel, Vec3 strikePos, int timeLeft) {
        // Intensity increases as strike gets closer
        float intensity = 1.0f - (timeLeft / (float) WARNING_DURATION);

        // Warning particles - increasing intensity at FIXED position
        int particleCount = (int) (5 + intensity * 15);

        // Electric sparks at ground level - STAYS at original position
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                strikePos.x, strikePos.y + 0.1, strikePos.z,
                particleCount, 0.3, 0.1, 0.3, 0.1);

        // Glowstone dust particles in a column (shows fixed strike zone)
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                strikePos.x, strikePos.y + 2, strikePos.z,
                (int) (3 + intensity * 7), 0.2, 1.0, 0.2, 0.05);

        // Final warning particles (last few ticks)
        if (timeLeft <= 10) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    strikePos.x, strikePos.y + 0.5, strikePos.z,
                    10, 0.5, 0.2, 0.5, 0.1);

            // Warning sound gets more intense at the FIXED position
            world.playSound(null, strikePos.x, strikePos.y, strikePos.z,
                    SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS,
                    0.2f + intensity * 0.3f, 1.2f + intensity * 0.8f);
        }
    }

    private void executeLightningStrike(ServerLevel serverLevel, Vec3 strikePos) {
        // Create the actual lightning bolt at the FIXED position
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
        if (lightning != null) {
            lightning.moveTo(strikePos);
            lightning.setVisualOnly(true);
            serverLevel.addFreshEntity(lightning);
        }

        // Check for targets at the FIXED strike location
        AABB strikeArea = new AABB(strikePos.add(-1.5, -1, -1.5), strikePos.add(1.5, 3, 1.5));
        List<LivingEntity> targetsAtStrike = world.getEntitiesOfClass(LivingEntity.class, strikeArea,
                entity -> entity != user && entity.isAlive());

        for (LivingEntity target : targetsAtStrike) {
            // Apply damage with extended stun (using configured damage, knockback, hitStun)
            hitTarget(target);

            // Extra knockback for this form (using configured knockback)
            Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
            target.push(knockbackDir.x * knockback, 0.3, knockbackDir.z * knockback);

            // Impact particle effects
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY() + 1, target.getZ(),
                    50, 1.0, 1.0, 1.0, 0.3);

            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getY() + target.getBbHeight(), target.getZ(),
                    30, 0.5, 0.5, 0.5, 0.2);
        }

        // Thunder impact sound
        world.playSound(null, strikePos.x, strikePos.y, strikePos.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0f, 0.8f);

        // Ground impact particles
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                strikePos.x, strikePos.y, strikePos.z,
                1, 0, 0, 0, 0);

        serverLevel.sendParticles(ParticleTypes.SMOKE,
                strikePos.x, strikePos.y + 0.1, strikePos.z,
                20, 1.0, 0.1, 1.0, 0.05);
    }

    @Override
    protected void onStop() {
        struckTargets.clear();

        // Execute any remaining pending strikes immediately (optional - for cleanup)
        if (!world.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) world;
            for (Vec3 strikePos : pendingStrikes.keySet()) {
                executeLightningStrike(serverLevel, strikePos);
            }
        }

        pendingStrikes.clear();
    }
}