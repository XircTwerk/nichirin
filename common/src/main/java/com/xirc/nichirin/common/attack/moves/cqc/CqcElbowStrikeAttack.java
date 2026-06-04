package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CqcElbowStrikeAttack extends AbstractCqcAttack {
    public CqcElbowStrikeAttack() { super("elbow_strike"); }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_STRONG, 0.65f, 1.5f);
        forwardBurst(world, user, ParticleTypes.CRIT, 4, 0.08, 0.04);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        super.onHitTarget(user, target, world);
        weakenTarget(target, 25, 0);
    }
}
