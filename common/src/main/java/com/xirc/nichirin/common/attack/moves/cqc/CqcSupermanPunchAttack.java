package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CqcSupermanPunchAttack extends AbstractCqcAttack {
    public CqcSupermanPunchAttack() { super("superman_punch"); }

    @Override
    protected void onStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.TRIDENT_THROW.value(), 0.75f, 1.4f);
        Vec3 look = user.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        if (horizontal.lengthSqr() > 0.0001) {
            horizontal = horizontal.normalize().scale(0.55);
            user.setDeltaMovement(horizontal.x, 0.22, horizontal.z);
            user.hurtMarked = true;
            user.hasImpulse = true;
        }
    }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        forwardBurst(world, user, ParticleTypes.CLOUD, 10, 0.22, 0.08);
        forwardBurst(world, user, ParticleTypes.CRIT, 4, 0.12, 0.06);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        super.onHitTarget(user, target, world);
        shoveTarget(user, target, 0.9, 0.18);
    }
}
