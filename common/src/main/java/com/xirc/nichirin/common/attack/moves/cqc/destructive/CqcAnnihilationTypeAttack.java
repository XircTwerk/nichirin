package com.xirc.nichirin.common.attack.moves.cqc.destructive;

import com.xirc.nichirin.common.attack.moves.cqc.AbstractCqcAttack;
import com.xirc.nichirin.common.attack.moves.demon.destructive.IDestructiveDeathCQC;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Annihilation Type — committed forward palm dash that fires twin circular shockwaves on swing
 * start. Heavy damage, high knockback, slow recovery. DD-enhanced via {@link IDestructiveDeathCQC}.
 */
public class CqcAnnihilationTypeAttack extends AbstractCqcAttack implements IDestructiveDeathCQC {

    public CqcAnnihilationTypeAttack() {
        super("annihilation_type");
    }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.5f);
        forwardBurst(world, user, ParticleTypes.EXPLOSION, 4, 0.4, 0.05);

        // Signature double-circle — only when Destructive Death + Shockwave Toggle are active.
        // Outside of DD this is a plain CQC strike with no projectiles.
        if (isShockwaveEnabled(user)) {
            spawnForwardShockwave(user, world, damage * 0.8f, 1.6f, 26, 1.6f, 1);
            spawnForwardShockwave(user, world, damage * 0.8f, 1.6f, 26, 1.6f, 1);
            spawnForwardShockwave(user, world, damage * 1.1f, 2.0f, 30, 2.0f, 2);
        }
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        playUserSound(world, target, SoundEvents.PLAYER_ATTACK_CRIT, 1.4f, 0.5f);
        hitBurst(world, target, ParticleTypes.EXPLOSION_EMITTER, 1, 0.0, 0.0, 0.0, 0.0);
        hitBurst(world, target, ParticleTypes.LARGE_SMOKE, 14, 0.4, 0.3, 0.4, 0.1);
    }
}
