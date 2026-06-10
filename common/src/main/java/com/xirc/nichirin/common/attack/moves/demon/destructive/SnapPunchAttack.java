package com.xirc.nichirin.common.attack.moves.demon.destructive;

import com.xirc.nichirin.common.attack.moves.cqc.AbstractCqcAttack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Snap Punch — a CQC move (anyone with CQC can use it) that gains Destructive Death enhancements.
 *
 * <p>Base CQC behaviour comes from {@link AbstractCqcAttack}; the {@link IDestructiveDeathCQC}
 * interface adds the "extra shockwave / red tint" when the user has Destructive Death equipped. The
 * basic per-hit chip shockwave from {@code DestructiveDeathCqcHook} runs for all CQC attacks; this
 * subclass goes further by also spawning a bigger forward shockwave on swing start when the toggle
 * is on (so even a whiff fires one).</p>
 */
public class SnapPunchAttack extends AbstractCqcAttack implements IDestructiveDeathCQC {

    public SnapPunchAttack() {
        super("snap_punch");
    }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_STRONG, 0.9f, 1.1f);
        forwardBurst(world, user, ParticleTypes.CRIT, 6, 0.18, 0.08);

        // Snap Punch's signature: when DD's Shockwave toggle is on, the punch ALSO launches a small
        // forward shockwave on the start of the active frame — fires even on a whiff.
        if (isShockwaveEnabled(user)) {
            spawnForwardShockwave(user, world, damage * 0.65f, 0.7f, 22, 1.0f, 0);
        }
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        Vec3 impact = user.position()
                .add(0, user.getBbHeight() * 0.5, 0)
                .add(user.getLookAngle().scale(range));
        world.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.1f, 0.7f);
        hitBurst(world, target, ParticleTypes.CRIT, 12, 0.3, 0.2, 0.3, 0.12);

        if (world instanceof ServerLevel sl && compassBuffsTarget(user, target)) {
            // Compass-tracked impact gets an extra blue flash on hit.
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                    6, 0.2, 0.3, 0.2, 0.05);
        }
    }
}
