package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CqcKneeStrikeAttack extends AbstractCqcAttack {
    public CqcKneeStrikeAttack() { super("knee_strike"); }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_STRONG, 0.7f, 1.0f);
        forwardBurst(world, user, ParticleTypes.POOF, 5, 0.1, 0.03);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        super.onHitTarget(user, target, world);
        pullTarget(user, target, 0.4);
        slowTarget(target, 18, 1);
    }
}
