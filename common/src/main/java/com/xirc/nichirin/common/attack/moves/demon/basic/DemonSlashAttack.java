package com.xirc.nichirin.common.attack.moves.demon.basic;

import com.xirc.nichirin.common.attack.component.AbstractDemonAttack;
import com.xirc.nichirin.common.attack.component.IDemonAttacker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Basic demon slash attack with followup potential
 * Stage 1: Right to left claw slash (no knockback)
 * Stage 2: Left to right claw slash (higher knockback)
 */
public class DemonSlashAttack extends AbstractDemonAttack<DemonSlashAttack, IDemonAttacker> {

    private boolean slashExecuted = false;
    private int slashStage = 1; // 1 = right-to-left, 2 = left-to-right followup
    private int slashTimer = 0;

    public DemonSlashAttack() {
        // Configuration comes from moveset
    }

    /**
     * Set the slash stage for this attack
     * @param stage 1 = right-to-left, 2 = left-to-right followup
     */
    public void setSlashStage(int stage) {
        this.slashStage = Math.max(1, Math.min(2, stage)); // Clamp between 1-2
    }

    @Override
    protected void onStart() {
        slashExecuted = false;
        slashTimer = 0;

        // Demon claw sound based on stage
        if (slashStage == 1) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WOLF_GROWL, SoundSource.PLAYERS, 0.8f, 1.2f);
        } else {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.RAVAGER_ATTACK, SoundSource.PLAYERS, 1.0f, 1.4f);
        }
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        slashTimer++;

        // Execute slash immediately (no windup)
        if (!slashExecuted && slashTimer >= windup) {
            executeSlash();
            slashExecuted = true;
        }
    }

    private void executeSlash() {
        if (user == null) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create slash effect based on stage
        if (slashStage == 1) {
            createRightToLeftSlash(userPos, lookDir);
        } else {
            createLeftToRightSlash(userPos, lookDir);
        }

        // Hit enemies in front
        List<LivingEntity> targets = getTargetsAtRange();

        for (LivingEntity target : targets) {
            // Use no immunity for rapid combo hits
            hitTargetNoImmunity(target);

            // Apply knockback only for second hit
            if (slashStage == 2) {
                Vec3 knockbackDir = target.position().subtract(userPos).normalize();
                target.push(knockbackDir.x * knockback, 0.2, knockbackDir.z * knockback);
            }

            // Hit sound
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 1.3f);
        }

        // Main slash sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f,
                slashStage == 1 ? 1.2f : 0.9f);
    }

    private void createRightToLeftSlash(Vec3 userPos, Vec3 lookDir) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 rightVector = lookDir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 slashCenter = userPos.add(lookDir.scale(range * 0.5));

        // Create claw slash from RIGHT TO LEFT
        int totalParticles = 10;

        for (int i = 0; i < totalParticles; i++) {
            int finalI = i;

            CompletableFuture.delayedExecutor(i * 15L, TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        if (world instanceof ServerLevel level) {
                            float progress = (float) finalI / (totalParticles - 1);

                            // Right to left motion
                            float horizontalOffset = (0.5f - progress) * range * 1.2f;
                            float forwardOffset = 0.3f * (1.0f - 4.0f * (progress - 0.5f) * (progress - 0.5f));

                            Vec3 particlePos = slashCenter
                                    .add(rightVector.scale(horizontalOffset))
                                    .add(lookDir.scale(forwardOffset));

                            // Red particles for demon claws
                            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                                    particlePos.x, particlePos.y, particlePos.z,
                                    3, 0.1, 0.1, 0.1, 0.1);

                            level.sendParticles(ParticleTypes.CRIT,
                                    particlePos.x, particlePos.y, particlePos.z,
                                    2, 0.15, 0.1, 0.15, 0.1);
                        }
                    });
        }
    }

    private void createLeftToRightSlash(Vec3 userPos, Vec3 lookDir) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 rightVector = lookDir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 slashCenter = userPos.add(lookDir.scale(range * 0.5));

        // Create claw slash from LEFT TO RIGHT (followup)
        int totalParticles = 12;

        for (int i = 0; i < totalParticles; i++) {
            int finalI = i;

            CompletableFuture.delayedExecutor(i * 12L, TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        if (world instanceof ServerLevel level) {
                            float progress = (float) finalI / (totalParticles - 1);

                            // Left to right motion
                            float horizontalOffset = (progress - 0.5f) * range * 1.4f;
                            float forwardOffset = 0.4f * (1.0f - 4.0f * (progress - 0.5f) * (progress - 0.5f));

                            Vec3 particlePos = slashCenter
                                    .add(rightVector.scale(horizontalOffset))
                                    .add(lookDir.scale(forwardOffset));

                            // More intense particles for followup
                            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                                    particlePos.x, particlePos.y, particlePos.z,
                                    4, 0.12, 0.1, 0.12, 0.12);

                            level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                                    particlePos.x, particlePos.y, particlePos.z,
                                    1, 0.1, 0.05, 0.1, 0.05);
                        }
                    });
        }
    }

    @Override
    protected void onStop() {
        slashExecuted = false;

        // Final claw marks effect
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    userPos.x, userPos.y, userPos.z,
                    slashStage == 2 ? 8 : 5, 0.3, 0.3, 0.3, 0.1);
        }
    }

    /**
     * Get the current slash stage
     */
    public int getSlashStage() {
        return slashStage;
    }

    /**
     * Check if this is the followup hit
     */
    public boolean isFollowup() {
        return slashStage == 2;
    }
}