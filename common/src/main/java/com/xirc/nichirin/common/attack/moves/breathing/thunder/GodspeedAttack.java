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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Godspeed — Thunder Breathing crouch-right-click. High-speed dash in the look direction over
 * {@link #DASH_TICKS} ticks, covering up to {@link #DEFAULT_DASH_DISTANCE} blocks (or the configured
 * teleport distance). Damages every entity the swept path crosses; raycast-clipped against terrain.
 */
public class GodspeedAttack extends ThunderBreathingAttackBase {

    private static final float DEFAULT_DASH_DISTANCE = 100.0f;
    private static final int DASH_TICKS = 20;
    private static final float AFTERIMAGE_ALPHA = 0.7f;
    private static final int AFTERIMAGE_LIFETIME_TICKS = 18;
    private static final int AFTERIMAGE_COPIES = 10;
    private static final float HIT_RADIUS = 1.6f;

    private Vec3 dashDirection = Vec3.ZERO;
    private float remainingDistance = 0f;
    private float distancePerTick = 0f;
    private final Set<UUID> hitDuringDash = new HashSet<>();

    @Override
    protected void onStart() {
        if (world.isClientSide) return;
        dashDirection = user.getLookAngle().normalize();
        float total = teleportDistance != null ? teleportDistance : DEFAULT_DASH_DISTANCE;
        remainingDistance = total;
        distancePerTick = total / DASH_TICKS;
        hitDuringDash.clear();
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                NicirinSoundRegistry.THUNDERCLAP_FLASH.get(), SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;
        if (remainingDistance <= 0f) return;

        Vec3 from = user.position();
        float step = Math.min(distancePerTick, remainingDistance);
        Vec3 desired = from.add(dashDirection.scale(step));
        Vec3 clipped = clipForward(from, desired);

        damageAlongSweep(from, clipped);
        teleportPlayer(user, clipped);
        NichirinPacketRegistry.sendAfterimageTrail(user, from, clipped,
                AFTERIMAGE_LIFETIME_TICKS, AFTERIMAGE_COPIES, AFTERIMAGE_ALPHA);

        spawnTrailParticles(from, clipped);

        float traveled = (float) clipped.distanceTo(from);
        remainingDistance -= traveled;

        // Hit a wall — finish the dash early.
        if (traveled < step - 0.05f) {
            remainingDistance = 0f;
        }
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

    private void damageAlongSweep(Vec3 from, Vec3 to) {
        AABB sweep = new AABB(from, to).inflate(HIT_RADIUS);
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, sweep,
                e -> e != user && e.isAlive() && !hitDuringDash.contains(e.getUUID()));
        for (LivingEntity target : entities) {
            hitDuringDash.add(target.getUUID());
            hitTargetNoImmunity(target);
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 14, 1, false, false, true));
            target.setDeltaMovement(
                    target.getDeltaMovement().x * 0.1,
                    0.55,
                    target.getDeltaMovement().z * 0.1
            );
            target.hurtMarked = true;
            target.hasImpulse = true;
            ShockedStatusEffect.markRecentLaunch(target);

            if (world instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().broadcast(target, new ClientboundSetEntityMotionPacket(target));
            }
            if (target instanceof ServerPlayer sp) {
                sp.connection.send(new ClientboundSetEntityMotionPacket(target));
            }
        }
    }

    private void teleportPlayer(LivingEntity entity, Vec3 to) {
        if (entity instanceof ServerPlayer sp) {
            sp.teleportTo(to.x, to.y, to.z);
            sp.connection.teleport(to.x, to.y, to.z, entity.getYRot(), entity.getXRot());
            entity.setDeltaMovement(Vec3.ZERO);
            sp.connection.send(new ClientboundSetEntityMotionPacket(entity));
        } else {
            entity.teleportTo(to.x, to.y, to.z);
            entity.setDeltaMovement(Vec3.ZERO);
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
