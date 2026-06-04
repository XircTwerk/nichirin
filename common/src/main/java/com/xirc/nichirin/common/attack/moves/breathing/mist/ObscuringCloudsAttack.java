package com.xirc.nichirin.common.attack.moves.breathing.mist;

import com.xirc.nichirin.common.entity.effect.PlayerCloneEntity;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Form 7: Obscuring Clouds.
 * Locks onto nearest enemy (stops if none). Becomes invisible and orbits the
 * target continuously, slashing each pass. Six clones mimic the orbit visually.
 */
public class ObscuringCloudsAttack extends MistBreathingAttackBase {

    private static final double ORBIT_SPEED  = 0.07;  // radians/tick  (~4° — one orbit ≈ 90 ticks)
    private static final double ORBIT_RADIUS = 3.0;
    private static final int    HIT_INTERVAL = 12;    // ticks between hits on same target
    private static final int    CLONE_INTERVAL = 6;
    private static final int    MAX_CLONES   = 6;     // six clones orbit alongside the user

    private LivingEntity orbitTarget      = null;
    private double       orbitAngle       = 0.0;
    private boolean      orbitInitialized = false;
    private int          nextCloneSpawnTick = 0;
    private Vec3         initPos          = null;

    private final Map<UUID, Integer> lastHitTicks  = new HashMap<>();
    private final List<UUID>         spawnedClones = new ArrayList<>();

    // -------------------------------------------------------------------------

    @Override
    protected void onStart() {
        // Rice Spirit pattern: find target first, bail if none
        orbitTarget = findClosestEnemy();
        if (orbitTarget == null) {
            stop();
            return;
        }

        initPos          = user.position();
        orbitAngle         = 0.0;
        orbitInitialized   = false;
        nextCloneSpawnTick = windup; // first clone spawns on the first active tick
        lastHitTicks.clear();
        spawnedClones.clear();

        user.addEffect(new MobEffectInstance(
                MobEffects.INVISIBILITY, windup + duration + 10, 0, false, false, false));

        if (world instanceof ServerLevel serverLevel) {
            Vec3 pos = user.position().add(0, user.getBbHeight() / 2, 0);
            serverLevel.sendParticles(ParticleTypes.CLOUD,    pos.x, pos.y, pos.z, 40, 1.2, 1.0, 1.2, 0.04);
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH, pos.x, pos.y, pos.z, 30, 1.0, 0.8, 1.0, 0.03);
        }
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Stop if target died or wandered out of twice the configured range
        if (orbitTarget == null || !orbitTarget.isAlive()
                || orbitTarget.distanceToSqr(user) > (range * 2) * (range * 2)) {
            stop();
            return;
        }

        int ticksSinceWindup = tickCount - windup;
        if (ticksSinceWindup < 0) return;
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Center tracks the target's feet so we stay grounded
        Vec3 center = orbitTarget.position();

        // Stagger clone spawning, but only up to MAX_CLONES total — previously this spawned a
        // new clone every CLONE_INTERVAL ticks for the whole (210-tick) duration, flooding the
        // area with dozens of clones.
        if (spawnedClones.size() < MAX_CLONES && tickCount >= nextCloneSpawnTick) {
            spawnNextClone(center);
            nextCloneSpawnTick = tickCount + CLONE_INTERVAL;
        }

        // Set initial orbit angle from where we currently stand so there's no jump
        if (!orbitInitialized) {
            Vec3 toUser = user.position().subtract(center);
            orbitAngle       = Math.atan2(toUser.z, toUser.x);
            orbitInitialized = true;
        }

        // Advance orbit linearly every tick
        orbitAngle += ORBIT_SPEED;

        double newX = center.x + Math.cos(orbitAngle) * ORBIT_RADIUS;
        double newZ = center.z + Math.sin(orbitAngle) * ORBIT_RADIUS;
        double y    = initPos.y;

        // Face inward toward orbit center
        double dx    = center.x - newX;
        double dz    = center.z - newZ;
        float newYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        user.setYRot(newYaw);

        user.setDeltaMovement(Vec3.ZERO);
        teleportSafe(new Vec3(newX, y, newZ));

        // Hit nearby targets every HIT_INTERVAL ticks
        if (ticksSinceWindup % 3 == 0) {
            float hitbox = hitboxSize * 2.5f;
            List<LivingEntity> targets = getTargetsInCustomHitbox(
                    center.add(0, user.getBbHeight() / 2, 0), hitbox, hitbox + 1.0f, hitbox);
            for (LivingEntity target : targets) {
                int last = lastHitTicks.getOrDefault(target.getUUID(), -HIT_INTERVAL);
                if (tickCount - last >= HIT_INTERVAL) {
                    hitTargetNoImmunity(target);
                    lastHitTicks.put(target.getUUID(), tickCount);
                }
            }
        }

        // Mist trail
        Vec3 trail = new Vec3(newX, y + user.getBbHeight() / 2, newZ);
        serverLevel.sendParticles(ParticleTypes.CLOUD,
                trail.x, trail.y, trail.z, 2, 0.12, 0.12, 0.12, 0.03);
        if (ticksSinceWindup % 3 == 0) {
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                    trail.x, trail.y, trail.z, 1, 0.08, 0.08, 0.08, 0.02);
        }
    }

    @Override
    protected void onStop() {
        user.setDeltaMovement(Vec3.ZERO);
        user.removeEffect(MobEffects.INVISIBILITY);

        if (world instanceof ServerLevel serverLevel) {
            Vec3 pos = user.position().add(0, user.getBbHeight() / 2, 0);
            serverLevel.sendParticles(ParticleTypes.CLOUD,         pos.x, pos.y, pos.z, 50, 1.5, 1.2, 1.5, 0.06);
            serverLevel.sendParticles(ParticleTypes.FALLING_WATER, pos.x, pos.y, pos.z, 30, 1.0, 0.8, 1.0, 0.05);
        }
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.7f);

        discardClones();
        lastHitTicks.clear();
        orbitTarget = null;
    }

    private void discardClones() {
        if (!(world instanceof ServerLevel serverLevel)) return;
        for (UUID id : spawnedClones) {
            net.minecraft.world.entity.Entity e = serverLevel.getEntity(id);
            if (e != null) e.discard();
        }
        spawnedClones.clear();
    }

    // -------------------------------------------------------------------------

    private void spawnNextClone(Vec3 center) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        int remainingLife = (windup + duration + 15) - tickCount;
        if (remainingLife <= 0) return;

        // Random angle so each clone swoops in from a different direction
        float angle = (float) (world.random.nextFloat() * 2.0 * Math.PI);
        Vec3 spawnCenter = new Vec3(center.x, user.getY(), center.z);

        PlayerCloneEntity clone = PlayerCloneEntity.create(
                NichirinEntityRegistry.PLAYER_CLONE.get(), world,
                user, spawnCenter, angle, 0, remainingLife);
        world.addFreshEntity(clone);
        clone.copyEquipmentFrom(user);
        spawnedClones.add(clone.getUUID());

        // Small mist burst at spawn point of this individual clone
        Vec3 spawnPos = spawnCenter.add(
                Math.cos(angle) * 8, user.getBbHeight() / 2, Math.sin(angle) * 8);
        serverLevel.sendParticles(ParticleTypes.CLOUD,
                spawnPos.x, spawnPos.y, spawnPos.z, 10, 0.3, 0.3, 0.3, 0.03);
    }

    private LivingEntity findClosestEnemy() {
        Vec3 pos = user.position();
        AABB box = new AABB(pos.subtract(range, 3, range), pos.add(range, 3, range));
        List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != user && e.isAlive() && !e.isSpectator());
        if (nearby.isEmpty()) return null;

        world.playSound(null, user, SoundEvents.NOTE_BLOCK_BELL.value(),
                SoundSource.PLAYERS, 0.8f, 1.5f);
        return nearby.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(user)))
                .orElse(null);
    }
}