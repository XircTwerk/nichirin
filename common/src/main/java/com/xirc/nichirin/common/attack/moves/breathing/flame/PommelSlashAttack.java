package com.xirc.nichirin.common.attack.moves.breathing.flame;

import com.xirc.nichirin.common.vfx.VfxIds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pommel Slash - Flame Breathing right-click attack
 * Performs 6 rapid alternating left-right slashes with flame effects
 */
public class PommelSlashAttack extends FlameBreathingAttackBase {

    private static final int SLASH_INTERVAL = 3;
    private static final int SLASH_COUNT = 6;

    private int totalSlashes;
    private int slashesPerformed = 0;
    private Set<LivingEntity> hitEntities = new HashSet<>();

    public PommelSlashAttack() {
    }

    @Override
    protected void onStart() {
        totalSlashes = SLASH_COUNT;
        slashesPerformed = 0;
        hitEntities.clear();

        // Flame pommel sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);

        // Initial flame buildup
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // perform() is first called the tick AFTER windup ends, so activeTick == 0 on that first
        // active tick. Fire one slash every SLASH_INTERVAL active ticks until all SLASH_COUNT are
        // done. Using slashesPerformed as the index keeps the slashes sequential (0..5).
        int activeTick = tickCount - windup - 1;
        if (activeTick < 0) return;

        if (activeTick % SLASH_INTERVAL == 0 && slashesPerformed < totalSlashes) {
            performSlash(slashesPerformed);
            slashesPerformed++;
        }
    }

    private void performSlash(int slashIndex) {
        Vec3 vfxDirection = user.getLookAngle().yRot((slashIndex & 1) == 0 ? 0.22f : -0.22f);
        playFlameVfxAt(VfxIds.FLAME_POMMEL_SLASH, user.position(), vfxDirection, 0.82f);
        // Alternate between left and right slashes
        boolean isLeftSlash = slashIndex % 2 == 0;

        // Create slash particles

        // Hit detection
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Slightly different positions for left/right slashes
        Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 slashOffset = rightDir.scale(isLeftSlash ? -0.5 : 0.5);
        Vec3 slashCenter = userPos.add(lookDir.scale(range * 0.8)).add(slashOffset);

        List<LivingEntity> targets = getTargetsInCustomHitbox(slashCenter, hitboxSize, hitboxSize * 1.25, hitboxSize * 0.75);

        for (LivingEntity target : targets) {
            // Allow multiple hits but with reduced damage after first hit
            boolean hasBeenHit = hitEntities.contains(target);

            if (hasBeenHit) {
                // Reduced damage for subsequent hits (30% damage)
                float originalDamage = damage;
                damage = damage * 0.3f;
                hitTargetNoImmunity(target);
                damage = originalDamage; // Reset damage
            } else {
                // Full damage for first hit
                hitTarget(target);
                hitEntities.add(target);
            }

            // Light knockback to keep enemies close for combo
            Vec3 lightKnockback = target.position().subtract(userPos).normalize().scale(knockback * 0.2f);
            target.push(lightKnockback.x, 0.05, lightKnockback.z);
        }

        // Slash sound with increasing pitch
        float pitch = 1.0f + (slashIndex * 0.1f);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, pitch);
    }

    private void createSlashParticles(boolean isLeftSlash) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() * 0.7, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();

        // Create arc of particles for the slash
        for (int i = -4; i <= 4; i++) {
            double angle = i * 15; // 15-degree increments for 120-degree arc
            double radians = Math.toRadians(angle);

            // Alternate direction based on left/right slash
            double actualAngle = isLeftSlash ? radians : -radians;

            Vec3 slashDir = lookDir.yRot((float)actualAngle);
            Vec3 particlePos = userPos.add(slashDir.scale(range * 0.8));

            // Flame particles for slashes
}

        // Central flame burst
        Vec3 centerPos = userPos.add(lookDir.scale(range * 0.6));
}

    protected void createFlameParticles() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Flame aura around user during pommel strikes
        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * 2 * Math.PI;
            double radius = 1.0 + Math.sin(tickCount * 0.3) * 0.2;

            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + Math.random() * 1.5;
}
    }

    @Override
    protected void onStop() {
        // Final flame discharge
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
}

        // Clear hit tracking
        hitEntities.clear();
        slashesPerformed = 0;
    }
}
