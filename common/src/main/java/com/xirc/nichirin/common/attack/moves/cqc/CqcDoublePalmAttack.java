package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CqcDoublePalmAttack extends AbstractCqcAttack {
    public CqcDoublePalmAttack() { super("double_palm"); }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.95f, 0.9f);
        forwardBurst(world, user, ParticleTypes.POOF, 10, 0.22, 0.08);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        super.onHitTarget(user, target, world);
        shoveTarget(user, target, 1.15, 0.12);
        slowTarget(target, 16, 0);
    }
}
