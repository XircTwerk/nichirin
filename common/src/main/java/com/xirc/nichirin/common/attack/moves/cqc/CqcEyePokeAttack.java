package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CqcEyePokeAttack extends AbstractCqcAttack {
    public CqcEyePokeAttack() { super("eye_poke"); }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.45f, 1.9f);
        forwardBurst(world, user, ParticleTypes.CRIT, 3, 0.06, 0.02);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        super.onHitTarget(user, target, world);
        blindTarget(target, 35);
        playTargetSound(world, target, SoundEvents.PLAYER_HURT, 0.45f, 1.8f);
    }
}
