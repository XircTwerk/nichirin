package com.xirc.nichirin.common.attack.moves.cqc.destructive;

import com.xirc.nichirin.common.attack.moves.cqc.AbstractCqcAttack;
import com.xirc.nichirin.common.attack.moves.demon.destructive.IDestructiveDeathCQC;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Eight-Layered Demon Core — eight rapid punches. Each "punch beat" during the active window
 * fires its own forward shockwave so the volley reads as eight distinct strikes overlapping in
 * time rather than one ring spawned at the swing start.
 */
public class CqcEightLayeredDemonCoreAttack extends AbstractCqcAttack implements IDestructiveDeathCQC {

    private static final int TOTAL_PUNCHES = 8;
    private static final int TICKS_BETWEEN_PUNCHES = 3;

    private int punchesFired = 0;
    private int activeTickElapsed = 0;

    public CqcEightLayeredDemonCoreAttack() {
        super("eight_layered_demon_core");
    }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        punchesFired = 0;
        activeTickElapsed = 0;
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_STRONG, 1.4f, 0.6f);
        forwardBurst(world, user, ParticleTypes.EXPLOSION, 3, 0.4, 0.05);
        firePunch(user, world); // punch 1 lands on the very first active tick
    }

    @Override
    protected void onActiveTick(LivingEntity user, Level world) {
        activeTickElapsed++;
        if (punchesFired >= TOTAL_PUNCHES) return;
        if (activeTickElapsed >= TICKS_BETWEEN_PUNCHES) {
            activeTickElapsed = 0;
            firePunch(user, world);
        }
    }

    /** One "punch beat": small swing burst + a forward shockwave when the toggle is on. */
    private void firePunch(LivingEntity user, Level world) {
        forwardBurst(world, user, ParticleTypes.CRIT, 4, 0.2, 0.08);
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.4f, 1.6f);

        if (isShockwaveEnabled(user)) {
            // Slight speed variance per beat so the chain reads as separate rings, not a column.
            float speedScale = 1.8f + (punchesFired % 3) * 0.12f;
            spawnForwardShockwave(user, world, damage * 0.55f, speedScale, 22, 1.2f, 1);
        }
        punchesFired++;
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        playUserSound(world, target, SoundEvents.PLAYER_ATTACK_CRIT, 1.3f, 0.5f);
        hitBurst(world, target, ParticleTypes.EXPLOSION, 6, 0.3, 0.25, 0.3, 0.05);
        hitBurst(world, target, ParticleTypes.LARGE_SMOKE, 10, 0.3, 0.25, 0.3, 0.08);
    }
}
