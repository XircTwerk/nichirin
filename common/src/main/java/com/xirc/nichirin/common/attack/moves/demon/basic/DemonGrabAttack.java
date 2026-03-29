package com.xirc.nichirin.common.attack.moves.demon.basic;

import com.xirc.nichirin.common.system.GrabManager;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import com.xirc.nichirin.registry.NichirinParticleRegistry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Demon grab attack.
 * <ul>
 *   <li>First activation: reaches forward and grabs the nearest target within range.
 *       The target is then held in front of the demon each tick by {@link GrabManager}.</li>
 *   <li>Second activation (while already grabbing): releases the target with a powerful launch.</li>
 * </ul>
 */
public class DemonGrabAttack {

    private static final float GRAB_RANGE    = 2.0f;
    private static final float GRAB_HITBOX   = 1.5f;
    private static final int   GRAB_DURATION = 40; // 2 seconds max hold

    public void execute(LivingEntity demon) {
        if (demon.level().isClientSide()) return;

        // Second activation: launch the held target
        if (GrabManager.isGrabbing(demon)) {
            GrabManager.releaseGrab(demon, true);
            return;
        }

        // First activation: show the reach hitbox with particles, then check for a target
        Vec3 forward    = demon.getLookAngle();
        Vec3 grabCenter = demon.position()
                .add(forward.scale(GRAB_RANGE))
                .add(0, demon.getBbHeight() * 0.4, 0);

        AABB hitbox = new AABB(
                grabCenter.x - GRAB_HITBOX, grabCenter.y - GRAB_HITBOX, grabCenter.z - GRAB_HITBOX,
                grabCenter.x + GRAB_HITBOX, grabCenter.y + GRAB_HITBOX, grabCenter.z + GRAB_HITBOX);

        // Show hitbox when F3+B debug view is on
        NichirinPacketRegistry.sendHitboxToTracking(demon, hitbox, 300L);

        // Reach particles
        if (demon.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    grabCenter.x, grabCenter.y, grabCenter.z, 6, 0.3, 0.3, 0.3, 0.0);
            sl.sendParticles(ParticleTypes.POOF,
                    grabCenter.x, grabCenter.y, grabCenter.z, 4, 0.2, 0.2, 0.2, 0.05);
        }

        List<LivingEntity> targets = demon.level().getEntitiesOfClass(
                LivingEntity.class, hitbox,
                e -> e != demon && e.isAlive());

        if (!targets.isEmpty()) {
            LivingEntity target = targets.get(0);
            GrabManager.startGrab(demon, target, GRAB_DURATION);
            // Impact particles on the grabbed target
            if (demon.level() instanceof ServerLevel sl) {
                Vec3 tp = target.position().add(0, target.getBbHeight() * 0.5, 0);
                sl.sendParticles(NichirinParticleRegistry.SLASH_IMPACT_SPARK.get(),
                        tp.x, tp.y, tp.z, 10, 0.2, 0.2, 0.2, 0.0);
                sl.sendParticles(ParticleTypes.CRIT,
                        tp.x, tp.y, tp.z, 6, 0.25, 0.25, 0.25, 0.05);
            }
        }
    }
}
