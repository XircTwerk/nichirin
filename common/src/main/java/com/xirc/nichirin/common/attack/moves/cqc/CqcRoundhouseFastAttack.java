package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CqcRoundhouseFastAttack extends AbstractCqcAttack {
    public CqcRoundhouseFastAttack() { super("roundhouse_fast"); }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_SWEEP, 0.85f, 1.45f);
        forwardBurst(world, user, ParticleTypes.SWEEP_ATTACK, 2, 0.3, 0.0);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        super.onHitTarget(user, target, world);
        slowTarget(target, 30, 0);
    }
}
