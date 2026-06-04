package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CqcCrossAttack extends AbstractCqcAttack {
    public CqcCrossAttack() { super("cross"); }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_STRONG, 0.7f, 1.35f);
        forwardBurst(world, user, ParticleTypes.POOF, 4, 0.12, 0.04);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        super.onHitTarget(user, target, world);
        shoveTarget(user, target, 0.45, 0.08);
    }
}
