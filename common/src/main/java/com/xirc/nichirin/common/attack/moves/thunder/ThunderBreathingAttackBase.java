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
 *
 * HITBOX SYSTEM: Uses AABB (Axis-Aligned Bounding Box) collision detection.
 * - No entities are created - pure math-based collision detection
 * - Creates invisible cubes in 3D space at specified positions
 * - Queries for LivingEntity objects within the cube
 * - Applies damage/effects to found entities
 * - Very lightweight with no entity spawn/despawn overhead
 */
public abstract class ThunderBreathingAttackBase {

    // Configuration from moveset - NO DEFAULT VALUES
    // These are set via configure() method called by the moveset
    protected float damage;
    protected float range;
    protected float knockback;
    protected float breathCost;
    protected int hitStun;
    protected float hitboxSize;
    protected int cooldown;
    protected int windup;
    protected int duration;

    // Movement properties (nullable - not all attacks use these)
    protected Float teleportDistance; // For teleport-based attacks like Thunderclap Flash
    protected Float dashSpeed; // For dash-based attacks like Rumble Flash
    protected Integer teleportWindup; // Special windup time for teleport attacks

    // Runtime state
    @Getter
    protected boolean isActive = false;
    protected int tickCount = 0;
    protected Player user;
    protected Level world;

    // Configuration flag to prevent double-configuration
    private boolean configured = false;

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

        // Check breath cost BEFORE marking as active
        if (breathCost > 0 && !BreathingManager.consume(user, breathCost)) {
            user.displayClientMessage(
                    Component.literal("Not enough breath!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            // DON'T set isActive = true if we don't have enough breath
            return;
        }

        // Only mark as active if we successfully consumed breath
        this.isActive = true;
        onStart();
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
        target.hurt(source, damage);

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
     * Used for attacks like ThunderClap Flash that should always hit
     */
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;

        // Reset invulnerability to allow immediate damage
        target.invulnerableTime = 0;

        // Apply damage
        DamageSource source = user.damageSources().playerAttack(user);
        target.hurt(source, damage);

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
     *
     * HITBOX SYSTEM EXPLANATION:
     * - Creates an AABB (cube) of size 'hitboxSize' centered at 'center'
     * - Uses Minecraft's built-in entity query system
     * - No entities are spawned - pure collision detection
     * - Returns list of LivingEntity objects within the cube
     * - Filters out the user and dead entities
     */
    protected List<LivingEntity> getTargetsInHitbox(Vec3 center) {
        // Create cube hitbox: center ± hitboxSize/2 in all directions
        AABB hitbox = new AABB(
                center.x - hitboxSize/2, center.y - hitboxSize/2, center.z - hitboxSize/2,
                center.x + hitboxSize/2, center.y + hitboxSize/2, center.z + hitboxSize/2
        );

        // Query for living entities within the hitbox
        return world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive());
    }

    /**
     * Get entities in a custom hitbox with specified dimensions
     * Useful for attacks that need non-cubic hitboxes
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
     * Useful for sweep attacks or projectile-like abilities
     */
    protected List<LivingEntity> getTargetsInLine(Vec3 start, Vec3 end, double thickness) {
        // Create bounding box that encompasses the entire line
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

            // Check if entity is actually close to the line
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
     * Get cooldown value for MoveExecutor
     */
    public int getCooldown() {
        return cooldown;
    }

    /**
     * Get total attack duration (windup + active duration)
     */
    public int getTotalDuration() {
        return windup + duration;
    }

    /**
     * Check if attack is currently active (for MoveExecutor reflection)
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Get current tick count
     */
    public int getTickCount() {
        return tickCount;
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

    /**
     * Check if attack was properly configured
     */
    public boolean isConfigured() {
        return configured;
    }

    // Nullable getters for movement properties
    public Float getTeleportDistance() { return teleportDistance; }
    public Float getDashSpeed() { return dashSpeed; }
    public Integer getTeleportWindup() { return teleportWindup; }

    // Helper methods to check if movement properties are configured
    public boolean hasTeleport() { return teleportDistance != null && teleportDistance > 0; }
    public boolean hasDash() { return dashSpeed != null && dashSpeed > 0; }
    public boolean hasTeleportWindup() { return teleportWindup != null; }

    // Getters for combat stats (useful for debugging)
    public float getDamage() { return damage; }
    public float getRange() { return range; }
    public float getKnockback() { return knockback; }
    public float getBreathCost() { return breathCost; }
    public int getHitStun() { return hitStun; }
    public float getHitboxSize() { return hitboxSize; }
    public int getWindup() { return windup; }
    public int getDuration() { return duration; }
    public Player getUser() { return user; }
    public Level getWorld() { return world; }
}