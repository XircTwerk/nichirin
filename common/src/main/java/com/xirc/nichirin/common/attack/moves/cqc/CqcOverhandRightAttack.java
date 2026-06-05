package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class CqcOverhandRightAttack extends AbstractCqcAttack {
    public CqcOverhandRightAttack() { super("overhand_right"); }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_CRIT, 0.8f, 0.75f);
        forwardBurst(world, user, ParticleTypes.CRIT, 5, 0.15, 0.06);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        super.onHitTarget(user, target, world);
        if (!target.onGround()) {
            slamTarget(target, 0.65);
            hitBurst(world, target, ParticleTypes.CLOUD, 8, 0.2, 0.08, 0.2, 0.04);
        }
    }
}
