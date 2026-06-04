package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CqcSpinningBackfistAttack extends AbstractCqcAttack {
    public CqcSpinningBackfistAttack() { super("spinning_backfist"); }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_SWEEP, 0.85f, 0.95f);
        forwardBurst(world, user, ParticleTypes.SWEEP_ATTACK, 2, 0.35, 0.0);
        forwardBurst(world, user, ParticleTypes.CLOUD, 5, 0.22, 0.03);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        super.onHitTarget(user, target, world);
        sidestepTarget(user, target, 0.85);
    }
}
