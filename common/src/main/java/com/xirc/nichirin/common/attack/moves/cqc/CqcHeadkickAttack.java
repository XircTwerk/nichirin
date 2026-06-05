package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CqcHeadkickAttack extends AbstractCqcAttack {
    public CqcHeadkickAttack() { super("headkick"); }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 0.85f);
        forwardBurst(world, user, ParticleTypes.SWEEP_ATTACK, 2, 0.25, 0.0);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        super.onHitTarget(user, target, world);
        weakenTarget(target, 30, 0);
        shoveTarget(user, target, 0.65, 0.15);
    }
}
