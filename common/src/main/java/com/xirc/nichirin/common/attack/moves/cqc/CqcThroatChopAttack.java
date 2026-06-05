package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CqcThroatChopAttack extends AbstractCqcAttack {
    public CqcThroatChopAttack() { super("throat_chop"); }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_CRIT, 0.55f, 1.2f);
        forwardBurst(world, user, ParticleTypes.SMOKE, 3, 0.08, 0.02);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        super.onHitTarget(user, target, world);
        weakenTarget(target, 45, 0);
        slowTarget(target, 30, 1);
    }
}
