package com.xirc.nichirin.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class TeleportUtil {

    public static boolean teleport(LivingEntity entity, Vec3 targetPos, TeleportOptions options) {
        Level world = entity.level();

        if (world.isClientSide) {
            return false;
        }

        Vec3 startPos = entity.position();
        if (options.requireSafe && !isPositionReasonable(world, targetPos, entity)) {
            // Try to find nearby safe position with more relaxed criteria
            targetPos = findReasonablePosition(world, targetPos, entity, options.maxSafeSearchRadius);
            if (targetPos == null) {
                // Don't fail - just teleport to original position
                targetPos = entity.position().add(entity.getLookAngle().scale(1.0)); // Minimal teleport
            }
        }

        // Pre-teleport callback
        if (options.preTeleport != null) {
            options.preTeleport.accept(entity);
        }

        // Create departure effects (server-side only)
        ServerLevel serverLevel = (ServerLevel) world;
        if (options.departureParticles != null) {
            createParticleTrail(serverLevel, startPos, options.departureParticles, options.departureParticleCount);
        }

        // Play departure sound
        if (options.departureSound != null) {
            world.playSound(null, startPos.x, startPos.y, startPos.z,
                    options.departureSound, SoundSource.PLAYERS, options.soundVolume, options.soundPitch);
        }
        if (entity instanceof ServerPlayer serverPlayer) {
            // For players: Use proper teleport method with forced sync
            serverPlayer.teleportTo(targetPos.x, targetPos.y, targetPos.z);
            // Force position sync to client immediately
            serverPlayer.connection.teleport(targetPos.x, targetPos.y, targetPos.z,
                    entity.getYRot(), entity.getXRot());

            // Reset velocity and sync
            entity.setDeltaMovement(Vec3.ZERO);
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(entity));
        } else {
            // For other entities: Standard teleport
            entity.teleportTo(targetPos.x, targetPos.y, targetPos.z);
            entity.setDeltaMovement(Vec3.ZERO);
        }

        // Reset fall distance if specified
        if (options.resetFallDistance) {
            entity.fallDistance = 0;
        }

        // Create arrival effects
        if (options.arrivalParticles != null) {
            createParticleTrail(serverLevel, targetPos, options.arrivalParticles, options.arrivalParticleCount);
        }

        // Play arrival sound
        if (options.arrivalSound != null) {
            world.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                    options.arrivalSound, SoundSource.PLAYERS, options.soundVolume, options.soundPitch);
        }

        // Create trail if specified
        if (options.createTrail) {
            createTeleportTrail(serverLevel, startPos, targetPos, options.trailParticles, options.trailDensity);
        }

        // Post-teleport callback
        if (options.postTeleport != null) {
            options.postTeleport.accept(entity);
        }

        // Server-authoritative damage along path
        if (options.damageAlongPath) {
            damageEntitiesInPath(entity, startPos, targetPos, options, serverLevel);
        }

        return true;
    }

    /**
     * Lenient position checking - check for deadly situations
     */
    public static boolean isPositionReasonable(Level level, Vec3 pos, Entity entity) {
        BlockPos feetPos = BlockPos.containing(pos.x, pos.y, pos.z);
        BlockPos headPos = feetPos.above();

        boolean feetSafe = !level.getBlockState(feetPos).blocksMotion();
        boolean headSafe = !level.getBlockState(headPos).blocksMotion();

        return feetSafe && headSafe;
    }

    /**
     * Reasonable position finder
     */
    private static Vec3 findReasonablePosition(Level world, Vec3 pos, Entity entity, float maxRadius) {
        Vec3[] offsets = {
                new Vec3(1, 0, 0), new Vec3(-1, 0, 0),
                new Vec3(0, 0, 1), new Vec3(0, 0, -1),
                new Vec3(1, 0, 1), new Vec3(-1, 0, -1),
                new Vec3(1, 0, -1), new Vec3(-1, 0, 1)
        };

        for (Vec3 offset : offsets) {
            Vec3 testPos = pos.add(offset.scale(1.5));
            if (isPositionReasonable(world, testPos, entity)) {
                return testPos;
            }
        }

        Vec3 upPos = pos.add(0, 1, 0);
        if (isPositionReasonable(world, upPos, entity)) {
            return upPos;
        }

        return pos;
    }

    /**
     * Direction teleport with relaxed block collision checks.
     */
    public static boolean teleportInDirection(LivingEntity entity, float distance, TeleportOptions options) {
        Level level = entity.level();
        if (level.isClientSide) {
            return false;
        }

        Vec3 startPos = entity.position();
        Vec3 lookVec = entity.getLookAngle();

        Vec3 finalPos = checkForMajorObstacles(level, startPos, lookVec, distance, entity);

        return teleport(entity, finalPos, options);
    }

    /**
     * Backstep teleport - moves the entity backward relative to the direction
     * they're looking, with obstacle avoidance and white death-poof trail particles.
     */
    public static boolean teleportBackward(LivingEntity entity, float distance, TeleportOptions options) {
        Level level = entity.level();
        if (level.isClientSide) {
            return false;
        }

        if (!entity.onGround()) {
            return false;
        }

        Vec3 startPos = entity.position();

        float yawRad = (float) Math.toRadians(entity.getYRot());
        Vec3 backDirection = new Vec3(Math.sin(yawRad), 0.0, -Math.cos(yawRad));

        Vec3 finalPos = checkForMajorObstacles(level, startPos, backDirection, distance, entity);

        // Ensure white death-poof trail is active for backstep teleports
        if (options != null) {
            options.createTrail = true;
            options.trailParticles = ParticleTypes.POOF;
        }

        return teleport(entity, finalPos, options);
    }

    /**
     * Check path for obstacles, placing on top of blocks at destination
     */
    private static Vec3 checkForMajorObstacles(Level level, Vec3 start, Vec3 direction, double maxDistance, Entity entity) {
        double stepSize = 0.5;

        for (double distance = stepSize; distance <= maxDistance; distance += stepSize) {
            Vec3 checkPos = start.add(direction.scale(distance));

            BlockPos centerPos = new BlockPos((int)Math.floor(checkPos.x), (int)Math.floor(checkPos.y + 1), (int)Math.floor(checkPos.z));
            BlockState centerBlock = level.getBlockState(centerPos);

            if (!centerBlock.isAir() && centerBlock.isSolidRender(level, centerPos)) {
                double stopDistance = Math.max(1.0, distance - stepSize);
                Vec3 stopPos = start.add(direction.scale(stopDistance));
                return adjustToSafePosition(level, stopPos, entity);
            }
        }

        Vec3 targetPos = start.add(direction.scale(maxDistance));
        return adjustToSafePosition(level, targetPos, entity);
    }

    /**
     * Adjust position to place entity safely on top of blocks if needed
     */
    private static Vec3 adjustToSafePosition(Level level, Vec3 targetPos, Entity entity) {
        double entityHeight = entity.getBbHeight();

        BlockPos feetPos = new BlockPos((int)Math.floor(targetPos.x), (int)Math.floor(targetPos.y), (int)Math.floor(targetPos.z));
        BlockPos headPos = new BlockPos((int)Math.floor(targetPos.x), (int)Math.floor(targetPos.y + entityHeight), (int)Math.floor(targetPos.z));

        BlockState feetBlock = level.getBlockState(feetPos);
        BlockState headBlock = level.getBlockState(headPos);

        boolean feetSolid = !feetBlock.isAir() && feetBlock.isSolidRender(level, feetPos);
        boolean headSolid = !headBlock.isAir() && headBlock.isSolidRender(level, headPos);

        if (feetSolid) {
            double blockTop = feetPos.getY() + 1.0;
            targetPos = new Vec3(targetPos.x, blockTop, targetPos.z);

            headPos = new BlockPos((int)Math.floor(targetPos.x), (int)Math.floor(targetPos.y + entityHeight), (int)Math.floor(targetPos.z));
            headBlock = level.getBlockState(headPos);
            headSolid = !headBlock.isAir() && headBlock.isSolidRender(level, headPos);

            if (headSolid) {
                targetPos = new Vec3(targetPos.x, headPos.getY() + 1.0, targetPos.z);
            }
        } else if (headSolid) {
            targetPos = new Vec3(targetPos.x, headPos.getY() + 1.0, targetPos.z);
        }

        return targetPos;
    }

    public static boolean teleport(LivingEntity entity, Vec3 targetPos) {
        return teleport(entity, targetPos, new TeleportOptions());
    }

    public static boolean teleportToEntity(LivingEntity teleporter, Entity target, float offset, TeleportOptions options) {
        if (teleporter.level().isClientSide) return false;

        Vec3 direction = target.position().subtract(teleporter.position()).normalize();
        Vec3 targetPos = target.position().subtract(direction.scale(offset));
        return teleport(teleporter, targetPos, options);
    }

    public static boolean teleportBehindEntity(LivingEntity teleporter, LivingEntity target, float distance, TeleportOptions options) {
        if (teleporter.level().isClientSide) return false;

        Vec3 targetLook = target.getLookAngle();
        Vec3 behindPos = target.position().subtract(targetLook.scale(distance));
        return teleport(teleporter, behindPos, options);
    }

    private static void createTeleportTrail(ServerLevel world, Vec3 start, Vec3 end, ParticleOptions particle, float density) {
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        if (distance <= 0.001) return;

        int particleCount = Math.max((int)(distance * density), 10);
        for (int i = 0; i <= particleCount; i++) {
            double progress = (double) i / particleCount;
            Vec3 pos = start.add(direction.scale(progress));
            world.sendParticles(particle, pos.x, pos.y + 0.8, pos.z, 4, 0.15, 0.25, 0.15, 0.02);
        }
    }

    private static void createParticleTrail(ServerLevel world, Vec3 pos, ParticleOptions particle, int count) {
        world.sendParticles(particle, pos.x, pos.y + 1, pos.z, count, 0.3, 0.5, 0.3, 0.1);
    }

    private static void damageEntitiesInPath(LivingEntity attacker, Vec3 start, Vec3 end, TeleportOptions options, ServerLevel world) {
        if (!options.damageAlongPath || options.pathDamage <= 0) return;

        Vec3 direction = end.subtract(start).normalize();
        double distance = start.distanceTo(end);
        Set<LivingEntity> hitEntities = new HashSet<>();

        for (double d = 0; d < distance; d += 0.3) {
            Vec3 checkPos = start.add(direction.scale(d));
            AABB hitbox = new AABB(checkPos.add(-1.5, -1, -1.5), checkPos.add(1.5, 2, 1.5));

            List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                    entity -> entity != attacker && entity.isAlive() && !hitEntities.contains(entity));

            for (LivingEntity target : targets) {
                hitEntities.add(target);

                if (options.pathDamageCallback != null) {
                    options.pathDamageCallback.accept(target);
                } else {
                    NichirinDamageHandler.hurt(
                            target, NichirinDamageSources.breathing(attacker), options.pathDamage);
                }
            }
        }
    }

    public static class TeleportOptions {
        // Effects
        public ParticleOptions departureParticles = ParticleTypes.PORTAL;
        public ParticleOptions arrivalParticles = ParticleTypes.PORTAL;
        public ParticleOptions trailParticles = ParticleTypes.ELECTRIC_SPARK;
        public int departureParticleCount = 20;
        public int arrivalParticleCount = 20;
        public float trailDensity = 4.0f;
        public boolean createTrail = true;

        // Sounds
        public SoundEvent departureSound = SoundEvents.ENDERMAN_TELEPORT;
        public SoundEvent arrivalSound = null;
        public float soundVolume = 1.0f;
        public float soundPitch = 1.0f;

        // Safety
        public boolean requireSafe = true;
        public float maxSafeSearchRadius = 3.0f;
        public boolean resetFallDistance = true;

        // Combat
        public boolean damageAlongPath = false;
        public float pathDamage = 0.0f;
        public Consumer<LivingEntity> pathDamageCallback = null;

        // Callbacks
        public Consumer<LivingEntity> preTeleport = null;
        public Consumer<LivingEntity> postTeleport = null;

        // Builder methods
        public TeleportOptions withParticles(ParticleOptions departure, ParticleOptions arrival) {
            this.departureParticles = departure;
            this.arrivalParticles = arrival;
            return this;
        }

        public TeleportOptions withTrail(ParticleOptions trail, float density) {
            this.trailParticles = trail;
            this.trailDensity = density;
            this.createTrail = true;
            return this;
        }

        public TeleportOptions withSounds(SoundEvent departure, SoundEvent arrival) {
            this.departureSound = departure;
            this.arrivalSound = arrival;
            return this;
        }

        public TeleportOptions withDamage(float damage) {
            this.damageAlongPath = true;
            this.pathDamage = damage;
            return this;
        }

        public TeleportOptions withDamageCallback(Consumer<LivingEntity> callback) {
            this.damageAlongPath = true;
            this.pathDamageCallback = callback;
            return this;
        }

        public TeleportOptions noTrail() {
            this.createTrail = false;
            return this;
        }

        public TeleportOptions unsafe() {
            this.requireSafe = false;
            return this;
        }
    }
}