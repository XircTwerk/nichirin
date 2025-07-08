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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.List;
import java.util.function.Consumer;

/**
 * Utility class for handling teleportation in combat moves
 */
public class TeleportUtil {

    /**
     * Teleports an entity with effects and safety checks
     */
    public static boolean teleport(LivingEntity entity, Vec3 targetPos, TeleportOptions options) {
        Level world = entity.level();
        Vec3 startPos = entity.position();

        // Safety check - ensure target position is valid
        if (!isSafePosition(world, targetPos, entity)) {
            if (options.requireSafe) {
                return false;
            }
            // Try to find nearest safe position
            targetPos = findNearestSafePosition(world, targetPos, entity, options.maxSafeSearchRadius);
            if (targetPos == null) {
                return false;
            }
        }

        // Pre-teleport callback
        if (options.preTeleport != null) {
            options.preTeleport.accept(entity);
        }

        // Create departure effects
        if (world instanceof ServerLevel serverLevel && options.departureParticles != null) {
            createParticleTrail(serverLevel, startPos, options.departureParticles, options.departureParticleCount);
        }

        // Play departure sound
        if (options.departureSound != null) {
            world.playSound(null, startPos.x, startPos.y, startPos.z,
                    options.departureSound, SoundSource.PLAYERS, options.soundVolume, options.soundPitch);
        }

        // Perform teleportation
        entity.teleportTo(targetPos.x, targetPos.y, targetPos.z);

        // Reset fall distance if specified
        if (options.resetFallDistance) {
            entity.fallDistance = 0;
        }

        // Force velocity sync for players
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(entity));
        }

        // Create arrival effects
        if (world instanceof ServerLevel serverLevel && options.arrivalParticles != null) {
            createParticleTrail(serverLevel, targetPos, options.arrivalParticles, options.arrivalParticleCount);
        }

        // Play arrival sound
        if (options.arrivalSound != null) {
            world.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                    options.arrivalSound, SoundSource.PLAYERS, options.soundVolume, options.soundPitch);
        }

        // Create trail if specified
        if (options.createTrail && world instanceof ServerLevel serverLevel) {
            createTeleportTrail(serverLevel, startPos, targetPos, options.trailParticles, options.trailDensity);
        }

        // Post-teleport callback
        if (options.postTeleport != null) {
            options.postTeleport.accept(entity);
        }

        // Damage entities along path if specified
        if (options.damageAlongPath) {
            damageEntitiesInPath(entity, startPos, targetPos, options);
        }

        return true;
    }

    /**
     * Teleport with default options
     */
    public static boolean teleport(LivingEntity entity, Vec3 targetPos) {
        return teleport(entity, targetPos, new TeleportOptions());
    }

    /**
     * Teleport in look direction
     */
    public static boolean teleportInDirection(LivingEntity entity, float distance, TeleportOptions options) {
        Vec3 direction = entity.getLookAngle();
        Vec3 targetPos = entity.position().add(direction.scale(distance));
        return teleport(entity, targetPos, options);
    }

    /**
     * Teleport to entity
     */
    public static boolean teleportToEntity(LivingEntity teleporter, Entity target, float offset, TeleportOptions options) {
        Vec3 direction = target.position().subtract(teleporter.position()).normalize();
        Vec3 targetPos = target.position().subtract(direction.scale(offset));
        return teleport(teleporter, targetPos, options);
    }

    /**
     * Teleport behind entity
     */
    public static boolean teleportBehindEntity(LivingEntity teleporter, LivingEntity target, float distance, TeleportOptions options) {
        Vec3 targetLook = target.getLookAngle();
        Vec3 behindPos = target.position().subtract(targetLook.scale(distance));
        return teleport(teleporter, behindPos, options);
    }

    /**
     * Check if position is safe for teleportation
     */
    private static boolean isSafePosition(Level world, Vec3 pos, Entity entity) {
        BlockPos blockPos = new BlockPos((int)pos.x, (int)pos.y, (int)pos.z);
        BlockState state = world.getBlockState(blockPos);
        BlockState stateAbove = world.getBlockState(blockPos.above());

        // Check if blocks are passable
        boolean canStand = state.isCollisionShapeFullBlock(world, blockPos) ||
                !state.getCollisionShape(world, blockPos, CollisionContext.of(entity)).isEmpty();
        boolean headClear = world.getBlockState(blockPos.above()).isAir();

        return !canStand && headClear;
    }

    /**
     * Find nearest safe position for teleportation
     */
    private static Vec3 findNearestSafePosition(Level world, Vec3 pos, Entity entity, float maxRadius) {
        // Search in expanding circles
        for (float r = 0.5f; r <= maxRadius; r += 0.5f) {
            for (int angle = 0; angle < 360; angle += 30) {
                double rad = Math.toRadians(angle);
                Vec3 checkPos = pos.add(Math.cos(rad) * r, 0, Math.sin(rad) * r);

                // Check positions at different heights
                for (int y = -2; y <= 2; y++) {
                    Vec3 testPos = checkPos.add(0, y, 0);
                    if (isSafePosition(world, testPos, entity)) {
                        return testPos;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Create particle trail for teleportation
     */
    private static void createTeleportTrail(ServerLevel world, Vec3 start, Vec3 end, ParticleOptions particle, float density) {
        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        Vec3 step = direction.normalize();

        int particleCount = (int)(distance * density);
        for (int i = 0; i < particleCount; i++) {
            Vec3 pos = start.add(step.scale(i / density));
            world.sendParticles(particle, pos.x, pos.y + 1, pos.z, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Create particle burst at position
     */
    private static void createParticleTrail(ServerLevel world, Vec3 pos, ParticleOptions particle, int count) {
        world.sendParticles(particle, pos.x, pos.y + 1, pos.z, count, 0.3, 0.5, 0.3, 0.1);
    }

    /**
     * Damage entities along teleport path
     */
    private static void damageEntitiesInPath(LivingEntity attacker, Vec3 start, Vec3 end, TeleportOptions options) {
        if (!options.damageAlongPath || options.pathDamage <= 0) return;

        Vec3 direction = end.subtract(start).normalize();
        double distance = start.distanceTo(end);

        for (double d = 0; d < distance; d += 0.5) {
            Vec3 checkPos = start.add(direction.scale(d));
            AABB hitbox = new AABB(checkPos.add(-1, -1, -1), checkPos.add(1, 1, 1));

            List<LivingEntity> targets = attacker.level().getEntitiesOfClass(LivingEntity.class, hitbox,
                    entity -> entity != attacker && entity.isAlive());

            for (LivingEntity target : targets) {
                if (options.pathDamageCallback != null) {
                    options.pathDamageCallback.accept(target);
                } else {
                    target.hurt(attacker.damageSources().playerAttack((Player)attacker), options.pathDamage);
                }
            }
        }
    }

    /**
     * Configuration options for teleportation
     */
    public static class TeleportOptions {
        // Effects
        public ParticleOptions departureParticles = ParticleTypes.PORTAL;
        public ParticleOptions arrivalParticles = ParticleTypes.PORTAL;
        public ParticleOptions trailParticles = ParticleTypes.ELECTRIC_SPARK;
        public int departureParticleCount = 20;
        public int arrivalParticleCount = 20;
        public float trailDensity = 4.0f; // Particles per block
        public boolean createTrail = true;

        // Sounds
        public SoundEvent departureSound = SoundEvents.ENDERMAN_TELEPORT;
        public SoundEvent arrivalSound = null;
        public float soundVolume = 1.0f;
        public float soundPitch = 1.0f;

        // Safety
        public boolean requireSafe = true;
        public float maxSafeSearchRadius = 2.0f;
        public boolean resetFallDistance = true;

        // Combat
        public boolean damageAlongPath = false;
        public float pathDamage = 0.0f;
        public Consumer<LivingEntity> pathDamageCallback = null;

        // Callbacks
        public Consumer<LivingEntity> preTeleport = null;
        public Consumer<LivingEntity> postTeleport = null;

        // Builder methods for easy configuration
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