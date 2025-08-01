package com.xirc.nichirin.common.attack.moves.thunder;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset.MoveConfiguration;
import com.xirc.nichirin.common.util.BreathingManager;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Base class for Thunder Breathing attacks
 * Now purely behavioral - all configuration comes from moveset
 *
 * IMPORTANT: All visual effects (particles, sounds) and audio should be handled
 * in the individual attack classes, NOT in the builder configuration.
 * The builder only handles combat stats, timing, and resources.
 */
public abstract class ThunderBreathingAttackBase {

    // Configuration from moveset - NO DEFAULT VALUES
    // These are set via configure() method called by the moveset
    @Getter
    protected float damage;
    @Getter
    protected float range;
    @Getter
    protected float knockback;
    @Getter
    protected float breathCost;
    @Getter
    protected int hitStun;
    @Getter
    protected float hitboxSize;
    @Getter
    protected int cooldown;
    @Getter
    protected int windup;
    @Getter
    protected int duration;

    // Movement properties (nullable - not all attacks use these)
    @Getter
    protected Float teleportDistance; // For teleport-based attacks like Thunderclap Flash
    @Getter
    protected Float dashSpeed; // For dash-based attacks like Rumble Flash
    @Getter
    protected Integer teleportWindup; // Special windup time for teleport attacks

    // Runtime state
    @Getter
    protected boolean isActive = false;
    @Getter
    protected int tickCount = 0;
    @Getter
    protected Player user;
    @Getter
    protected Level world;

    // Configuration and breath consumption tracking
    @Getter
    private boolean configured = false;
    private boolean breathConsumed = false; // Track if breath was actually consumed

    /**
     * Configure this attack with values from the moveset
     * This MUST be called by the moveset before starting the attack
     */
    public void configure(MoveConfiguration config) {
        if (configured) {
            return; // Prevent double-configuration
        }

        // Combat Stats - use sensible defaults if not configured
        this.damage = config.getDamageOrDefault(10.0f);
        this.range = config.getRangeOrDefault(5.0f);
        this.knockback = config.getKnockbackOrDefault(0.3f);
        this.hitStun = config.getHitStunOrDefault(20);
        this.hitboxSize = config.getHitboxSizeOrDefault(2.0f);

        // Timing
        this.cooldown = config.getCooldownOrDefault(40);
        this.windup = config.getWindupOrDefault(5);
        this.duration = config.getDurationOrDefault(20);

        // Resources
        this.breathCost = config.getBreathCostOrDefault(15.0f);

        // Movement (nullable - only set if configured in moveset)
        this.teleportDistance = config.getTeleportDistance();
        this.dashSpeed = config.getDashSpeed();
        this.teleportWindup = config.getTeleportWindup();

        this.configured = true;
    }

    /**
     * Start the attack
     * Called by MoveExecutor after configuration is complete
     */
    public void start(Player user, Level world) {
        if (!configured) {
            System.err.println("Warning: " + this.getClass().getSimpleName() + " started without configuration!");
            // Set emergency defaults to prevent crashes
            setEmergencyDefaults();
        }

        this.user = user;
        this.world = world;
        this.tickCount = 0;
        this.breathConsumed = false;

        // Check breath cost BEFORE marking as active
        if (breathCost > 0 && !BreathingManager.hasBreath(user, breathCost)) {
            user.displayClientMessage(
                    Component.literal("Not enough breath!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            // DON'T set isActive = true if we don't have enough breath
            return;
        }

        // Consume breath BEFORE calling onStart()
        if (breathCost > 0) {
            if (BreathingManager.consume(user, breathCost)) {
                breathConsumed = true;
            } else {
                user.displayClientMessage(
                        Component.literal("Failed to consume breath!")
                                .withStyle(style -> style.withColor(0xFF5555)),
                        true
                );
                return;
            }
        }

        // Mark as active AFTER breath consumption
        this.isActive = true;

        // Call onStart() - if this calls stop(), the attack will be cancelled
        onStart();

        // If onStart() called stop(), we need to refund the breath
        if (!isActive && breathConsumed) {
            // Refund the breath since the attack was cancelled
            BreathingManager.restore(user, breathCost);
            breathConsumed = false;
        }
    }

    /**
     * Tick the attack - called every game tick while active
     */
    public void tick() {
        if (!isActive || user == null || world == null) {
            return;
        }

        tickCount++;

        // Check if we're past windup phase
        if (tickCount > windup) {
            perform();
        }

        // Check if attack duration is complete
        if (tickCount >= windup + duration) {
            stop();
        }
    }

    /**
     * Stop the attack
     */
    public void stop() {
        if (isActive) {
            isActive = false;
            onStop();
        }
    }

    /**
     * Apply damage and effects to a target
     * Standard hit method with immunity frames
     */
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;

        // Apply damage using configured values
        DamageSource source = user.damageSources().playerAttack(user);
        boolean damaged = target.hurt(source, damage);

        // Apply shocked effect if hitStun is configured
        if (hitStun > 0) {
            target.addEffect(new MobEffectInstance(
                    NichirinEffectRegistry.SHOCKED.get(),
                    hitStun,
                    0, // Amplifier 0 (effect only has 1 level)
                    false, // Ambient
                    true   // Show particles
            ));
        }

        // Apply knockback if configured
        if (knockback > 0) {
            Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
            target.push(knockbackDir.x * knockback, 0.1, knockbackDir.z * knockback);
        }
    }

    /**
     * Special hit method that removes immunity frames
     * Used for attacks like Rice Spirit that should always hit with rapid strikes
     */
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;

        // FIXED: Reset invulnerability to allow immediate damage
        target.invulnerableTime = 0;
        target.hurtTime = 0; // Also reset hurt animation timer

        // Apply damage
        DamageSource source = user.damageSources().playerAttack(user);
        boolean damaged = target.hurt(source, damage);


        // Apply shocked effect
        if (hitStun > 0) {
            target.addEffect(new MobEffectInstance(
                    NichirinEffectRegistry.SHOCKED.get(),
                    hitStun,
                    0,
                    false,
                    true
            ));
        }

        // Apply knockback
        if (knockback > 0) {
            Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
            target.push(knockbackDir.x * knockback, 0.1, knockbackDir.z * knockback);
        }
    }

    /**
     * Get entities in a hitbox centered at the given position
     */
    protected List<LivingEntity> getTargetsInHitbox(Vec3 center) {
        AABB hitbox = new AABB(
                center.x - hitboxSize/2, center.y - hitboxSize/2, center.z - hitboxSize/2,
                center.x + hitboxSize/2, center.y + hitboxSize/2, center.z + hitboxSize/2
        );

        return world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive());
    }

    /**
     * Get entities in a custom hitbox with specified dimensions
     */
    protected List<LivingEntity> getTargetsInCustomHitbox(Vec3 center, double width, double height, double depth) {
        AABB hitbox = new AABB(
                center.x - width/2, center.y - height/2, center.z - depth/2,
                center.x + width/2, center.y + height/2, center.z + depth/2
        );

        return world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive());
    }

    /**
     * Get entities in a line between two points
     */
    protected List<LivingEntity> getTargetsInLine(Vec3 start, Vec3 end, double thickness) {
        AABB lineBounds = new AABB(
                Math.min(start.x, end.x) - thickness,
                Math.min(start.y, end.y) - thickness,
                Math.min(start.z, end.z) - thickness,
                Math.max(start.x, end.x) + thickness,
                Math.max(start.y, end.y) + thickness,
                Math.max(start.z, end.z) + thickness
        );

        return world.getEntitiesOfClass(LivingEntity.class, lineBounds, entity -> {
            if (entity == user || !entity.isAlive()) {
                return false;
            }

            Vec3 entityPos = entity.position().add(0, entity.getBbHeight()/2, 0);
            double distanceToLine = distancePointToLine(entityPos, start, end);
            return distanceToLine <= thickness;
        });
    }

    /**
     * Calculate distance from a point to a line segment
     */
    private double distancePointToLine(Vec3 point, Vec3 lineStart, Vec3 lineEnd) {
        Vec3 lineVec = lineEnd.subtract(lineStart);
        Vec3 pointVec = point.subtract(lineStart);

        double lineLength = lineVec.length();
        if (lineLength == 0) {
            return point.distanceTo(lineStart);
        }

        double projection = pointVec.dot(lineVec) / (lineLength * lineLength);
        projection = Math.max(0, Math.min(1, projection));

        Vec3 closestPoint = lineStart.add(lineVec.scale(projection));
        return point.distanceTo(closestPoint);
    }

    /**
     * Set emergency default values if attack wasn't properly configured
     */
    private void setEmergencyDefaults() {
        this.damage = 10.0f;
        this.range = 5.0f;
        this.knockback = 0.3f;
        this.breathCost = 15.0f;
        this.hitStun = 20;
        this.hitboxSize = 2.0f;
        this.cooldown = 40;
        this.windup = 5;
        this.duration = 20;
        this.configured = true;
    }

    // Abstract methods that must be implemented by subclasses

    /**
     * Called when attack starts (after breath consumption)
     * Implement visual/audio startup effects here
     * If you call stop() in this method, breath will be refunded
     */
    protected abstract void onStart();

    /**
     * Called every tick during the attack (after windup period)
     * Implement the main attack logic here
     */
    protected abstract void perform();

    /**
     * Called when attack ends
     * Implement cleanup logic here
     */
    protected void onStop() {
        // Override if needed - default implementation does nothing
    }

    // Getters for configured values and MoveExecutor compatibility

    /**
     * Get total attack duration (windup + active duration)
     */
    public int getTotalDuration() {
        return windup + duration;
    }

    /**
     * Check if attack is in windup phase
     */
    public boolean isInWindup() {
        return isActive && tickCount <= windup;
    }

    /**
     * Check if attack is in active/perform phase
     */
    public boolean isInActivePhase() {
        return isActive && tickCount > windup && tickCount < windup + duration;
    }

    // Helper methods to check if movement properties are configured
    public boolean hasTeleport() { return teleportDistance != null && teleportDistance > 0; }
    public boolean hasDash() { return dashSpeed != null && dashSpeed > 0; }
    public boolean hasTeleportWindup() { return teleportWindup != null; }

    /**
     * Check if breath was consumed (for debugging)
     */
    public boolean wasBreathConsumed() {
        return breathConsumed;
    }
}