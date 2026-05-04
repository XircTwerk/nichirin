package com.xirc.nichirin.common.attack.moves;

import com.xirc.nichirin.common.system.GrabManager;
import lombok.Getter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Katana Grab — uses the same {@link GrabManager} as the demon grab.
 *
 * <ol>
 *   <li>AABB scan: find the nearest enemy in range.</li>
 *   <li>{@link GrabManager#startGrab} pins the target via {@code teleportTo} each tick
 *       (exactly how the demon hold works).</li>
 *   <li>At the end of the hold, damage + custom launch fires before GrabManager releases.</li>
 * </ol>
 */
public class KatanaGrabAttack {

    private static final int   GRAB_DURATION = 16; // ticks held (~0.8 s)
    private static final int   RECOVERY      = 6;
    private static final float GRAB_RANGE    = 3.5f;
    private static final float HIT_DAMAGE    = 8.0f;
    private static final float LAUNCH_H      = 2.2f;
    private static final float LAUNCH_V      = 0.6f;
    private static final int   COOLDOWN      = 60;

    @Getter private boolean isActive  = false;
    private int             holdTick  = 0;
    private boolean         hitFired  = false;
    @Getter private final int cooldown = COOLDOWN;


    public boolean start(Player player) {
        if (player.level().isClientSide()) return false;
        if (isActive) return false;

        LivingEntity target = findTarget(player);
        if (target == null) return false;

        GrabManager.startGrab(player, target, GRAB_DURATION);
        holdTick = 0;
        hitFired = false;
        isActive = true;

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 mid = target.position().add(0, target.getBbHeight() * 0.5, 0);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    mid.x, mid.y, mid.z, 10, 0.3, 0.3, 0.3, 0.1);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.55f);

        return true;
    }


    public void tick(Player player) {
        if (!isActive) return;
        if (player.level().isClientSide()) return;

        holdTick++;

        if (!hitFired) {
            if (holdTick < GRAB_DURATION) {
                // Let GrabManager teleport the target in place each tick
                GrabManager.tick(player);
            } else {
                // Last hold tick: grab target reference BEFORE GrabManager releases it,
                // fire the hit move, then let GrabManager clean up
                LivingEntity target = GrabManager.getGrabbedTarget(player);
                GrabManager.tick(player); // will auto-release (ticksRemaining → 0)
                if (target != null && target.isAlive()) {
                    executeHitMove(player, target);
                }
                hitFired = true;
            }
        }

        if (holdTick >= GRAB_DURATION + RECOVERY) {
            isActive = false;
        }
    }


    private void executeHitMove(Player player, LivingEntity target) {
        DamageSource src = player.damageSources().playerAttack(player);
        target.hurt(src, HIT_DAMAGE);

        Vec3 dir = target.position().subtract(player.position()).normalize();
        if (dir.lengthSqr() < 0.001) dir = player.getLookAngle();

        target.setDeltaMovement(new Vec3(dir.x * LAUNCH_H, LAUNCH_V, dir.z * LAUNCH_H));
        target.hurtMarked = true;
        target.hasImpulse = true;

        if (target instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(target));
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 mid = target.position().add(0, target.getBbHeight() * 0.5, 0);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    mid.x, mid.y, mid.z, 18, 0.3, 0.3, 0.3, 0.1);
            serverLevel.sendParticles(ParticleTypes.POOF,
                    mid.x, mid.y, mid.z,  6, 0.15, 0.15, 0.15, 0.02);
        }
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.75f);
    }


    private LivingEntity findTarget(Player player) {
        Vec3 eye    = player.getEyePosition();
        Vec3 look   = player.getLookAngle();
        Vec3 center = eye.add(look.scale(GRAB_RANGE * 0.5));

        AABB box = new AABB(
                center.x - GRAB_RANGE, center.y - 1.5, center.z - GRAB_RANGE,
                center.x + GRAB_RANGE, center.y + 1.5, center.z + GRAB_RANGE);

        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class, box,
                e -> e != player && e.isAlive() && !e.isSpectator());

        // Closest entity first
        LivingEntity best  = null;
        double       bestD = Double.MAX_VALUE;
        for (LivingEntity e : candidates) {
            double d = e.distanceToSqr(player);
            if (d < bestD) { bestD = d; best = e; }
        }
        return best;
    }
}
