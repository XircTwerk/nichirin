package com.xirc.nichirin.common.attack.moves.breathing.thunder;

import com.xirc.nichirin.common.effect.ShockedStatusEffect;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import com.xirc.nichirin.registry.NichirinSoundRegistry;
import com.xirc.nichirin.common.vfx.VfxIds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Godspeed — Thunder Breathing crouch-right-click.
 *
 * <p>10-tick windup (handled by the base class), then a smooth velocity dash in the
 * look direction. Any entity swept during the dash is "dragged" — it receives the same per-tick
 * velocity as the player so it travels alongside them, and is hit for {@code damage} every
 * {@link #HIT_INTERVAL_TICKS} ticks for the remainder of the active window.</p>
 */
public class GodspeedAttack extends ThunderBreathingAttackBase {

    private static final float DEFAULT_DASH_DISTANCE = 300.0f;
    private static final int DASH_TICKS = 25;
    private static final float MAX_SMOOTH_DISTANCE_PER_TICK = 3.5f;
    private static final int HIT_INTERVAL_TICKS = 5;
    private static final float AFTERIMAGE_ALPHA = 0.7f;
    private static final int AFTERIMAGE_LIFETIME_TICKS = 18;
    private static final int AFTERIMAGE_COPIES = 10;
    private static final float HIT_RADIUS = 1.6f;
    // Radius used to find dragged entities on subsequent ticks (they may drift slightly).
    private static final double DRAG_SEARCH_RADIUS = 60.0;

    private Vec3 dashDirection = Vec3.ZERO;
    private Vec3 dashOrigin = Vec3.ZERO;
    private float totalDistance = 0f;
    private float distancePerTick = 0f;
    private float traveled = 0f;
    private float maxTravel = 0f;
    // Maps dragged entity UUID → tickCount at which drag started (for hit-interval math).
    private final Map<UUID, Integer> dragStart = new HashMap<>();

    @Override
    protected void onStart() {
        if (world.isClientSide) return;
        totalDistance = teleportDistance != null ? teleportDistance : DEFAULT_DASH_DISTANCE;
        distancePerTick = Math.min(totalDistance / DASH_TICKS, MAX_SMOOTH_DISTANCE_PER_TICK);
        dragStart.clear();
    }

    @Override
    protected void onActiveStart() {
        if (world.isClientSide) return;
        // Lock the dash direction and origin at dash-start (after windup). We advance along this
        // pre-clipped path each tick instead of re-reading user.position(), whose server-side value
        // lags far behind the client during a fast velocity dash (and sags under gravity) — that lag
        // was what placed the dash/particles "under and behind" the player.
        dashDirection = user.getLookAngle().normalize();
        dashOrigin = user.position();
        Vec3 clippedEnd = clipForward(dashOrigin, dashOrigin.add(dashDirection.scale(totalDistance)));
        maxTravel = (float) clippedEnd.distanceTo(dashOrigin);
        traveled = 0f;
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                NichirinSoundRegistry.THUNDERCLAP_FLASH.get(), SoundSource.PLAYERS, 1.0f, 1.5f);
        playThunderVfx(VfxIds.GODSPEED, user.position(), dashDirection, 1.25f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        Vec3 perTickVelocity = Vec3.ZERO;

        if (traveled < maxTravel) {
            float prev = traveled;
            traveled = Math.min(maxTravel, traveled + distancePerTick);
            Vec3 from = dashOrigin.add(dashDirection.scale(prev));
            Vec3 to = dashOrigin.add(dashDirection.scale(traveled));
            perTickVelocity = to.subtract(from);

            sweepIntoDrag(from, to);
            applySmoothDashVelocity(perTickVelocity);

            NichirinPacketRegistry.sendAfterimageTrail(user, from, to,
                    AFTERIMAGE_LIFETIME_TICKS, AFTERIMAGE_COPIES, AFTERIMAGE_ALPHA);
        }

        applyDragAndHit(perTickVelocity);
    }

    private void applySmoothDashVelocity(Vec3 velocity) {
        user.setDeltaMovement(velocity);
        user.hurtMarked = true;
        user.hasImpulse = true;
        user.fallDistance = 0;
        if (user instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(user));
        }
    }

    /** Adds any entity inside the swept volume to the drag set (first contact only). */
    private void sweepIntoDrag(Vec3 from, Vec3 to) {
        AABB sweep = new AABB(from, to).inflate(HIT_RADIUS);
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, sweep,
                e -> e != user && e.isAlive() && !dragStart.containsKey(e.getUUID()));
        for (LivingEntity e : entities) {
            dragStart.put(e.getUUID(), tickCount);
        }
    }

    /**
     * Applies drag velocity (during the dash) and delivers periodic hits to all dragged entities.
     */
    private void applyDragAndHit(Vec3 dashVelocity) {
        if (dragStart.isEmpty()) return;

        AABB searchBox = new AABB(user.position(), user.position()).inflate(DRAG_SEARCH_RADIUS);
        List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> dragStart.containsKey(e.getUUID()) && e.isAlive());

        for (LivingEntity target : nearby) {
            int elapsed = tickCount - dragStart.get(target.getUUID());

            // Hit first (every HIT_INTERVAL_TICKS) so its knockback can't override the drag below.
            if (elapsed % HIT_INTERVAL_TICKS == 0) {
                hitTargetNoImmunity(target);
                ShockedStatusEffect.markRecentLaunch(target);
            }

            // Drag: carry the entity along with the dash via the same per-tick velocity (smooth),
            // applied after the hit above so the hit's knockback can't override it.
            if (!dashVelocity.equals(Vec3.ZERO)) {
                target.setDeltaMovement(dashVelocity);
                target.hurtMarked = true;
                target.hasImpulse = true;
                if (world instanceof ServerLevel sl) {
                    sl.getChunkSource().broadcast(target, new ClientboundSetEntityMotionPacket(target));
                }
                if (target instanceof ServerPlayer sp) {
                    sp.connection.send(new ClientboundSetEntityMotionPacket(target));
                }
            }
        }

        // Prune dead or out-of-range entries so the map doesn't grow unbounded.
        dragStart.keySet().removeIf(uuid -> nearby.stream().noneMatch(e -> e.getUUID().equals(uuid)));
    }

    private Vec3 clipForward(Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from);
        double total = dir.length();
        if (total < 1e-4) return to;
        Vec3 step = dir.normalize().scale(0.5);
        Vec3 cursor = from;
        Vec3 lastSafe = from;
        for (double traveled = 0; traveled < total; traveled += 0.5) {
            cursor = cursor.add(step);
            BlockPos bp = BlockPos.containing(cursor.x, cursor.y + 1.0, cursor.z);
            BlockState state = world.getBlockState(bp);
            if (!state.isAir() && state.isSolidRender(world, bp)) {
                return lastSafe;
            }
            lastSafe = cursor;
        }
        return lastSafe;
    }

    @Override
    protected void onStop() {
        if (user != null) {
            user.setDeltaMovement(Vec3.ZERO);
            user.hurtMarked = true;
        }
        dragStart.clear();
        super.onStop();
    }
}
