package com.xirc.nichirin.common.attack.moves.breathing.beast;

import com.xirc.nichirin.common.util.HitboxData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Second Fang: Slice - Two progressing X-shaped slash projectiles.
 * Hitboxes advance forward over ticks in an X pattern.
 */
public class BeastXSliceAttack extends BeastBreathingAttackBase {

    private static final int SLASH_1_START = 1;
    private static final int SLASH_1_END = 6;
    private static final int SLASH_2_START = 5;
    private static final int SLASH_2_END = 10;

    @Override
    protected void onStart() {
        playSlashSound();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        int t = tickCount;
        Vec3 origin = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 look = user.getLookAngle();
        Vec3 perp = new Vec3(-look.z, 0, look.x).normalize();

        // First X-slash: advancing forward, slicing diagonally
        if (t >= SLASH_1_START && t <= SLASH_1_END) {
            float progress = (float)(t - SLASH_1_START) / (SLASH_1_END - SLASH_1_START);
            float dist = 1.0f + progress * 4.0f;
            Vec3 center = origin.add(look.scale(dist));

            // X-pattern: two diagonal slashes
            Vec3 diagA = origin.add(look.add(perp).normalize().scale(dist));
            Vec3 diagB = origin.add(look.subtract(perp).normalize().scale(dist));

            hitInXPattern(center, diagA, diagB, look, perp);
            createXSlashParticles(center, look, perp, 1);
        }

        // Second X-slash: starts slightly after first, advances further
        if (t >= SLASH_2_START && t <= SLASH_2_END) {
            float progress = (float)(t - SLASH_2_START) / (SLASH_2_END - SLASH_2_START);
            float dist = 3.0f + progress * 5.0f;
            Vec3 center = origin.add(look.scale(dist));

            Vec3 diagA = origin.add(look.add(perp).normalize().scale(dist));
            Vec3 diagB = origin.add(look.subtract(perp).normalize().scale(dist));

            hitInXPattern(center, diagA, diagB, look, perp);
            createXSlashParticles(center, look, perp, 2);
        }
    }

    private void hitInXPattern(Vec3 center, Vec3 diagA, Vec3 diagB, Vec3 look, Vec3 perp) {
        List<LivingEntity> targets = getTargetsInCustomHitbox(center, 2.5f, HitboxData.HitboxShape.WIDE);
        for (LivingEntity t : targets) {
            hitTarget(t);
        }
        List<LivingEntity> targetsA = getTargetsInCustomHitbox(diagA, 1.5f, HitboxData.HitboxShape.CUBE);
        for (LivingEntity t : targetsA) hitTarget(t);
        List<LivingEntity> targetsB = getTargetsInCustomHitbox(diagB, 1.5f, HitboxData.HitboxShape.CUBE);
        for (LivingEntity t : targetsB) hitTarget(t);
    }

    private void createXSlashParticles(Vec3 center, Vec3 look, Vec3 perp, int slashNum) {
        if (!(world instanceof ServerLevel sl)) return;

        float spread = 2.5f;
        for (int i = -3; i <= 3; i++) {
            double t = i / 3.0;
            // Diagonal 1: top-left to bottom-right
            Vec3 p1 = center.add(look.scale(t * 0.5)).add(perp.scale(t * spread));
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p1.x, p1.y, p1.z, 1, 0.05, 0.05, 0.05, 0);

            // Diagonal 2: top-right to bottom-left
            Vec3 p2 = center.add(look.scale(t * 0.5)).add(perp.scale(-t * spread));
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p2.x, p2.y, p2.z, 1, 0.05, 0.05, 0.05, 0);

            sl.sendParticles(ParticleTypes.CRIT, p1.x, p1.y, p1.z, 1, 0.1, 0.1, 0.1, 0.05);
            sl.sendParticles(ParticleTypes.CRIT, p2.x, p2.y, p2.z, 1, 0.1, 0.1, 0.1, 0.05);
        }

        if (slashNum == 1 || tickCount % 2 == 0) {
            world.playSound(null, center.x, center.y, center.z,
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.3f + slashNum * 0.1f);
        }
    }

    @Override
    protected void onStop() {}
}
