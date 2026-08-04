package com.xirc.nichirin.common.attack.moves.breathing.beast;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.common.vfx.VfxManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("rawtypes")
public abstract class BeastBreathingAttackBase extends AbstractBreathingAttack<BeastBreathingAttackBase, IBreathingAttacker> {

    protected void playBeastVfx(ResourceLocation effectId, Vec3 origin, Vec3 direction, float scale) {
        if (world instanceof ServerLevel serverLevel) {
            VfxManager.playAttached(serverLevel, user, effectId, origin, direction, scale);
        }
    }

    protected void playBeastVfxAt(ResourceLocation effectId, Vec3 origin, Vec3 direction, float scale) {
        if (world instanceof ServerLevel serverLevel) {
            VfxManager.playOwned(serverLevel, user, effectId, origin, direction, scale);
        }
    }

    protected void playSlashSound() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
        }
    }

    protected void playHeavySlashSound() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.9f);
        }
    }

    @Override
    protected void hitTarget(LivingEntity target) {
        super.hitTarget(target);
        playHeavySlashSound();
    }

    @Override
    protected abstract void onStart();

    @Override
    protected abstract void perform();
}
