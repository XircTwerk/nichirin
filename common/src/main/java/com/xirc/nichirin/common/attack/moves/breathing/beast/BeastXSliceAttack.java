package com.xirc.nichirin.common.attack.moves.breathing.beast;

import com.xirc.nichirin.common.util.HitboxData;
import com.xirc.nichirin.common.vfx.VfxIds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Second Fang: Slice. Two X-shaped slashes that advance forward over ticks.
public class BeastXSliceAttack extends BeastBreathingAttackBase {

    private static final int SLASH_1_START = 1;
    private static final int SLASH_1_END = 6;
    private static final int SLASH_2_START = 5;
    private static final int SLASH_2_END = 10;

    @Override
    protected void onStart() {
        playSlashSound();
        playBeastVfx(VfxIds.BEAST_X_SLICE,
                user.position().add(0, user.getBbHeight() * 0.5, 0), user.getLookAngle(), 1.0f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        int t = tickCount;
        Vec3 origin = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 look = user.getLookAngle();
        Vec3 perp = new Vec3(-look.z, 0, look.x).normalize();

        if (t >= SLASH_1_START && t <= SLASH_1_END) {
            float progress = (float)(t - SLASH_1_START) / (SLASH_1_END - SLASH_1_START);
            float dist = 1.0f + progress * 4.0f;
            Vec3 center = origin.add(look.scale(dist));

            Vec3 diagA = origin.add(look.add(perp).normalize().scale(dist));
            Vec3 diagB = origin.add(look.subtract(perp).normalize().scale(dist));

            hitInXPattern(center, diagA, diagB, look, perp);
            playXSlashSound(center, 1);
        }

        if (t >= SLASH_2_START && t <= SLASH_2_END) {
            float progress = (float)(t - SLASH_2_START) / (SLASH_2_END - SLASH_2_START);
            float dist = 3.0f + progress * 5.0f;
            Vec3 center = origin.add(look.scale(dist));

            Vec3 diagA = origin.add(look.add(perp).normalize().scale(dist));
            Vec3 diagB = origin.add(look.subtract(perp).normalize().scale(dist));

            hitInXPattern(center, diagA, diagB, look, perp);
            playXSlashSound(center, 2);
        }
    }

    private void hitInXPattern(Vec3 center, Vec3 diagA, Vec3 diagB, Vec3 look, Vec3 perp) {
        List<LivingEntity> targets = getTargetsInCustomHitbox(center, hitboxSize * 1.25f, HitboxData.HitboxShape.CUBE);
        for (LivingEntity t : targets) {
            hitTarget(t);
        }
        List<LivingEntity> targetsA = getTargetsInCustomHitbox(diagA, hitboxSize * 0.75f, HitboxData.HitboxShape.CUBE);
        for (LivingEntity t : targetsA) hitTarget(t);
        List<LivingEntity> targetsB = getTargetsInCustomHitbox(diagB, hitboxSize * 0.75f, HitboxData.HitboxShape.CUBE);
        for (LivingEntity t : targetsB) hitTarget(t);
    }

    private void playXSlashSound(Vec3 center, int slashNum) {
        if (slashNum == 1 || tickCount % 2 == 0) {
            world.playSound(null, center.x, center.y, center.z,
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.3f + slashNum * 0.1f);
        }
    }

    @Override
    protected void onStop() {}
}
