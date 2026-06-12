package com.xirc.nichirin.common.attack.moves.cqc.destructive;

import com.xirc.nichirin.common.attack.moves.cqc.AbstractCqcAttack;
import com.xirc.nichirin.common.attack.moves.demon.destructive.IDestructiveDeathCQC;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Leg Type — Explosive Flurry: rapid-fire straight kicks. Fires a shockwave on every kick beat so
 * the swept area is sustained for the active window.
 */
public class CqcExplosiveFlurryAttack extends AbstractCqcAttack implements IDestructiveDeathCQC {

    public CqcExplosiveFlurryAttack() {
        super("explosive_flurry");
    }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.2f);
        forwardBurst(world, user, ParticleTypes.CRIT, 10, 0.3, 0.12);

        if (isShockwaveEnabled(user)) {
            // Burst of three shockwaves with stepping delays approximated via different speeds.
            spawnForwardShockwave(user, world, damage * 0.55f, 2.2f, 14, 0.9f, 2);
            spawnForwardShockwave(user, world, damage * 0.55f, 2.4f, 14, 0.9f, 2);
            spawnForwardShockwave(user, world, damage * 0.55f, 2.6f, 14, 0.9f, 2);
        }
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        playUserSound(world, target, SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.6f, 1.5f);
        hitBurst(world, target, ParticleTypes.CRIT, 6, 0.25, 0.2, 0.25, 0.1);
    }
}
