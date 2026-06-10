package com.xirc.nichirin.common.attack.moves.cqc.destructive;

import com.xirc.nichirin.common.attack.moves.cqc.AbstractCqcAttack;
import com.xirc.nichirin.common.attack.moves.demon.destructive.IDestructiveDeathCQC;
import com.xirc.nichirin.common.entity.attack.ShockwaveEntity;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Crushing Type — Ten Thousand Leaves Flashing Willow: airborne dive-punch. Spawns a ring of
 * shockwaves outward from the impact point so it reads as crowd-control AoE.
 */
public class CqcTenThousandLeavesFlashingWillowAttack extends AbstractCqcAttack implements IDestructiveDeathCQC {

    private static final int RING_SHOCKWAVES = 8;

    public CqcTenThousandLeavesFlashingWillowAttack() {
        super("ten_thousand_leaves_flashing_willow");
    }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.7f);
        forwardBurst(world, user, ParticleTypes.EXPLOSION, 1, 0.0, 0.0);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        playUserSound(world, target, SoundEvents.PLAYER_ATTACK_CRIT, 1.5f, 0.5f);
        hitBurst(world, target, ParticleTypes.EXPLOSION_EMITTER, 1, 0.0, 0.0, 0.0, 0.0);
        hitBurst(world, target, ParticleTypes.CRIT, 24, 0.5, 0.3, 0.5, 0.2);

        if (isShockwaveEnabled(user)) {
            // Ring of shockwaves spreading outward from the impact (CC sweep). Use the user's
            // facing for "0°"; the shockwave system spins them off uniformly around the horizon.
            Vec3 forward = user.getLookAngle().normalize();
            double baseAngle = Math.atan2(forward.z, forward.x);
            for (int i = 0; i < RING_SHOCKWAVES; i++) {
                double angle = baseAngle + (i / (double) RING_SHOCKWAVES) * Math.PI * 2.0;
                Vec3 dir = new Vec3(Math.cos(angle), 0, Math.sin(angle));
                new ShockwaveEntity.Builder()
                        .owner(user)
                        .origin(target.position().add(0, target.getBbHeight() * 0.4, 0))
                        .direction(dir)
                        .damage(damage * 0.45f)
                        .speed(1.8f)
                        .lifeTicks(22)
                        .hitboxRadius(1.0f)
                        .pierces(1)
                        .red(isOverdriveActive(user))
                        .spawn(NichirinEntityRegistry.SHOCKWAVE.get(), world);
            }
        }
    }
}
