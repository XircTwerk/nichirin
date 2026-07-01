package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CqcJabAttack extends AbstractCqcAttack {
    public CqcJabAttack() { super("jab"); }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.8f); // swing sound
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        super.onHitTarget(user, target, world); // standard CQC impact sound + particle burst
        pullTarget(user, target, 0.18);
        slowTarget(target, 16, 0);
    }
}
