package com.xirc.nichirin.common.attack.component;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset.MoveConfiguration;
import com.xirc.nichirin.common.network.s2c.TriggerShaderPacket;
import com.xirc.nichirin.common.util.BreathingManager;
import com.xirc.nichirin.common.util.ComboIntegration;
import com.xirc.nichirin.common.util.HitboxData;
import com.xirc.nichirin.common.util.enums.MoveClass;
import com.xirc.nichirin.client.renderer.effects.AttackHitboxRenderer;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
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
import java.util.ArrayList;
import java.util.UUID;

/**
 * Base class for all breathing technique attacks.
 * Now follows the same pattern as ThunderBreathingAttackBase with moveset configuration.
 * Enhanced with rotation-aware hitbox system for directional attacks.
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
    protected LivingEntity user;
    protected Level world;

    // Self-ticking system - attacks register themselves for automatic ticking (Players only)
    private static final java.util.concurrent.ConcurrentHashMap<java.util.UUID, java.util.List<AbstractBreathingAttack<?, ?>>> selfTickingAttacks = new java.util.concurrent.ConcurrentHashMap<>();

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

        this.damage = config.getDamageOrDefault(0f);
        this.range = config.getRangeOrDefault(0f);
        this.knockback = config.getKnockbackOrDefault(0f);
        this.hitStun = config.getHitStunOrDefault(0);
        this.hitboxSize = config.getHitboxSizeOrDefault(0f);

        this.cooldown = config.getCooldownOrDefault(0);
        this.windup = config.getWindupOrDefault(0);
        this.duration = config.getDurationOrDefault(0);

        this.breathCost = config.getBreathCostOrDefault(0f);

        this.teleportDistance = config.getTeleportDistance();
        this.dashSpeed = config.getDashSpeed();
        this.teleportWindup = config.getTeleportWindup();

        this.configured = true;
    }

    /**
     * Start the attack for a Player (handles breath cost and client messages).
     */
    public void start(Player player, Level world) {
        if (!configured) return;
        if (duration <= 0) return;

        this.user = player;
        this.world = world;
        this.tickCount = 0;
        this.breathConsumed = false;
        this.hitEntities.clear();
        this.hitCount = 0;

        if (breathCost > 0 && !BreathingManager.hasBreath(player, breathCost)) {
            player.displayClientMessage(
                    Component.literal("Not enough breath!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            return;
        }

        if (breathCost > 0) {
            if (BreathingManager.consume(player, breathCost)) {
                breathConsumed = true;
            } else {
                player.displayClientMessage(
                        Component.literal("Failed to consume breath!")
                                .withStyle(style -> style.withColor(0xFF5555)),
                        true
                );
                return;
            }
        }

        this.isActive = true;
        registerForTicking();

        try {
            onStart();
        } catch (Exception e) {
            e.printStackTrace();
            this.isActive = false;
            if (breathConsumed) {
                BreathingManager.restore(player, breathCost);
                breathConsumed = false;
            }
            return;
        }

        if (!isActive && breathConsumed) {
            BreathingManager.restore(player, breathCost);
            breathConsumed = false;
        }
    }

    /**
     * Start the attack for any LivingEntity (NPC path — breath already consumed by caller).
     */
    public void start(LivingEntity entity, Level world) {
        if (entity instanceof Player player) {
            start(player, world);
            return;
        }
        if (!configured) return;
        if (duration <= 0) return;

        this.user = entity;
        this.world = world;
        this.tickCount = 0;
        this.breathConsumed = false;
        this.hitEntities.clear();
        this.hitCount = 0;

        this.isActive = true;
        registerForTicking();

        try {
            onStart();
        } catch (Exception e) {
            e.printStackTrace();
            this.isActive = false;
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
            try {
                perform();
            } catch (Exception e) {
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

            try {
                onStop();
            } catch (Exception e) {
                e.printStackTrace();
            }

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

        // Apply damage using configured values
        DamageSource source = user instanceof Player p
                ? user.damageSources().playerAttack(p)
                : user.damageSources().mobAttack(user);
        boolean damaged = target.hurt(source, damage);

        if (damaged && user instanceof Player player) {
            // Add combo tracking for breathing attacks (players only)
            ComboIntegration.handleSuccessfulHit(player, target, hitStun, damage);
        }

        // Apply hit stun if configured
        if (hitStun > 0) {
            target.invulnerableTime = hitStun;

            // Apply actual stun effect
            MobEffectInstance stunInstance = new MobEffectInstance(
                    NichirinEffectRegistry.STUNNED.get(),
                    hitStun, // Duration in ticks
                    2, // Amplifier
                    false, // Ambient
                    false, // Show particles
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

        // Trigger impact shake on the attacking player's screen (server → client packet).
        // Magnitude is derived from configured damage and hitStun so heavier hits feel stronger.
        if (damaged && user instanceof ServerPlayer sp) {
            float mag = (damage / 20.0f + hitStun / 20.0f) * 0.5f;
            mag = Math.max(0.2f, Math.min(2.0f, mag));
            TriggerShaderPacket impactPacket = new TriggerShaderPacket(
                    "com.xirc.nichirin.client.shader.ImpactShakeShaderEffect", true, mag);
            NichirinPacketRegistry.sendToPlayer(impactPacket, sp);
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
        DamageSource source = user instanceof Player p
                ? user.damageSources().playerAttack(p)
                : user.damageSources().mobAttack(user);
        boolean damaged = target.hurt(source, damage);

        if (damaged && user instanceof Player player) {
            // Add combo tracking for breathing attacks (players only, no immunity version)
            ComboIntegration.handleSuccessfulHit(player, target, hitStun, damage);
        }

        // Apply hit stun
        if (hitStun > 0) {
            target.invulnerableTime = hitStun;

            // Apply actual stun effect
            MobEffectInstance stunInstance = new MobEffectInstance(
                    NichirinEffectRegistry.STUNNED.get(),
                    hitStun, // Duration in ticks
                    2, // Amplifier
                    false, // Ambient
                    false, // Show particles
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
     * NOW WITH ROTATION SUPPORT: Hitboxes rotate based on player's look direction
     */
    protected List<LivingEntity> getTargetsInHitbox(Vec3 center) {
        HitboxData hitboxData = new HitboxData(hitboxSize);
        AABB hitbox = hitboxData.createAABBFromEntity(user);

        // Override center position if different from player position
        if (!center.equals(user.position().add(0, user.getBbHeight() / 2, 0))) {
            Vec3 lookDirection = user.getLookAngle();
            float yaw = (float) Math.toRadians(user.getYRot());
            hitbox = hitboxData.createAABB(center, lookDirection, yaw);
        }

        // Add to visual debugger if on client
        if (user.level().isClientSide) {
            AttackHitboxRenderer.addHitbox(hitbox);
        } else {
            // Server side - send packet to client for visual debugging
            if (user instanceof ServerPlayer serverPlayer) {
                NichirinPacketRegistry.sendHitboxToClient(
                        serverPlayer, hitbox, 2500L
                );
            }
        }

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive());

        return targets;
    }

    /**
     * Get entities in a custom hitbox with specified shape and size
     * NOW WITH ROTATION SUPPORT: Custom hitboxes also rotate with player direction
     */
    protected List<LivingEntity> getTargetsInCustomHitbox(Vec3 center, float size, HitboxData.HitboxShape shape) {
        HitboxData hitboxData = new HitboxData(size, shape);

        Vec3 lookDirection = user.getLookAngle();
        float yaw = (float) Math.toRadians(user.getYRot());
        AABB hitbox = hitboxData.createAABB(center, lookDirection, yaw);

        // Add to visual debugger if on client
        if (user.level().isClientSide) {
            AttackHitboxRenderer.addHitbox(hitbox);
        } else {
            // Server side - send packet to client
            if (user instanceof ServerPlayer serverPlayer) {
                NichirinPacketRegistry.sendHitboxToClient(
                        serverPlayer, hitbox, 2500L
                );
            }
        }

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive());

        return targets;
    }

    /**
     * Legacy method for backward compatibility - converts old parameters to new system
     */
    protected List<LivingEntity> getTargetsInCustomHitbox(Vec3 center, double width, double height, double depth) {
        // Convert dimensions to our new system
        float size = (float) Math.max(width, Math.max(height, depth));

        // Determine best shape based on dimensions
        HitboxData.HitboxShape shape;
        if (width > height && width > depth) {
            shape = HitboxData.HitboxShape.WIDE;
        } else if (height > width && height > depth) {
            shape = HitboxData.HitboxShape.TALL;
        } else if (depth > width && depth > height) {
            shape = HitboxData.HitboxShape.LONG;
        } else {
            shape = HitboxData.HitboxShape.CUBE;
        }

        return getTargetsInCustomHitbox(center, size, shape);
    }

    /**
     * Get entities in a line between two points
     * NOW WITH ROTATION SUPPORT: Line hitboxes can be rotated based on the line direction
     */
    protected List<LivingEntity> getTargetsInLine(Vec3 start, Vec3 end, double thickness) {
        // Calculate the distance and create multiple hitboxes along the line
        double distance = start.distanceTo(end);
        int hitboxCount = Math.max(1, (int) Math.ceil(distance / thickness));

        Set<LivingEntity> allTargets = new HashSet<>();
        Set<AABB> lineHitboxes = new HashSet<>();

        // Calculate the direction of the line for rotation
        Vec3 lineDirection = end.subtract(start).normalize();
        float lineYaw = (float) Math.atan2(-lineDirection.x, lineDirection.z);

        for (int i = 0; i <= hitboxCount; i++) {
            double progress = hitboxCount > 0 ? (double) i / hitboxCount : 0;
            Vec3 hitboxCenter = start.lerp(end, progress);

            // Create a LONG shaped hitbox oriented along the line direction
            HitboxData hitboxData = new HitboxData((float) thickness, HitboxData.HitboxShape.LONG);
            AABB hitbox = hitboxData.createAABB(hitboxCenter, lineDirection, lineYaw);
            lineHitboxes.add(hitbox);

            List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                    entity -> entity != user && entity.isAlive());
            allTargets.addAll(targets);
        }

        // Add all hitboxes to visual debugger if on client
        if (user.level().isClientSide) {
            AttackHitboxRenderer.addHitboxes(lineHitboxes);
        } else {
            // Server side - send packets for each hitbox
            if (user instanceof ServerPlayer serverPlayer) {
                for (AABB hitbox : lineHitboxes) {
                    NichirinPacketRegistry.sendHitboxToClient(
                            serverPlayer, hitbox, 2500L
                    );
                }
            }
        }

        return new ArrayList<>(allTargets);
    }

    /**
     * Create a hitbox at the configured range from the player
     * NOW WITH ROTATION SUPPORT: Range hitboxes are positioned and oriented correctly
     */
    protected List<LivingEntity> getTargetsAtRange() {
        Vec3 playerPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDirection = user.getLookAngle();
        Vec3 hitboxCenter = playerPos.add(lookDirection.scale(range));

        HitboxData hitboxData = new HitboxData(hitboxSize);
        AABB hitbox = hitboxData.createAABBFromEntity(user);

        // Override center to be at range distance
        float yaw = (float) Math.toRadians(user.getYRot());
        hitbox = hitboxData.createAABB(hitboxCenter, lookDirection, yaw);

        // Add to visual debugger if on client
        if (user.level().isClientSide) {
            AttackHitboxRenderer.addHitbox(hitbox);
        } else {
            // Server side - send packet to client
            if (user instanceof ServerPlayer serverPlayer) {
                NichirinPacketRegistry.sendHitboxToClient(
                        serverPlayer, hitbox, 2500L
                );
            }
        }

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive());

        return targets;
    }

    /**
     * Create a hitbox at the configured range with custom shape
     * NOW WITH ROTATION SUPPORT: Custom shaped hitboxes at range rotate properly
     */
    protected List<LivingEntity> getTargetsAtRange(HitboxData.HitboxShape shape) {
        Vec3 playerPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDirection = user.getLookAngle();
        Vec3 hitboxCenter = playerPos.add(lookDirection.scale(range));

        return getTargetsInCustomHitbox(hitboxCenter, hitboxSize, shape);
    }

    /**
     * Create multiple hitboxes from player to max range
     * NOW WITH ROTATION SUPPORT: Range line follows player's look direction
     */
    protected List<LivingEntity> getTargetsInRangeLine(float spacing) {
        Vec3 playerPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDirection = user.getLookAngle();
        Vec3 endPos = playerPos.add(lookDirection.scale(range));

        return getTargetsInLine(playerPos, endPos, spacing);
    }

    /**
     * Create a cone of hitboxes at the configured range
     * NOW WITH ROTATION SUPPORT: Cone properly rotates with player direction
     */
    protected List<LivingEntity> getTargetsInCone(float coneAngle, int hitboxCount) {
        Vec3 playerPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDirection = user.getLookAngle();
        float playerYaw = (float) Math.toRadians(user.getYRot());

        Set<LivingEntity> allTargets = new HashSet<>();
        Set<AABB> coneHitboxes = new HashSet<>();

        float angleStep = coneAngle / (hitboxCount - 1);
        float startAngle = -coneAngle / 2;

        for (int i = 0; i < hitboxCount; i++) {
            float angle = startAngle + (i * angleStep);

            // Calculate the yaw for this hitbox (player yaw + cone offset)
            float hitboxYaw = playerYaw + (float) Math.toRadians(angle);

            // Calculate direction vector for this angle
            Vec3 rotatedDirection = rotateVectorY(lookDirection, Math.toRadians(angle));
            Vec3 hitboxCenter = playerPos.add(rotatedDirection.scale(range));

            // Create hitbox with proper rotation
            HitboxData hitboxData = new HitboxData(hitboxSize);
            AABB hitbox = hitboxData.createAABB(hitboxCenter, rotatedDirection, hitboxYaw);
            coneHitboxes.add(hitbox);

            List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                    entity -> entity != user && entity.isAlive());
            allTargets.addAll(targets);
        }

        // Add all hitboxes to visual debugger if on client
        if (user.level().isClientSide) {
            AttackHitboxRenderer.addHitboxes(coneHitboxes);
        } else {
            // Server side - send packets
            if (user instanceof ServerPlayer serverPlayer) {
                for (AABB hitbox : coneHitboxes) {
                    NichirinPacketRegistry.sendHitboxToClient(
                            serverPlayer, hitbox, 2500L
                    );
                }
            }
        }

        return new ArrayList<>(allTargets);
    }

    /**
     * Create a fan/sweep attack that covers an arc in front of the player
     * NEW METHOD: Creates multiple hitboxes in an arc pattern with proper rotation
     */
    protected List<LivingEntity> getTargetsInSweep(float sweepAngle, float sweepRange, int hitboxCount) {
        Vec3 playerPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDirection = user.getLookAngle();
        float playerYaw = (float) Math.toRadians(user.getYRot());

        Set<LivingEntity> allTargets = new HashSet<>();
        Set<AABB> sweepHitboxes = new HashSet<>();

        float angleStep = sweepAngle / (hitboxCount - 1);
        float startAngle = -sweepAngle / 2;

        for (int i = 0; i < hitboxCount; i++) {
            float angle = startAngle + (i * angleStep);
            float hitboxYaw = playerYaw + (float) Math.toRadians(angle);

            // Calculate position for this hitbox
            Vec3 rotatedDirection = rotateVectorY(lookDirection, Math.toRadians(angle));
            Vec3 hitboxCenter = playerPos.add(rotatedDirection.scale(sweepRange));

            // Create WIDE hitbox for sweep attacks
            HitboxData hitboxData = new HitboxData(hitboxSize, HitboxData.HitboxShape.WIDE);
            AABB hitbox = hitboxData.createAABB(hitboxCenter, rotatedDirection, hitboxYaw);
            sweepHitboxes.add(hitbox);

            List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                    entity -> entity != user && entity.isAlive());
            allTargets.addAll(targets);
        }

        // Add all hitboxes to visual debugger
        if (user.level().isClientSide) {
            AttackHitboxRenderer.addHitboxes(sweepHitboxes);
        } else {
            if (user instanceof ServerPlayer serverPlayer) {
                for (AABB hitbox : sweepHitboxes) {
                    NichirinPacketRegistry.sendHitboxToClient(
                            serverPlayer, hitbox, 2500L
                    );
                }
            }
        }

        return new ArrayList<>(allTargets);
    }

    /**
     * Create a thrust attack with a long hitbox extending from the player
     * NEW METHOD: Creates a single long hitbox that follows player direction
     */
    protected List<LivingEntity> getTargetsInThrust() {
        Vec3 playerPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDirection = user.getLookAngle();

        // Position the thrust hitbox halfway between player and max range
        Vec3 hitboxCenter = playerPos.add(lookDirection.scale(range / 2));

        // Create a LONG hitbox with the range as the size
        HitboxData hitboxData = new HitboxData(range, HitboxData.HitboxShape.LONG);
        AABB hitbox = hitboxData.createAABBFromEntity(user);

        // Override center position
        float yaw = (float) Math.toRadians(user.getYRot());
        hitbox = hitboxData.createAABB(hitboxCenter, lookDirection, yaw);

        // Add to visual debugger
        if (user.level().isClientSide) {
            AttackHitboxRenderer.addHitbox(hitbox);
        } else {
            if (user instanceof ServerPlayer serverPlayer) {
                NichirinPacketRegistry.sendHitboxToClient(
                        serverPlayer, hitbox, 2500L
                );
            }
        }

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                entity -> entity != user && entity.isAlive());

        return targets;
    }

    /**
     * Create a 360-degree circular attack around the player
     * NEW METHOD: Creates multiple hitboxes in a circle around the player
     */
    protected List<LivingEntity> getTargetsInCircle(float radius, int hitboxCount) {
        Vec3 playerPos = user.position().add(0, user.getBbHeight() / 2, 0);

        Set<LivingEntity> allTargets = new HashSet<>();
        Set<AABB> circleHitboxes = new HashSet<>();

        float angleStep = 360f / hitboxCount;

        for (int i = 0; i < hitboxCount; i++) {
            float angle = i * angleStep;
            float angleRadians = (float) Math.toRadians(angle);

            // Calculate position around the circle
            double offsetX = radius * Math.cos(angleRadians);
            double offsetZ = radius * Math.sin(angleRadians);
            Vec3 hitboxCenter = playerPos.add(offsetX, 0, offsetZ);

            // Calculate direction from player to hitbox for rotation
            Vec3 direction = hitboxCenter.subtract(playerPos).normalize();

            HitboxData hitboxData = new HitboxData(hitboxSize);
            AABB hitbox = hitboxData.createAABB(hitboxCenter, direction, angleRadians);
            circleHitboxes.add(hitbox);

            List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, hitbox,
                    entity -> entity != user && entity.isAlive());
            allTargets.addAll(targets);
        }

        // Add all hitboxes to visual debugger
        if (user.level().isClientSide) {
            AttackHitboxRenderer.addHitboxes(circleHitboxes);
        } else {
            if (user instanceof ServerPlayer serverPlayer) {
                for (AABB hitbox : circleHitboxes) {
                    NichirinPacketRegistry.sendHitboxToClient(
                            serverPlayer, hitbox, 2500L
                    );
                }
            }
        }

        return new ArrayList<>(allTargets);
    }

    /**
     * Utility method to rotate a vector around the Y axis
     */
    private Vec3 rotateVectorY(Vec3 vector, double angleRadians) {
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);

        double newX = vector.x * cos - vector.z * sin;
        double newZ = vector.x * sin + vector.z * cos;

        return new Vec3(newX, vector.y, newZ);
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
     *
     * Default implementation: Creates a single hitbox at range and processes targets
     * Override this for custom attack patterns
     */
    protected void perform() {
        // Default implementation for simple attacks
        List<LivingEntity> targets = getTargetsAtRange();
        for (LivingEntity target : targets) {
            hitTarget(target);
        }
    }

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
    public void onRegister(MoveClass moveClass) {
        // Override if needed - default implementation does nothing
    }

    /**
     * Register this attack for automatic ticking
     */
    private void registerForTicking() {
        // Only Player attacks self-tick; NPC attacks are ticked via MoveExecutor.tickAttacks.
        if (user instanceof Player) {
            selfTickingAttacks.computeIfAbsent(user.getUUID(), k -> new java.util.ArrayList<>()).add(this);
        }
    }

    /**
     * Unregister this attack from automatic ticking
     */
    private void unregisterFromTicking() {
        if (user instanceof Player) {
            var attacks = selfTickingAttacks.get(user.getUUID());
            if (attacks != null) {
                attacks.remove(this);
                if (attacks.isEmpty()) {
                    selfTickingAttacks.remove(user.getUUID());
                }
            }
        }
    }

    /**
     * Tick all self-registered attacks - CALL THIS FROM YOUR MAIN TICK HANDLER
     */
    public static void tickAllActiveAttacks(MinecraftServer server) {
        if (selfTickingAttacks.isEmpty()) {
            return;
        }

        java.util.List<java.util.UUID> toClean = new java.util.ArrayList<>();

        for (var entry : selfTickingAttacks.entrySet()) {
            java.util.UUID uuid = entry.getKey();
            var attacks = entry.getValue();

            // Remove stale entries for UUIDs with no active attacks
            if (attacks.isEmpty()) {
                toClean.add(uuid);
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
                        toRemove.add(attack);
                    }
                }

                attacks.removeAll(toRemove);
            }
        }

        for (java.util.UUID uuid : toClean) {
            selfTickingAttacks.remove(uuid);
        }
    }

    /**
     * Clear all self-ticking attacks for a player (on disconnect, death, etc.)
     */
    public static void clearSelfTickingAttacks(Player player) {
        var attacks = selfTickingAttacks.remove(player.getUUID());
        if (attacks != null) {
            for (var attack : attacks) {
                try {
                    attack.stop();
                } catch (Exception e) {
                    // Ignore exceptions during cleanup
                }
            }
        }
    }
}