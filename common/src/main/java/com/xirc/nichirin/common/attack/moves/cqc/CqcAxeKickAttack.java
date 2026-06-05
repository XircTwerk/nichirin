package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CqcAxeKickAttack extends AbstractCqcAttack {
    public CqcAxeKickAttack() { super("axe_kick"); }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_CRIT, 0.9f, 0.7f);
        forwardBurst(world, user, ParticleTypes.CRIT, 6, 0.18, 0.05);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        super.onHitTarget(user, target, world);
        if (!target.onGround()) {
            slamTarget(target, 1.0);
        } else {
            slowTarget(target, 35, 1);
        }
        hitBurst(world, target, ParticleTypes.CLOUD, 10, 0.25, 0.12, 0.25, 0.04);
    }
}
