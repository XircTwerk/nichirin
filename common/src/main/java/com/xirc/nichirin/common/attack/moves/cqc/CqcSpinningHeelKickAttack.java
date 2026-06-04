package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CqcSpinningHeelKickAttack extends AbstractCqcAttack {
    public CqcSpinningHeelKickAttack() { super("spinning_heel_kick"); }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.65f);
        forwardBurst(world, user, ParticleTypes.SWEEP_ATTACK, 3, 0.45, 0.0);
        forwardBurst(world, user, ParticleTypes.CLOUD, 8, 0.35, 0.04);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        super.onHitTarget(user, target, world);
        launchTarget(user, target, 0.45, 0.75);
    }
}
