package com.xirc.nichirin.common.attack.moves.demon.destructive;

import com.xirc.nichirin.common.entity.attack.ShockwaveEntity;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Destructive Death ultimate — Blue Silver Chaotic Afterglow.
 *
 * <p>Wiki: "After utilizing Compass Needle, Akaza unleashes an omni-directional barrage of
 * countless thin and sharp shockwaves that deliver a hundred blows to his target coming from
 * everywhere almost at the same time."</p>
 *
 * <p>Mechanically: requires Compass Needle to be active; finds the nearest Compass-tracked target;
 * spawns 12 clone "anchor points" in a sphere around the user; from each anchor sends an
 * afterimage streak converging on the target, and fires a fast piercing shockwave from that
 * anchor toward the target. Result reads as 12 clones swarming the target from every angle.</p>
 */
public class BlueSilverChaoticAfterglowAttack extends DestructiveDeathAttackBase {

    private static final int CLONE_COUNT = 12;
    private static final float CLONE_RING_RADIUS = 6.0f;
    private static final float CLONE_DAMAGE = 8.0f;
    private static final float CLONE_SPEED = 3.2f;
    private static final int CLONE_LIFE_TICKS = 18;
    private static final float CLONE_HITBOX = 1.0f;
    private static final int CLONE_PIERCES = 6;
    private static final int CLONE_HIT_STUN = 14;
    private static final int AFTERIMAGE_LIFETIME_TICKS = 14;
    private static final int AFTERIMAGE_COPIES = 5;
    private static final float AFTERIMAGE_ALPHA = 0.7f;

    private boolean hasExecuted = false;

    @Override
    protected void onStart() {
        hasExecuted = false;
    }

    @Override
    protected void perform() {
        if (hasExecuted) return;
        hasExecuted = true;
        if (!(user instanceof ServerPlayer sp)) return;

        long now = world.getGameTime();
        if (!DestructiveDeathState.isCompassActive(sp.getUUID(), now)) {
            sp.displayClientMessage(
                    Component.literal("Compass Needle must be active!")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return;
        }

        LivingEntity target = nearestTrackedTarget(sp);
        if (target == null) {
            sp.displayClientMessage(
                    Component.literal("No tracked target in range!")
                            .withStyle(s -> s.withColor(0xFF5555)), true);
            return;
        }

        Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.55, 0);
        for (int i = 0; i < CLONE_COUNT; i++) {
            double yaw = (i / (double) CLONE_COUNT) * Math.PI * 2.0;
            // Spread clones across yaw + a small vertical offset so it reads as 3D, not a flat ring.
            double pitch = ((i % 3) - 1) * 0.35; // -0.35, 0, +0.35
            Vec3 offset = new Vec3(Math.cos(yaw), Math.sin(pitch), Math.sin(yaw))
                    .normalize().scale(CLONE_RING_RADIUS);
            Vec3 spawn = sp.position().add(0, sp.getBbHeight() * 0.55, 0).add(offset);

            Vec3 dir = targetPos.subtract(spawn).normalize();

            // Afterimage trail: ghosts of the player converging from each anchor onto the target.
            NichirinPacketRegistry.sendAfterimageTrail(sp, spawn, targetPos,
                    AFTERIMAGE_LIFETIME_TICKS, AFTERIMAGE_COPIES, AFTERIMAGE_ALPHA);

            // Shockwave from each anchor, aimed at the target.
            new ShockwaveEntity.Builder()
                    .owner(sp)
                    .origin(spawn)
                    .direction(dir)
                    .damage(CLONE_DAMAGE * compassFinalScalar())
                    .knockback(0.25f)
                    .hitStun(CLONE_HIT_STUN)
                    .speed(CLONE_SPEED)
                    .lifeTicks(CLONE_LIFE_TICKS)
                    .hitboxRadius(CLONE_HITBOX)
                    .pierces(CLONE_PIERCES)
                    .phaseWalls(true) // converging clones shouldn't get clipped by terrain
                    .red(isOverdriveActive())
                    .spawn(NichirinEntityRegistry.SHOCKWAVE.get(), world);
        }

        // Boom sound at the target so the hit is anchored to where it lands.
        world.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.6f, 0.7f);
        // Soft impact ring at the player too.
        if (world instanceof ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SONIC_BOOM,
                    sp.getX(), sp.getY() + sp.getBbHeight() * 0.55, sp.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** Closest Compass-tracked entity in range; null when nothing is tracked or in reach. */
    private LivingEntity nearestTrackedTarget(ServerPlayer sp) {
        Set<UUID> tracked = CompassNeedleTracker.getTrackedTargets(sp.getUUID());
        if (tracked.isEmpty()) return null;
        AABB search = sp.getBoundingBox().inflate(30.0);
        List<LivingEntity> candidates = world.getEntitiesOfClass(LivingEntity.class, search,
                e -> e != sp && e.isAlive() && tracked.contains(e.getUUID()));
        LivingEntity best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (LivingEntity e : candidates) {
            double d = e.distanceToSqr(sp);
            if (d < bestDistSq) {
                bestDistSq = d;
                best = e;
            }
        }
        return best;
    }

    /** +35% damage scalar while Compass Overdrive is on. */
    private float compassFinalScalar() {
        if (!(user instanceof ServerPlayer sp)) return 1.0f;
        return DestructiveDeathState.isCompassOverdriveActive(sp.getUUID(), world.getGameTime()) ? 1.35f : 1.0f;
    }

    @Override
    protected void onStop() {
        hasExecuted = false;
    }
}
