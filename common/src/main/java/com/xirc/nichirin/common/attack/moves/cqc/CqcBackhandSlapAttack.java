package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CqcBackhandSlapAttack extends AbstractCqcAttack {
    public CqcBackhandSlapAttack() { super("backhand_slap"); }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.75f, 0.85f);
        forwardBurst(world, user, ParticleTypes.SWEEP_ATTACK, 1, 0.16, 0.0);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        super.onHitTarget(user, target, world);
        sidestepTarget(user, target, 0.65);
        weakenTarget(target, 20, 0);
    }
}
