package com.xirc.nichirin.common.attack.moves.cqc.destructive;

import com.xirc.nichirin.common.attack.moves.cqc.AbstractCqcAttack;
import com.xirc.nichirin.common.attack.moves.demon.destructive.IDestructiveDeathCQC;
import com.xirc.nichirin.common.entity.attack.ShockwaveEntity;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Ten Thousand Leaves Flashing Willow — hyper-armoured torus AoE. The user pulses outward,
 * striking every entity in the ring band and firing 12 shockwaves radially.
 *
 * <p>25s windup, hyper armor, 1.5s stun on hit. Entities hugging the user inside the hole are safe.</p>
 */
public class CqcTenThousandLeavesFlashingWillowAttack extends AbstractCqcAttack implements IDestructiveDeathCQC {

    private static final float OUTER_RADIUS = 5.0f;
    private static final float INNER_RADIUS = 1.5f;
    private static final float HEIGHT = 2.5f;
    private static final int RING_SHOCKWAVES = 12;

    public CqcTenThousandLeavesFlashingWillowAttack() {
        super("ten_thousand_leaves_flashing_willow");
    }

    @Override
    protected AABB buildHitbox(LivingEntity user) {
        Vec3 c = user.position();
        return new AABB(
                c.x - OUTER_RADIUS, c.y - 0.1, c.z - OUTER_RADIUS,
                c.x + OUTER_RADIUS, c.y + HEIGHT, c.z + OUTER_RADIUS);
    }

    @Override
    protected boolean acceptTarget(LivingEntity user, LivingEntity candidate) {
        double dx = candidate.getX() - user.getX();
        double dz = candidate.getZ() - user.getZ();
        double horizDistSq = dx * dx + dz * dz;
        return horizDistSq >= INNER_RADIUS * INNER_RADIUS
                && horizDistSq <= OUTER_RADIUS * OUTER_RADIUS;
    }

    @Override
    protected void onActiveStart(LivingEntity user, Level world) {
        playUserSound(world, user, SoundEvents.PLAYER_ATTACK_STRONG, 1.6f, 0.45f);
        if (world instanceof ServerLevel sl) {
            for (int i = 0; i < 64; i++) {
                double angle = (i / 64.0) * Math.PI * 2.0;
                double cx = user.getX() + Math.cos(angle) * OUTER_RADIUS;
                double cz = user.getZ() + Math.sin(angle) * OUTER_RADIUS;
                sl.sendParticles(ParticleTypes.EXPLOSION,
                        cx, user.getY() + 0.4, cz, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        if (isShockwaveEnabled(user)) {
            for (int i = 0; i < RING_SHOCKWAVES; i++) {
                double angle = (i / (double) RING_SHOCKWAVES) * Math.PI * 2.0;
                Vec3 dir = new Vec3(Math.cos(angle), 0, Math.sin(angle));
                new ShockwaveEntity.Builder()
                        .owner(user)
                        .origin(user.position().add(0, user.getBbHeight() * 0.5, 0))
                        .direction(dir)
                        .damage(damage * 0.5f)
                        .knockback(0.45f)
                        .hitStun(hitStun)
                        .speed(2.4f)
                        .lifeTicks(20)
                        .hitboxRadius(1.0f)
                        .pierces(2)
                        .red(isOverdriveActive(user))
                        .spawn(NichirinEntityRegistry.SHOCKWAVE.get(), world);
            }
        }
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        playTargetSound(world, target, SoundEvents.PLAYER_ATTACK_CRIT, 1.4f, 0.55f);
        hitBurst(world, target, ParticleTypes.EXPLOSION, 4, 0.3, 0.25, 0.3, 0.05);
    }
}
