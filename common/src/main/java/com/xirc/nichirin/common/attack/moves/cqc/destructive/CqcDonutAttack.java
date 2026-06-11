package com.xirc.nichirin.common.attack.moves.cqc.destructive;

import com.xirc.nichirin.common.attack.moves.cqc.AbstractCqcAttack;
import com.xirc.nichirin.common.attack.moves.demon.destructive.IDestructiveDeathCQC;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Donut — single-target arm-thrust. The user drives their arm through the target's torso,
 * dealing massive damage and leaving them impaled (heavily slowed) for the full hit-stun window.
 */
public class CqcDonutAttack extends AbstractCqcAttack implements IDestructiveDeathCQC {

    public CqcDonutAttack() {
        super("donut");
    }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_STRONG, 1.8f, 0.35f);
        forwardBurst(world, user, ParticleTypes.EXPLOSION_EMITTER, 1, 0.0, 0.0);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        playTargetSound(world, target, SoundEvents.PLAYER_ATTACK_CRIT, 1.6f, 0.4f);
        playTargetSound(world, target, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.3f);
        hitBurst(world, target, ParticleTypes.EXPLOSION_EMITTER, 2, 0.0, 0.0, 0.0, 0.0);
        hitBurst(world, target, ParticleTypes.EXPLOSION, 8, 0.4, 0.5, 0.4, 0.05);
        hitBurst(world, target, ParticleTypes.CRIT, 30, 0.5, 0.4, 0.5, 0.2);
        // Impalement: target is pinned in place for the full stun window.
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, hitStun * 2, 3, false, true, true));
    }
}
