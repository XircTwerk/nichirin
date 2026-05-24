package com.xirc.nichirin.common.attack.moves.breathing.beast;

import com.xirc.nichirin.common.util.HitboxData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Fourth Fang: Slice 'n' Dice. 8 rapid diagonal slashes that bypass immunity frames.
public class BeastSliceNDiceAttack extends BeastBreathingAttackBase {

    private static final int TICKS_PER_SLASH = 2;

    private int totalSlashes;
    private int lastSlashTick = -1;

    @Override
    protected void onStart() {
        totalSlashes = Math.max(1, duration / TICKS_PER_SLASH);
        lastSlashTick = -1;
        playSlashSound();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        int activeTick = tickCount - windup - 1; // ticks since perform() first ran
        if (activeTick < 0) return;

        int slashIndex = activeTick / TICKS_PER_SLASH;
        if (slashIndex >= totalSlashes) return;

        if (lastSlashTick < 0 || activeTick / TICKS_PER_SLASH != (lastSlashTick - windup - 1) / TICKS_PER_SLASH) {
            lastSlashTick = tickCount;
            executeSlash(slashIndex);
        }
    }

    private void executeSlash(int slashIndex) {
        Vec3 origin = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 look = user.getLookAngle();
        Vec3 perp = new Vec3(-look.z, 0, look.x).normalize();

        double diagOffset = (slashIndex % 2 == 0) ? 1.0 : -1.0;
        Vec3 diagDir = look.add(perp.scale(diagOffset)).normalize();
        Vec3 slashCenter = origin.add(diagDir.scale(1.5));

        List<LivingEntity> targets = getTargetsInCustomHitbox(slashCenter, 2.0f, HitboxData.HitboxShape.WIDE);
        for (LivingEntity target : targets) {
            hitTargetNoImmunity(target);
        }

        createRapidSlashEffect(slashCenter, diagDir);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.6f, 1.2f + (slashIndex * 0.05f));
    }

    private void createRapidSlashEffect(Vec3 center, Vec3 dir) {
        if (!(world instanceof ServerLevel sl)) return;
        Vec3 perp = new Vec3(-dir.z, 0, dir.x).normalize();
        for (int i = -2; i <= 2; i++) {
            Vec3 p = center.add(perp.scale(i * 0.4));
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
            sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 2, 0.15, 0.15, 0.15, 0.08);
        }
    }

    @Override
    protected void onStop() {
        lastSlashTick = -1;
    }
}
