package com.xirc.nichirin.common.attack.moves.breathing.beast;

import com.xirc.nichirin.common.util.HitboxData;
import com.xirc.nichirin.common.vfx.VfxIds;
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
        if (slashIndex == 0) playBeastVfx(VfxIds.BEAST_RAPID_SLASH, origin, look, 1.0f);

        List<LivingEntity> targets = getTargetsInCustomHitbox(slashCenter, hitboxSize, HitboxData.HitboxShape.CUBE);
        for (LivingEntity target : targets) {
            hitTargetNoImmunity(target);
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.6f, 1.2f + (slashIndex * 0.05f));
    }

    @Override
    protected void onStop() {
        lastSlashTick = -1;
    }
}
