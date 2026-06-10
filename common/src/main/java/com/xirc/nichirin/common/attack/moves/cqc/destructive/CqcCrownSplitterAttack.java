package com.xirc.nichirin.common.attack.moves.cqc.destructive;

import com.xirc.nichirin.common.attack.moves.cqc.AbstractCqcAttack;
import com.xirc.nichirin.common.attack.moves.demon.destructive.IDestructiveDeathCQC;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Leg Type — Crown Splitter: rising reverse axe-kick. Single high-damage hit; produces a vertical
 * shockwave from the foot's connection point.
 */
public class CqcCrownSplitterAttack extends AbstractCqcAttack implements IDestructiveDeathCQC {

    public CqcCrownSplitterAttack() {
        super("crown_splitter");
    }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.85f);
        forwardBurst(world, user, ParticleTypes.SWEEP_ATTACK, 1, 0.0, 0.0);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        playUserSound(world, target, SoundEvents.PLAYER_ATTACK_CRIT, 1.3f, 0.7f);
        hitBurst(world, target, ParticleTypes.CRIT, 18, 0.25, 0.4, 0.25, 0.18);
        hitBurst(world, target, ParticleTypes.CLOUD, 6, 0.3, 0.2, 0.3, 0.05);

        if (isShockwaveEnabled(user)) {
            // The "splitter" wave goes UPWARD from the impact — single bigger shockwave skewed up.
            spawnForwardShockwave(user, world, damage * 0.7f, 1.6f, 18, 1.4f, 0);
            spawnForwardShockwave(user, world, damage * 0.5f, 1.9f, 20, 1.0f, 1);
        }
    }
}
