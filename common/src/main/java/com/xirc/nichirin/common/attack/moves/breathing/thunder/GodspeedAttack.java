package com.xirc.nichirin.common.attack.moves.breathing.thunder;

import com.xirc.nichirin.common.effect.ShockedStatusEffect;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import com.xirc.nichirin.registry.NicirinSoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * <p>10-tick windup (handled by the base class), then a 7-tick 300-block velocity dash in the
 * look direction. Any entity swept during the dash is "dragged" — it receives the same per-tick
 * velocity as the player so it travels alongside them, and is hit for {@code damage} every
 * {@link #HIT_INTERVAL_TICKS} ticks for the remainder of the active window.</p>
 */
public class GodspeedAttack extends ThunderBreathingAttackBase {

    private static final float DEFAULT_DASH_DISTANCE = 300.0f;
    private static final int DASH_TICKS = 7;
    private static final int HIT_INTERVAL_TICKS = 5;
    private static final float AFTERIMAGE_ALPHA = 0.7f;
    private static final int AFTERIMAGE_LIFETIME_TICKS = 18;
    private static final int AFTERIMAGE_COPIES = 10;
    private static final float HIT_RADIUS = 1.6f;
    // Radius used to find dragged entities on subsequent ticks (they may drift slightly).
    private static final double DRAG_SEARCH_RADIUS = 60.0;

    private Vec3 dashDirection = Vec3.ZERO;
    private float remainingDistance = 0f;
    private float distancePerTick = 0f;
    // Maps dragged entity UUID → tickCount at which drag started (for hit-interval math).
    private final Map<UUID, Integer> dragStart = new HashMap<>();

    @Override
    protected void onStart() {
        if (world.isClientSide) return;
        dashDirection = user.getLookAngle().normalize();
        float total = teleportDistance != null ? teleportDistance : DEFAULT_DASH_DISTANCE;
        remainingDistance = total;
        distancePerTick = total / DASH_TICKS;
        dragStart.clear();
    }

    @Override
    protected void onActiveStart() {
        if (world.isClientSide) return;
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                NicirinSoundRegistry.THUNDERCLAP_FLASH.get(), SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        Vec3 perTickVelocity = Vec3.ZERO;

        if (remainingDistance > 0f) {
            Vec3 from = user.position();
            float step = Math.min(distancePerTick, remainingDistance);
            Vec3 desired = from.add(dashDirection.scale(step));
            Vec3 clipped = clipForward(from, desired);

            sweepIntoDrag(from, clipped);
            perTickVelocity = clipped.subtract(from);
            teleportPlayer(user, clipped);
            NichirinPacketRegistry.sendAfterimageTrail(user, from, clipped,
                    AFTERIMAGE_LIFETIME_TICKS, AFTERIMAGE_COPIES, AFTERIMAGE_ALPHA);
            spawnTrailParticles(from, clipped);

            float traveled = (float) clipped.distanceTo(from);
            remainingDistance -= traveled;
            if (traveled < step - 0.05f) {
                remainingDistance = 0f;
            }
        }

        applyDragAndHit(perTickVelocity);
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
            int startTick = dragStart.get(target.getUUID());
            int elapsed = tickCount - startTick;

            // Drag: pull the entity along with the dash.
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

            // Hit every HIT_INTERVAL_TICKS ticks (elapsed 0, 5, 10, …).
            if (elapsed % HIT_INTERVAL_TICKS == 0) {
                hitTargetNoImmunity(target);
                target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, hitStun, 1, false, false, true));
                ShockedStatusEffect.markRecentLaunch(target);
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

    private void teleportPlayer(LivingEntity entity, Vec3 to) {
        Vec3 delta = to.subtract(entity.position());
        entity.setDeltaMovement(delta);
        entity.hurtMarked = true;
        entity.hasImpulse = true;
        if (entity instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(entity));
        }
        entity.fallDistance = 0;
    }

    private void spawnTrailParticles(Vec3 from, Vec3 to) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        Vec3 mid = from.add(to).scale(0.5);
        serverLevel.sendParticles(NichirinParticleRegistry.THUNDER.get(),
                mid.x, mid.y + 0.9, mid.z, 6, 0.2, 0.2, 0.2, 0.05);
    }
}
