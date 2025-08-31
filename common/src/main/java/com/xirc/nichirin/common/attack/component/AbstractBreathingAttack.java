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

    // Configuration from moveset - NO DEFAULT VALUES - MUST be configured
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

    // Self-ticking system - attacks register themselves for automatic ticking
    private static final java.util.concurrent.ConcurrentHashMap<Player, java.util.List<AbstractBreathingAttack<?, ?>>> selfTickingAttacks = new java.util.concurrent.ConcurrentHashMap<>();

    // Hit tracking
    @Setter
    private Set<UUID> hitEntities = new HashSet<>();
    @Setter
    private int hitCount = 0;

    // Configuration and breath consumption tracking
    private boolean configured = false;
    private boolean breathConsumed = false;

    /**
     * Configure this attack with values from the moveset
     * This MUST be called by the moveset before starting the attack
     */
    public void configure(MoveConfiguration config) {
        if (configured) {
            return; // Prevent double-configuration
        }

        // Combat Stats - REMOVED defaults - these MUST be provided by moveset
        this.damage = config.getDamageOrDefault(0f);
        this.range = config.getRangeOrDefault(0f);
        this.knockback = config.getKnockbackOrDefault(0f);
        this.hitStun = config.getHitStunOrDefault(0);
        this.hitboxSize = config.getHitboxSizeOrDefault(0f);

        // Timing
        this.cooldown = config.getCooldownOrDefault(0);
        this.windup = config.getWindupOrDefault(0);
        this.duration = config.getDurationOrDefault(0);

        // Resources
        this.breathCost = config.getBreathCostOrDefault(0f);

        // Movement (nullable - only set if configured in moveset)
        this.teleportDistance = config.getTeleportDistance();
        this.dashSpeed = config.getDashSpeed();
        this.teleportWindup = config.getTeleportWindup();

        this.configured = true;

        System.out.println("DEBUG: Attack " + this.getClass().getSimpleName() + " configured with:");
        System.out.println("  Damage: " + this.damage);
        System.out.println("  Range: " + this.range);
        System.out.println("  Hitbox Size: " + this.hitboxSize);
        System.out.println("  Duration: " + this.duration);
        System.out.println("  Windup: " + this.windup);
    }

    /**
     * Start the attack - unified interface (legacy compatibility)
     */
    public void start(Player user, Level world) {
        // CRITICAL: Check configuration first
        if (!configured) {
            System.err.println("ERROR: " + this.getClass().getSimpleName() + " cannot start - not configured!");
            return;
        }

        // Validate only that duration exists (attacks need to run for some time)
        if (duration <= 0) {
            System.err.println("ERROR: " + this.getClass().getSimpleName() + " has invalid configuration values!");
            System.err.println("  Duration: " + duration + " (must be > 0)");
            System.err.println("  All other values can be 0 for special attack types");
            return;
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

        // Register for self-ticking
        registerForTicking();

        System.out.println("DEBUG: Attack " + this.getClass().getSimpleName() + " starting with active=true");

        // Call onStart() - if this calls stop(), we'll handle it
        try {
            onStart();
        } catch (Exception e) {
            System.err.println("ERROR in onStart(): " + e.getMessage());
            e.printStackTrace();
            // Clean up on error
            this.isActive = false;
            if (breathConsumed) {
                BreathingManager.restore(user, breathCost);
                breathConsumed = false;
            }
            return;
        }

        // If onStart() called stop(), refund breath
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

        System.out.println("DEBUG: " + this.getClass().getSimpleName() + " tick " + tickCount +
                " (windup=" + windup + ", duration=" + duration + ", active=" + isActive + ")");

        // Check if we're past windup phase
        if (tickCount > windup) {
            try {
                perform();
            } catch (Exception e) {
                System.err.println("ERROR in perform(): " + e.getMessage());
                e.printStackTrace();
            }
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

            // Unregister from self-ticking
            unregisterFromTicking();

            System.out.println("DEBUG: Attack " + this.getClass().getSimpleName() + " stopped");
            try {
                onStop();
            } catch (Exception e) {
                System.err.println("ERROR in onStop(): " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Apply damage and effects to a target with immunity frames
     */
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;

        System.out.println("DEBUG: Hitting target " + target.getName().getString() + " with damage " + damage);

        // Check if already hit (for non-multi-hit attacks)
        if (hitEntities.contains(target.getUUID())) {
            System.out.println("DEBUG: Target already hit, skipping");
            return;
        }

        // Apply damage using configured values
        DamageSource source = user.damageSources().playerAttack(user);
        boolean damaged = target.hurt(source, damage);

        System.out.println("DEBUG: Damage applied: " + damaged + ", damage value: " + damage);

        if (damaged) {
            // Add combo tracking for breathing attacks
            com.xirc.nichirin.common.util.ComboIntegration.handleSuccessfulHit(user, target, hitStun, damage);
        }

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
            System.out.println("DEBUG: Applied knockback: " + knockback);
        }

        // Track hit
        hitEntities.add(target.getUUID());
        hitCount++;
    }

    /**
     * Special hit method that removes immunity frames
     */
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;

        System.out.println("DEBUG: Hitting target (no immunity) " + target.getName().getString() + " with damage " + damage);

        // Reset invulnerability to allow immediate damage
        target.invulnerableTime = 0;
        target.hurtTime = 0;

        // Apply damage
        DamageSource source = user.damageSources().playerAttack(user);
        boolean damaged = target.hurt(source, damage);

        System.out.println("DEBUG: No-immunity damage applied: " + damaged + ", damage value: " + damage);

        if (damaged) {
            // Add combo tracking for breathing attacks (no immunity version)
            com.xirc.nichirin.common.util.ComboIntegration.handleSuccessfulHit(user, target, hitStun, damage);
        }

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

        System.out.println("DEBUG: Checking hitbox at " + center + " with size " + hitboxSize);
        System.out.println("DEBUG: Hitbox bounds: " + hitbox);

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive());

        System.out.println("DEBUG: Found " + targets.size() + " targets in hitbox");
        return targets;
    }

    /**
     * Get entities in a custom hitbox with specified dimensions
     */
    protected List<LivingEntity> getTargetsInCustomHitbox(Vec3 center, double width, double height, double depth) {
        AABB hitbox = new AABB(
                center.x - width/2, center.y - height/2, center.z - depth/2,
                center.x + width/2, center.y + height/2, center.z + depth/2
        );

        System.out.println("DEBUG: Custom hitbox at " + center + " dimensions: " + width + "x" + height + "x" + depth);
        System.out.println("DEBUG: Custom hitbox bounds: " + hitbox);

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive());

        System.out.println("DEBUG: Found " + targets.size() + " targets in custom hitbox");
        return targets;
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

    /**
     * Register this attack for automatic ticking
     */
    private void registerForTicking() {
        if (user != null) {
            selfTickingAttacks.computeIfAbsent(user, k -> new java.util.ArrayList<>()).add(this);
            System.out.println("DEBUG: Registered " + this.getClass().getSimpleName() + " for self-ticking");
        }
    }

    /**
     * Unregister this attack from automatic ticking
     */
    private void unregisterFromTicking() {
        if (user != null) {
            var attacks = selfTickingAttacks.get(user);
            if (attacks != null) {
                attacks.remove(this);
                if (attacks.isEmpty()) {
                    selfTickingAttacks.remove(user);
                }
                System.out.println("DEBUG: Unregistered " + this.getClass().getSimpleName() + " from self-ticking");
            }
        }
    }

    /**
     * Tick all self-registered attacks - CALL THIS FROM YOUR MAIN TICK HANDLER
     */
    public static void tickAllActiveAttacks(net.minecraft.server.MinecraftServer server) {
        if (selfTickingAttacks.isEmpty()) {
            return;
        }

        java.util.List<Player> playersToClean = new java.util.ArrayList<>();

        for (var entry : selfTickingAttacks.entrySet()) {
            Player player = entry.getKey();
            var attacks = entry.getValue();

            if (player == null || !player.isAlive()) {
                playersToClean.add(player);
                continue;
            }

            java.util.List<AbstractBreathingAttack<?, ?>> toRemove = new java.util.ArrayList<>();

            synchronized (attacks) {
                for (var attack : new java.util.ArrayList<>(attacks)) {
                    try {
                        if (attack.isActive()) {
                            attack.tick();
                        } else {
                            toRemove.add(attack);
                        }
                    } catch (Exception e) {
                        System.err.println("Error ticking self-managed attack: " + e.getMessage());
                        toRemove.add(attack);
                    }
                }

                // Remove inactive attacks
                attacks.removeAll(toRemove);
            }
        }

        // Clean up disconnected players
        for (Player player : playersToClean) {
            selfTickingAttacks.remove(player);
        }
    }

    /**
     * Clear all self-ticking attacks for a player (on disconnect, death, etc.)
     */
    public static void clearSelfTickingAttacks(Player player) {
        var attacks = selfTickingAttacks.remove(player);
        if (attacks != null) {
            System.out.println("DEBUG: Clearing " + attacks.size() + " self-ticking attacks for " + player.getName().getString());
            for (var attack : attacks) {
                try {
                    attack.stop();
                } catch (Exception e) {
                    System.err.println("Error stopping self-ticking attack: " + e.getMessage());
                }
            }
        }
    }
}