package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CqcLowKickAttack extends AbstractCqcAttack {
    public CqcLowKickAttack() { super("low_kick"); }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_SWEEP, 0.55f, 1.6f);
        forwardBurst(world, user, ParticleTypes.CLOUD, 5, 0.2, 0.03);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        super.onHitTarget(user, target, world);
        slowTarget(target, 55, 1);
        shoveTarget(user, target, 0.25, 0.02);
    }
}
