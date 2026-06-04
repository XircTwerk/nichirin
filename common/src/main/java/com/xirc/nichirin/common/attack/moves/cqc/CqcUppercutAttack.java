package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CqcUppercutAttack extends AbstractCqcAttack {
    public CqcUppercutAttack() { super("uppercut"); }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_CRIT, 0.9f, 0.9f);
        forwardBurst(world, user, ParticleTypes.CLOUD, 8, 0.18, 0.08);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        super.onHitTarget(user, target, world);
        launchTarget(user, target, 0.45, 0.18);
        hitBurst(world, target, ParticleTypes.CRIT, 12, 0.15, 0.45, 0.15, 0.12);
        playTargetSound(world, target, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f);
    }
}
