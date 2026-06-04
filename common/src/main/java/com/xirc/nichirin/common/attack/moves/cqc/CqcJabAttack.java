package com.xirc.nichirin.common.attack.moves.cqc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CqcJabAttack extends AbstractCqcAttack {
    public CqcJabAttack() { super("jab"); }

    @Override
    protected AABB buildHitbox(LivingEntity user) {
        Vec3 origin = user.position().add(0, user.getBbHeight() * 0.42, 0);
        Vec3 center = origin.add(user.getLookAngle().scale(range));
        return new AABB(
                center.x - hitboxSize, center.y - hitboxSize * 0.65, center.z - hitboxSize,
                center.x + hitboxSize, center.y + hitboxSize * 0.65, center.z + hitboxSize
        );
    }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.8f);
        forwardBurst(world, user, ParticleTypes.POOF, 8, 0.18, 0.06);
        forwardBurst(world, user, ParticleTypes.CRIT, 6, 0.16, 0.08);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        Vec3 impact = user.position().add(0, user.getBbHeight() * 0.48, 0).add(user.getLookAngle().scale(range));
        world.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.35f, 0.6f);
        world.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.85f, 0.75f);
        hitBurst(world, target, ParticleTypes.CRIT, 18, 0.32, 0.28, 0.32, 0.16);
        hitBurst(world, target, ParticleTypes.POOF, 12, 0.32, 0.18, 0.32, 0.08);
        hitBurst(world, target, ParticleTypes.CLOUD, 8, 0.35, 0.12, 0.35, 0.05);
        pullTarget(user, target, 0.18);
        slowTarget(target, 16, 0);
    }
}
