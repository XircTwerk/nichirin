package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CqcKneeAttack extends AbstractCqcAttack {
    public CqcKneeAttack() { super("knee"); }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_STRONG, 0.6f, 1.2f);
        forwardBurst(world, user, ParticleTypes.POOF, 4, 0.08, 0.03);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        super.onHitTarget(user, target, world);
        pullTarget(user, target, 0.25);
    }
}
