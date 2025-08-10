package com.xirc.nichirin.common.attack.component;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset.MoveConfiguration;
import com.xirc.nichirin.common.util.BreathingManager;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

/**
 * Base class for all breathing technique attacks.
 * Now follows the same pattern as ThunderBreathingAttackBase with moveset configuration.
 */
@Getter
@SuppressWarnings("rawtypes")
public abstract class AbstractBreathingAttack<T extends AbstractBreathingAttack, A extends IBreathingAttacker> {

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

    // Configuration and breath consumption tracking
    private boolean configured = false;
    private boolean breathConsumed = false;

    // Legacy builder pattern support (for backward compatibility)
    // These will be overridden by configure() if called
    protected boolean builderConfigured = false;

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
        this.range = config.getRangeOrDefault(3.0f);
        this.knockback = config.getKnockbackOrDefault(0f);
        this.hitStun = config.getHitStunOrDefault(8);
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
        this.builderConfigured = false; // Moveset config overrides builder
    }

    /**
     * Legacy builder method support - will be overridden by configure() if called
     */
    @SuppressWarnings("unchecked")
    public T withTiming(int cooldown, int windup, int duration) {
        if (!configured) {
            this.cooldown = cooldown;
            this.windup = windup;
            this.duration = duration;
            this.builderConfigured = true;
        }
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withDamage(float damage) {
        if (!configured) {
            this.damage = damage;
            this.builderConfigured = true;
        }
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withRange(float range) {
        if (!configured) {
            this.range = range;
            this.builderConfigured = true;
        }
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withKnockback(float knockback) {
        if (!configured) {
            this.knockback = knockback;
            this.builderConfigured = true;
        }
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T withBreathCost(float cost) {
        if (!configured) {
            this.breathCost = cost;
            this.builderConfigured = true;
        }
        return (T) this;
    }

    /**
     * Start the attack - unified interface
     */
    public void start(Player user, Level world) {
        if (!configured && !builderConfigured) {
            System.err.println("Warning: " + this.getClass().getSimpleName() + " started without configuration!");
            setEmergencyDefaults();
        }

        this.user = user;
        this.world = world;
        this.tickCount = 0;
        this.breathConsumed = false;
        this.hitEntities.clear();
        this.hitCount = 0;

        // Check breath cost BEFORE marking as active
        if (breathCost > 0 && !BreathingManager.hasBreath(user, breathCost)) {
            user.displayClientMessage(
                    Component.literal("Not enough breath!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
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
            BreathingManager.restore(user, breathCost);
            breathConsumed = false;
        }
    }

    /**
     * Legacy start method for backward compatibility
     */
    public void start(Player player) {
        start(player, player.level());
    }

    /**
     * Start with attacker interface (legacy support)
     */
    public void start(A attacker) {
        Player player = attacker.getPlayer();
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
     * Legacy tick method for backward compatibility
     */
    public void tick(Player player) {
        tick();
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
     * Apply damage and effects to a target with immunity frames and Musical Score bonuses
     */
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;

        // Check if already hit (for non-multi-hit attacks)
        if (hitEntities.contains(target.getUUID())) {
            return;
        }

        // Get Musical Score breathing damage multiplier (includes rhythm bonus)
        float breathingMultiplier = com.xirc.nichirin.common.effect.MusicalScoreEffect.getBreathingDamageMultiplier(user);

        // Apply breathing damage multiplier to base damage
        float originalDamage = this.damage;
        this.damage = originalDamage * breathingMultiplier;

        // Log the damage boost if active
        if (breathingMultiplier > 1.0f) {
            com.xirc.nichirin.BreathOfNichirin.LOGGER.debug("Musical Score boosting {} breathing damage: {}x multiplier ({}->{})",
                    user.getName().getString(), breathingMultiplier, originalDamage, this.damage);

            // Show rhythm feedback to player
            if (breathingMultiplier >= 6.0f) { // 3.0 base * 2.0 rhythm = perfect timing
                user.displayClientMessage(net.minecraft.network.chat.Component.literal("PERFECT RHYTHM! 6x DAMAGE!")
                        .withStyle(style -> style.withColor(0x00FF00).withBold(true)), true);

                // Trigger client-side success feedback
                if (world.isClientSide) {
                    com.xirc.nichirin.client.gui.RhythmMeter.triggerSuccess();
                }
            } else if (breathingMultiplier >= 4.5f) { // 3.0 base * 1.5 rhythm = good timing
                user.displayClientMessage(net.minecraft.network.chat.Component.literal("Good Rhythm! 4.5x Damage!")
                        .withStyle(style -> style.withColor(0xFFFF00).withBold(true)), true);
            }
        }

        // Apply damage using configured values
        DamageSource source = user.damageSources().playerAttack(user);
        boolean damaged = target.hurt(source, damage);

        // Restore original damage for next hit
        this.damage = originalDamage;

        // Apply hit stun if configured
        if (hitStun > 0) {
            target.invulnerableTime = hitStun;

            // Apply actual stun effect
            MobEffectInstance stunInstance = new MobEffectInstance(
                    NichirinEffectRegistry.STUNNED.get(),
                    hitStun, // Duration in ticks
                    0, // Amplifier
                    false, // Ambient
                    true, // Show particles
                    true // Show icon
            );
            target.addEffect(stunInstance);
        }

        // Apply knockback if configured
        if (knockback > 0) {
            Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
            target.push(knockbackDir.x * knockback, 0.1, knockbackDir.z * knockback);
        }

        // Track hit
        hitEntities.add(target.getUUID());
        hitCount++;
    }

    /**
     * Special hit method that removes immunity frames with Musical Score bonuses
     */
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;

        // Get Musical Score breathing damage multiplier (includes rhythm bonus)
        float breathingMultiplier = com.xirc.nichirin.common.effect.MusicalScoreEffect.getBreathingDamageMultiplier(user);

        // Apply breathing damage multiplier to base damage
        float originalDamage = this.damage;
        this.damage = originalDamage * breathingMultiplier;

        // Show rhythm feedback for no-immunity hits too
        if (breathingMultiplier >= 6.0f) {
            user.displayClientMessage(net.minecraft.network.chat.Component.literal("PERFECT RHYTHM! 6x DAMAGE!")
                    .withStyle(style -> style.withColor(0x00FF00).withBold(true)), true);
        } else if (breathingMultiplier >= 4.5f) {
            user.displayClientMessage(net.minecraft.network.chat.Component.literal("Good Rhythm! 4.5x Damage!")
                    .withStyle(style -> style.withColor(0xFFFF00).withBold(true)), true);
        }

        // Reset invulnerability to allow immediate damage
        target.invulnerableTime = 0;
        target.hurtTime = 0;

        // Apply damage
        DamageSource source = user.damageSources().playerAttack(user);
        boolean damaged = target.hurt(source, damage);

        // Restore original damage
        this.damage = originalDamage;

        // Apply hit stun
        if (hitStun > 0) {
            target.invulnerableTime = hitStun;

            // Apply actual stun effect
            MobEffectInstance stunInstance = new MobEffectInstance(
                    NichirinEffectRegistry.STUNNED.get(),
                    hitStun, // Duration in ticks
                    0, // Amplifier
                    false, // Ambient
                    true, // Show particles
                    true // Show icon
            );
            target.addEffect(stunInstance);
        }

        // Apply knockback
        if (knockback > 0) {
            Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
            target.push(knockbackDir.x * knockback, 0.1, knockbackDir.z * knockback);
        }

        // Track hit (allow multiple hits for no-immunity attacks)
        hitCount++;
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

    /**
     * Check if breath was consumed (for debugging)
     */
    public boolean wasBreathConsumed() {
        return breathConsumed;
    }

    /**
     * Legacy method support for MoveClass registration
     */
    public void onRegister(com.xirc.nichirin.common.util.enums.MoveClass moveClass) {
        // Override if needed - default implementation does nothing
    }
}