package com.xirc.nichirin.common.attack.component;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset.MoveConfiguration;
import com.xirc.nichirin.common.util.ComboIntegration;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Base class for all attack types - breathing techniques, katana attacks, martial arts, etc.
 * Provides core functionality without being tied to any specific resource system
 */
@Getter
public abstract class AbstractAttack<T extends AbstractAttack<T>> {

    // Configuration from moveset or manual setup
    protected float damage = 10.0f;
    protected float range = 3.0f;
    protected float knockback = 0.3f;
    protected int hitStun = 20;
    protected float hitboxSize = 2.0f;
    protected int cooldown = 40;
    protected int windup = 5;
    protected int duration = 20;

    // Movement properties (nullable - not all attacks use these)
    protected Float teleportDistance;
    protected Float dashSpeed;
    protected Integer teleportWindup;

    // Runtime state
    protected boolean isActive = false;
    protected int tickCount = 0;
    protected Player user;
    protected Level world;

    // Hit tracking
    @Setter
    private Set<UUID> hitEntities = new HashSet<>();
    @Setter
    private int hitCount = 0;

    // Configuration tracking
    private boolean configured = false;

    /**
     * Configure this attack with values from a moveset
     */
    public void configure(MoveConfiguration config) {
        if (configured) {
            return; // Prevent double-configuration
        }

        // Combat Stats
        this.damage = config.getDamageOrDefault(10.0f);
        this.range = config.getRangeOrDefault(3.0f);
        this.knockback = config.getKnockbackOrDefault(0.3f);
        this.hitStun = config.getHitStunOrDefault(20);
        this.hitboxSize = config.getHitboxSizeOrDefault(2.0f);

        // Timing
        this.cooldown = config.getCooldownOrDefault(40);
        this.windup = config.getWindupOrDefault(5);
        this.duration = config.getDurationOrDefault(20);

        // Movement (nullable)
        this.teleportDistance = config.getTeleportDistance();
        this.dashSpeed = config.getDashSpeed();
        this.teleportWindup = config.getTeleportWindup();

        this.configured = true;
    }

    /**
     * Builder-style configuration methods
     */
    @SuppressWarnings("unchecked")
    public T withDamage(float damage) {
        if (!configured) {
            this.damage = damage;
        }
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withRange(float range) {
        if (!configured) {
            this.range = range;
        }
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withKnockback(float knockback) {
        if (!configured) {
            this.knockback = knockback;
        }
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withHitStun(int hitStun) {
        if (!configured) {
            this.hitStun = hitStun;
        }
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withHitboxSize(float hitboxSize) {
        if (!configured) {
            this.hitboxSize = hitboxSize;
        }
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withTiming(int cooldown, int windup, int duration) {
        if (!configured) {
            this.cooldown = cooldown;
            this.windup = windup;
            this.duration = duration;
        }
        return (T) this;
    }

    /**
     * Start the attack - base implementation
     */
    public void start(Player user, Level world) {
        this.user = user;
        this.world = world;
        this.tickCount = 0;
        this.hitEntities.clear();
        this.hitCount = 0;

        // Check if attack can start (subclasses can override for resource checks)
        if (!canStart()) {
            return;
        }

        // Consume resources (subclasses implement)
        if (!consumeResources()) {
            return;
        }

        // Mark as active
        this.isActive = true;

        // Call startup logic
        onStart();

        // If onStart() called stop(), refund resources
        if (!isActive) {
            refundResources();
        }
    }

    /**
     * Legacy start method
     */
    public void start(Player player) {
        start(player, player.level());
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
     * Apply damage and effects to a target with immunity frames
     */
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;

        // Check if already hit (for non-multi-hit attacks)
        if (hitEntities.contains(target.getUUID())) {
            return;
        }

        // Apply damage
        DamageSource source = user.damageSources().playerAttack(user);
        boolean damaged = target.hurt(source, damage);

        if (damaged) {
            // Handle combo integration - this is where the magic happens
            handleSuccessfulHit(target);

            // Apply knockback if configured
            if (knockback > 0) {
                Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
                target.push(knockbackDir.x * knockback, 0.1, knockbackDir.z * knockback);
            }

            // Track hit
            hitEntities.add(target.getUUID());
            hitCount++;
        }
    }

    /**
     * Special hit method that removes immunity frames
     */
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;

        // Reset invulnerability to allow immediate damage
        target.invulnerableTime = 0;
        target.hurtTime = 0;

        // Apply damage
        DamageSource source = user.damageSources().playerAttack(user);
        boolean damaged = target.hurt(source, damage);

        if (damaged) {
            // Handle combo integration
            handleSuccessfulHit(target);

            // Apply knockback
            if (knockback > 0) {
                Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
                target.push(knockbackDir.x * knockback, 0.1, knockbackDir.z * knockback);
            }

            // Track hit (allow multiple hits for no-immunity attacks)
            hitCount++;
        }
    }

    /**
     * Handle successful hit - calls combo integration and damage tracking
     * Subclasses can override for special behavior
     */
    protected void handleSuccessfulHit(LivingEntity target) {
        int stunTicks = hitStun > 0 ? hitStun : 20;
        ComboIntegration.handleSuccessfulHit(user, target, stunTicks, damage);
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

    // Utility methods

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

    // Abstract methods that subclasses must implement

    /**
     * Check if attack can start - implement resource checks here
     * Return false to prevent attack from starting
     */
    protected abstract boolean canStart();

    /**
     * Consume resources needed for this attack (breath, stamina, etc.)
     * Return false if resources couldn't be consumed
     */
    protected abstract boolean consumeResources();

    /**
     * Refund resources if attack was cancelled in onStart()
     */
    protected abstract void refundResources();

    /**
     * Called when attack starts (after resource consumption)
     * Implement visual/audio startup effects here
     * If you call stop() in this method, resources will be refunded
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
}