package com.xirc.nichirin.common.attack.component;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset.MoveConfiguration;
import com.xirc.nichirin.common.util.ComboIntegration;
import lombok.Getter;
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
 * Base class specifically for katana attacks
 * Uses stamina resources and integrates with combo system
 */
@Getter
public abstract class KatanaAttack {

    // Configuration
    protected float damage = 4.0f;
    protected float range = 2.5f;
    protected float knockback = 0.3f;
    protected int hitStun = 15;
    protected float hitboxSize = 1.5f;
    protected int cooldown = 0;
    protected int windup = 5;
    protected int duration = 20;

    // Runtime state
    protected boolean isActive = false;
    protected int tickCount = 0;
    protected Player user;
    protected Level world;

    // Hit tracking
    private Set<UUID> hitEntities = new HashSet<>();
    private int hitCount = 0;

    /**
     * Configure this attack with values from a moveset
     */
    public void configure(MoveConfiguration config) {
        this.damage = config.getDamageOrDefault(4.0f);
        this.range = config.getRangeOrDefault(2.5f);
        this.knockback = config.getKnockbackOrDefault(0.3f);
        this.hitStun = config.getHitStunOrDefault(15);
        this.hitboxSize = config.getHitboxSizeOrDefault(1.5f);
        this.cooldown = config.getCooldownOrDefault(0);
        this.windup = config.getWindupOrDefault(5);
        this.duration = config.getDurationOrDefault(20);
    }

    /**
     * Builder-style configuration methods
     */
    public KatanaAttack withDamage(float damage) {
        this.damage = damage;
        return this;
    }

    public KatanaAttack withRange(float range) {
        this.range = range;
        return this;
    }

    public KatanaAttack withKnockback(float knockback) {
        this.knockback = knockback;
        return this;
    }

    public KatanaAttack withHitStun(int hitStun) {
        this.hitStun = hitStun;
        return this;
    }

    public KatanaAttack withHitboxSize(float hitboxSize) {
        this.hitboxSize = hitboxSize;
        return this;
    }

    public KatanaAttack withTiming(int cooldown, int windup, int duration) {
        this.cooldown = cooldown;
        this.windup = windup;
        this.duration = duration;
        return this;
    }

    /**
     * Start the attack
     */
    public void start(Player user) {
        this.user = user;
        this.world = user.level();
        this.tickCount = 0;
        this.hitEntities.clear();
        this.hitCount = 0;
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
            // Handle combo integration - this is the key part for combo HUD
            ComboIntegration.handleSuccessfulHit(user, target, hitStun, damage);

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
            ComboIntegration.handleSuccessfulHit(user, target, hitStun, damage);

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

    // Abstract methods that subclasses must implement

    /**
     * Called when attack starts
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
}