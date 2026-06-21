package com.xirc.nichirin.common.system.movement;

import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.config.NichirinModConfig.MovementConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Velocity-only movement primitives for any LivingEntity.
 * No stamina consumed here — callers are responsible for resource checks.
 * Player-specific wrappers in Dash/Backstep/Dodge/PlayerDoubleJump delegate here.
 */
public final class EntityMovement {

    // Movement tuning is read from config (movement.* in nichirin-server.toml).
    private static MovementConfig mcfg() {
        return NichirinModConfig.get().movement;
    }
    private static double dashForce()       { return mcfg().dashForce; }
    private static double doubleJumpVel()    { return mcfg().doubleJumpVelocity; }
    private static double backstepDist()     { return mcfg().backstepDistance; }
    private static double airDodgeDist()     { return mcfg().airDodgeDistance; }
    private static double sprintMultiplier() { return mcfg().sprintMultiplier; }

    private EntityMovement() {}


    /**
     * Applies a ground dash impulse in the given direction.
     * Pass a normalized horizontal Vec3; zero falls back to the entity's look direction.
     */
    public static void applyDash(LivingEntity entity, Vec3 direction) {
        Vec3 dir = direction.lengthSqr() > 0.001
                ? new Vec3(direction.x, 0, direction.z).normalize()
                : new Vec3(entity.getLookAngle().x, 0, entity.getLookAngle().z).normalize();

        Vec3 current = entity.getDeltaMovement();
        entity.setDeltaMovement(
                current.x + dir.x * dashForce(),
                Math.max(current.y, 0.1),
                current.z + dir.z * dashForce()
        );
        entity.hasImpulse = true;

        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5f, 1.3f);

        spawnTrailParticles(entity);
    }


    /**
     * Teleports the entity backward up to backstepDist() blocks, stopping at obstacles.
     */
    public static void applyBackstep(LivingEntity entity) {
        Vec3 back = entity.getLookAngle().scale(-1);
        Vec3 horiz = new Vec3(back.x, 0, back.z).normalize();

        Vec3 dest = findSafePosition(entity, horiz, backstepDist());
        if (dest == null) return;

        entity.teleportTo(dest.x, dest.y, dest.z);

        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.2f);

        if (entity.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF,
                    entity.getX(), entity.getY() + 1, entity.getZ(),
                    12, 0.3, 0.5, 0.3, 0.05);
        }
    }


    /**
     * Applies a short directional burst while airborne.
     * Pass a normalized Vec3; zero uses the entity's current look direction.
     */
    public static void applyAirDodge(LivingEntity entity, Vec3 direction) {
        Vec3 dir = direction.lengthSqr() > 0.001
                ? direction.normalize()
                : entity.getLookAngle().normalize();

        Vec3 current = entity.getDeltaMovement();
        entity.setDeltaMovement(
                current.x + dir.x * airDodgeDist(),
                current.y + 0.15,
                current.z + dir.z * airDodgeDist()
        );
        entity.hasImpulse = true;

        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4f, 1.8f);
    }


    /**
     * Applies an upward impulse with optional horizontal direction.
     * Caller must have already checked canDoubleJump() and called markDoubleJumped().
     */
    public static void applyDoubleJump(LivingEntity entity, Vec3 horizontalDirection) {
        if (entity.onGround()) return;

        Vec3 horiz = horizontalDirection.lengthSqr() > 0.001
                ? new Vec3(horizontalDirection.x, 0, horizontalDirection.z).normalize().scale(0.4)
                : new Vec3(entity.getLookAngle().x, 0, entity.getLookAngle().z).normalize().scale(0.3);

        Vec3 current = entity.getDeltaMovement();
        entity.setDeltaMovement(
                current.x + horiz.x,
                doubleJumpVel(),
                current.z + horiz.z
        );
        entity.hasImpulse = true;

        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.5f, 1.8f);

        if (entity.level() instanceof ServerLevel sl) {
            for (int i = 0; i < 24; i++) {
                double angle = (Math.PI * 2 * i) / 24;
                sl.sendParticles(ParticleTypes.CLOUD,
                        entity.getX() + Math.cos(angle),
                        entity.getY(),
                        entity.getZ() + Math.sin(angle),
                        1, Math.cos(angle) * 0.3, 0.15, Math.sin(angle) * 0.3, 0.05);
            }
        }
    }


    /**
     * Applies a temporary sprint attribute boost for the given duration in ticks.
     * The caller's tick() should call clearSprint() once combat ends or a timer expires.
     */
    public static void applySprint(LivingEntity entity) {
        entity.setSprinting(true);
        var speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;

        var existing = speedAttr.getModifier(SPRINT_ID);
        if (existing == null) {
            speedAttr.addTransientModifier(new AttributeModifier(
                    SPRINT_ID, sprintMultiplier() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));
        }
    }

    public static void clearSprint(LivingEntity entity) {
        entity.setSprinting(false);
        var speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.removeModifier(SPRINT_ID);
    }

    private static final ResourceLocation SPRINT_ID =
            ResourceLocation.fromNamespaceAndPath("nichirin", "npc_sprint");


    private static Vec3 findSafePosition(LivingEntity entity, Vec3 direction, double maxDist) {
        Vec3 start = entity.position();
        int steps = (int)(maxDist / 0.5);

        for (int i = steps; i >= 1; i--) {
            Vec3 candidate = start.add(direction.scale(i * 0.5));
            if (entity.level().noCollision(entity, entity.getBoundingBox().move(candidate.subtract(start)))) {
                return candidate;
            }
        }
        return null;
    }

    private static void spawnTrailParticles(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.CLOUD,
                entity.getX(), entity.getY() + 1, entity.getZ(),
                8, 0.2, 0.3, 0.2, 0.02);
    }
}
