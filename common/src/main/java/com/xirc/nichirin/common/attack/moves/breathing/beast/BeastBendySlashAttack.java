package com.xirc.nichirin.common.attack.moves.breathing.beast;

import com.xirc.nichirin.common.util.HitboxData;
import com.xirc.nichirin.common.vfx.VfxIds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Ninth Fang: Extending Bendy Slash. Long-range forward sweep, covers everything in front in one wide slash.
public class BeastBendySlashAttack extends BeastBreathingAttackBase {

    private boolean slashDone = false;

    @Override
    protected void onStart() {
        slashDone = false;
    }

    @Override
    protected void onActiveStart() {
        playHeavySlashSound();
    }

    @Override
    protected void perform() {
        if (world.isClientSide || slashDone) return;

        executeFrontalSlash();
        slashDone = true;
    }

    private void executeFrontalSlash() {
        Vec3 origin = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 look = user.getLookAngle();
        playBeastVfx(VfxIds.BEAST_BENDY_SLASH, origin, look, Math.max(0.75f, range / 9.0f));

        for (int dist = 1; dist <= (int) range; dist++) {
            Vec3 center = origin.add(look.scale(dist));
            List<LivingEntity> targets = getTargetsInCustomHitbox(center, hitboxSize, HitboxData.HitboxShape.CUBE);
            for (LivingEntity target : targets) {
                hitTarget(target);
            }
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.6f);
    }

    @Override
    protected void onStop() {
        slashDone = false;
    }
}
