package com.xirc.nichirin.common.attack.component;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset.MoveConfiguration;
import com.xirc.nichirin.common.util.ComboIntegration;
import com.xirc.nichirin.common.util.HitboxData;
import com.xirc.nichirin.client.renderer.effects.AttackHitboxRenderer;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import com.xirc.nichirin.common.system.DemonManager;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for all demon art attacks.
 * Similar to AbstractBreathingAttack but for demon abilities - no breath/stamina costs.
 * Fixed hitbox system with proper positioning and no rotation issues.
 * Now includes blood restoration on successful hits (1 blood every 3 hits).
 *
 * REFACTORED: Now supports both Players and NPCs (uses LivingEntity)
 */
@Getter
@SuppressWarnings("rawtypes")
public abstract class AbstractDemonAttack<T extends AbstractDemonAttack, A extends IDemonAttacker> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDemonAttack.class);

    // Configuration from moveset - NO DEFAULT VALUES - MUST be configured
    protected float damage;
    protected float range;
    protected float knockback;
    protected int hitStun;
    protected float hitboxSize;
    protected int cooldown;
    protected int windup;
    protected int duration;

    // Blood restoration properties (3 hits = 1 blood system)
    protected int hitsForBlood = 3; // Default: 3 hits for 1 blood point
    protected int maxBloodPerAttack = 3; // Default: maximum 3 blood points per attack
    protected int bloodOnKill = 3; // Default: 3 blood points for killing

    // Movement properties (nullable - not all attacks use these)
    protected Float teleportDistance;
    protected Float dashSpeed;
    protected Integer teleportWindup;

    // Runtime state - CHANGED FROM Player TO LivingEntity for NPC support
    @Setter
    protected boolean isActive = false;
    protected int tickCount = 0;
    protected LivingEntity user;
    protected Level world;

    // Self-ticking system - CHANGED to use UUID instead of Player for NPC support
    private static final ConcurrentHashMap<UUID, List<AbstractDemonAttack<?, ?>>> selfTickingAttacks = new ConcurrentHashMap<>();

    // Hit tracking
    @Setter
    private Set<UUID> hitEntities = new HashSet<>();
    @Setter
    private int hitCount = 0;
    private int hitCountForBlood = 0; // Track hits for blood gain (every 3 hits)
    private int bloodGainedThisAttack = 0; // Track blood gained to enforce max

    // Configuration tracking
    private boolean configured = false;

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

        // Blood restoration uses hardcoded defaults
        // Can be overridden by subclasses if needed

        // Movement (nullable - only set if configured in moveset)
        this.teleportDistance = config.getTeleportDistance();
        this.dashSpeed = config.getDashSpeed();
        this.teleportWindup = config.getTeleportWindup();

        this.configured = true;
    }

    /**
     * Start the attack - REFACTORED: Now works with LivingEntity (Players AND NPCs)
     */
    public void start(LivingEntity user, Level world) {
        // CRITICAL: Check configuration first
        if (!configured) {
            LOGGER.warn("Demon attack {} not configured before start()", this.getClass().getSimpleName());
            return;
        }

        // Validate only that duration exists (attacks need to run for some time)
        if (duration <= 0) {
            LOGGER.warn("Demon attack {} has invalid duration: {}", this.getClass().getSimpleName(), duration);
            return;
        }

        this.user = user;
        this.world = world;
        this.tickCount = 0;
        this.hitEntities.clear();
        this.hitCount = 0;
        this.hitCountForBlood = 0;
        this.bloodGainedThisAttack = 0;

        // No resource cost check for demon attacks - they're free to use

        // Mark as active
        this.isActive = true;

        // Register for self-ticking
        registerForTicking();

        // Call onStart()
        try {
            onStart();
        } catch (Exception e) {
            LOGGER.error("Error in demon attack {} onStart()", this.getClass().getSimpleName(), e);
            // Clean up on error
            this.isActive = false;
            return;
        }
    }

    /**
     * Legacy start method for backward compatibility - Player version
     */
    public void start(Player player, Level world) {
        start((LivingEntity) player, world);
    }

    /**
     * Legacy start method for backward compatibility - Player only
     */
    public void start(Player player) {
        start((LivingEntity) player, player.level());
    }

    /**
     * Start with attacker interface - Player only
     */
    public void start(A attacker) {
        Player player = attacker.getPlayer();
        start((LivingEntity) player, player.level());
    }

    /**
     * Tick the attack - called every game tick while active
     */
    public void tick() {
        if (!isActive || user == null || world == null) {
            return;
        }

        // Check if user is still alive
        if (!user.isAlive()) {
            stop();
            return;
        }

        // For players, check if they're still a demon
        // For NPCs, just continue (they're always demons if they have demon moveset)
        if (user instanceof Player player && !DemonManager.isDemon(player)) {
            stop();
            return;
        }

        tickCount++;

        // Cancel attack if the user was hit during windup
        if (tickCount <= windup && user.hurtTime > 0 && windup > 0) {
            stop();
            return;
        }

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
     * Now includes blood restoration for demons (1 blood every 3 hits)
     */
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;

        // Check if already hit (for non-multi-hit attacks)
        if (hitEntities.contains(target.getUUID())) {
            return;
        }

        // Apply damage using configured values
        DamageSource source;
        if (user instanceof Player player) {
            source = user.damageSources().playerAttack(player);
        } else {
            source = user.damageSources().mobAttack(user);
        }
        boolean damaged = target.hurt(source, damage);

        if (damaged) {
            // Add combo tracking for demon attacks (only for players)
            if (user instanceof Player player) {
                ComboIntegration.handleSuccessfulHit(player, target, hitStun, damage);
            }

            // Handle blood restoration (1 blood every 3 hits) - ONLY for players
            handleBloodGain(target);
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
    }

    /**
     * Special hit method that removes immunity frames
     * Now includes blood restoration for demons (1 blood every 3 hits)
     */
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;

        // Reset invulnerability to allow immediate damage
        target.invulnerableTime = 0;
        target.hurtTime = 0;

        // Apply damage
        DamageSource source;
        if (user instanceof Player player) {
            source = user.damageSources().playerAttack(player);
        } else {
            source = user.damageSources().mobAttack(user);
        }
        boolean damaged = target.hurt(source, damage);

        if (damaged) {
            // Add combo tracking for demon attacks (no immunity version) - only for players
            if (user instanceof Player player) {
                ComboIntegration.handleSuccessfulHit(player, target, hitStun, damage);
            }

            // Handle blood restoration (1 blood every 3 hits) - ONLY for players
            handleBloodGain(target);
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
     * Handle blood gain from hitting targets
     * 1 blood every 3 hits, 3 blood on kill
     * FIXED: Only works for Players (NPCs use their own blood system)
     */
    private void handleBloodGain(LivingEntity target) {
        // Only players have blood points managed by DemonManager
        // NPCs have their own blood system through NPCResourceManager
        if (!(user instanceof Player player)) {
            return; // Skip blood gain for NPCs (handled by NPC system)
        }

        if (!DemonManager.isDemon(player)) {
            return;
        }

        // Check if target dies from this hit (for kill bonus)
        boolean targetDied = !target.isAlive() || target.getHealth() <= 0;

        if (targetDied) {
            // Kill rewards always apply — multi-kill AoE grants blood per kill.
            // The maxBloodPerAttack cap intentionally does NOT apply to kills.
            if (bloodOnKill > 0) {
                DemonManager.addBloodPoints(player, bloodOnKill);

                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(
                            Component.literal("§c+" + bloodOnKill + " Blood (Kill)")
                                    .withStyle(style -> style.withColor(0xDC143C)),
                            true
                    );
                }
            }
        } else if (bloodGainedThisAttack < maxBloodPerAttack) {
            // Count hits for blood gain (every 3 hits = 1 blood)
            hitCountForBlood++;
            if (hitCountForBlood >= hitsForBlood) {
                int bloodToGain = Math.min(1, maxBloodPerAttack - bloodGainedThisAttack);
                if (bloodToGain > 0) {
                    DemonManager.addBloodPoints(player, bloodToGain);
                    bloodGainedThisAttack += bloodToGain;
                    hitCountForBlood = 0; // Reset hit counter

                    // Send feedback message
                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.displayClientMessage(
                                Component.literal("§c+" + bloodToGain + " Blood")
                                        .withStyle(style -> style.withColor(0xDC143C)),
                                true
                        );
                    }
                }
            }
        }
    }

    /**
     * Get entities in a hitbox centered at the given position
     * FIXED: Uses position-based detection instead of rotated AABBs
     */
    protected List<LivingEntity> getTargetsInHitbox(Vec3 center) {
        // Use a simple search area around the center
        double searchRadius = hitboxSize + 2; // Add buffer for safety
        AABB searchArea = new AABB(
                center.x - searchRadius, center.y - searchRadius, center.z - searchRadius,
                center.x + searchRadius, center.y + searchRadius, center.z + searchRadius
        );

        List<LivingEntity> potentialTargets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity != user && entity.isAlive());

        // Filter by actual distance
        List<LivingEntity> validTargets = new ArrayList<>();
        for (LivingEntity target : potentialTargets) {
            Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
            double distance = center.distanceTo(targetPos);

            if (distance <= hitboxSize) {
                validTargets.add(target);
            }
        }

        // Create simple visual hitbox for debugging
        AABB visualHitbox = new AABB(
                center.x - hitboxSize/2, center.y - hitboxSize/2, center.z - hitboxSize/2,
                center.x + hitboxSize/2, center.y + hitboxSize/2, center.z + hitboxSize/2
        );

        if (user.level().isClientSide) {
            AttackHitboxRenderer.addHitbox(visualHitbox);
        } else {
            NichirinPacketRegistry.sendHitboxToTracking(user, visualHitbox, 2500L);
        }

        return validTargets;
    }

    /**
     * Get entities in a custom hitbox with specified shape and size
     * FIXED: Uses simple distance checking instead of rotation
     */
    protected List<LivingEntity> getTargetsInCustomHitbox(Vec3 center, float size, HitboxData.HitboxShape shape) {
        // Use distance-based detection regardless of shape
        double searchRadius = size + 2;
        AABB searchArea = new AABB(
                center.x - searchRadius, center.y - searchRadius, center.z - searchRadius,
                center.x + searchRadius, center.y + searchRadius, center.z + searchRadius
        );

        List<LivingEntity> potentialTargets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity != user && entity.isAlive());

        List<LivingEntity> validTargets = new ArrayList<>();
        for (LivingEntity target : potentialTargets) {
            Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
            double distance = center.distanceTo(targetPos);

            if (distance <= size) {
                validTargets.add(target);
            }
        }

        // Simple visual representation
        AABB visualHitbox = new AABB(
                center.x - size/2, center.y - size/2, center.z - size/2,
                center.x + size/2, center.y + size/2, center.z + size/2
        );

        if (user.level().isClientSide) {
            AttackHitboxRenderer.addHitbox(visualHitbox);
        } else {
            NichirinPacketRegistry.sendHitboxToTracking(user, visualHitbox, 2500L);
        }

        return validTargets;
    }

    /**
     * Legacy method for backward compatibility
     */
    protected List<LivingEntity> getTargetsInCustomHitbox(Vec3 center, double width, double height, double depth) {
        float size = (float) Math.max(width, Math.max(height, depth));
        return getTargetsInCustomHitbox(center, size, HitboxData.HitboxShape.CUBE);
    }

    /**
     * Get entities in a line between two points
     * FIXED: Uses mathematical line-to-point distance calculation
     */
    protected List<LivingEntity> getTargetsInLine(Vec3 start, Vec3 end, double thickness) {
        double lineLength = start.distanceTo(end);
        Vec3 center = start.add(end).scale(0.5);
        double searchRadius = Math.max(lineLength, thickness) + 2;

        AABB searchArea = new AABB(
                center.x - searchRadius, center.y - searchRadius, center.z - searchRadius,
                center.x + searchRadius, center.y + searchRadius, center.z + searchRadius
        );

        List<LivingEntity> potentialTargets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity != user && entity.isAlive());

        List<LivingEntity> validTargets = new ArrayList<>();
        Vec3 lineDirection = end.subtract(start).normalize();

        for (LivingEntity target : potentialTargets) {
            Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
            Vec3 toTarget = targetPos.subtract(start);

            // Project target onto line
            double projection = toTarget.dot(lineDirection);
            projection = Math.max(0, Math.min(lineLength, projection)); // Clamp to line segment

            Vec3 closestPoint = start.add(lineDirection.scale(projection));
            double distanceToLine = targetPos.distanceTo(closestPoint);

            if (distanceToLine <= thickness / 2) {
                validTargets.add(target);
            }
        }

        // Simple visual representation
        AABB visualBox = new AABB(
                Math.min(start.x, end.x) - thickness/2,
                Math.min(start.y, end.y) - thickness/2,
                Math.min(start.z, end.z) - thickness/2,
                Math.max(start.x, end.x) + thickness/2,
                Math.max(start.y, end.y) + thickness/2,
                Math.max(start.z, end.z) + thickness/2
        );

        if (user.level().isClientSide) {
            AttackHitboxRenderer.addHitbox(visualBox);
        } else {
            NichirinPacketRegistry.sendHitboxToTracking(user, visualBox, 2500L);
        }

        return validTargets;
    }

    /**
     * Create a hitbox at the configured range from the user
     * FIXED: Uses simple positioning without rotation
     */
    protected List<LivingEntity> getTargetsAtRange() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDirection = user.getLookAngle();
        Vec3 hitboxCenter = userPos.add(lookDirection.scale(range));

        return getTargetsInHitbox(hitboxCenter);
    }

    /**
     * Create a hitbox at the configured range with custom shape
     */
    protected List<LivingEntity> getTargetsAtRange(HitboxData.HitboxShape shape) {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDirection = user.getLookAngle();
        Vec3 hitboxCenter = userPos.add(lookDirection.scale(range));

        return getTargetsInCustomHitbox(hitboxCenter, hitboxSize, shape);
    }

    /**
     * Create multiple hitboxes from user to max range
     * FIXED: Uses proper line detection
     */
    protected List<LivingEntity> getTargetsInRangeLine(float spacing) {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDirection = user.getLookAngle();
        Vec3 endPos = userPos.add(lookDirection.scale(range));

        double thickness = Math.min(spacing, hitboxSize);
        return getTargetsInLine(userPos, endPos, thickness);
    }

    /**
     * Create a cone of hitboxes at the configured range
     * FIXED: Uses angle-based detection instead of rotated hitboxes
     */
    protected List<LivingEntity> getTargetsInCone(float coneAngle, int hitboxCount) {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDirection = user.getLookAngle().normalize();

        // Use broader search area
        double searchRadius = range + hitboxSize;
        AABB searchArea = new AABB(
                userPos.x - searchRadius, userPos.y - searchRadius, userPos.z - searchRadius,
                userPos.x + searchRadius, userPos.y + searchRadius, userPos.z + searchRadius
        );

        List<LivingEntity> potentialTargets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity != user && entity.isAlive());

        List<LivingEntity> validTargets = new ArrayList<>();
        double coneAngleRad = Math.toRadians(coneAngle / 2); // Half angle for easier calculation
        double cosHalfAngle = Math.cos(coneAngleRad);

        for (LivingEntity target : potentialTargets) {
            Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
            Vec3 toTarget = targetPos.subtract(userPos);
            double distance = toTarget.length();

            if (distance <= range && distance > 0) {
                Vec3 toTargetNorm = toTarget.normalize();
                double dotProduct = lookDirection.dot(toTargetNorm);

                // Check if target is within cone angle
                if (dotProduct >= cosHalfAngle) {
                    validTargets.add(target);
                }
            }
        }

        // Create visual cone representation (simplified)
        List<AABB> coneBoxes = new ArrayList<>();
        for (int i = 0; i < 5; i++) { // Just 5 visual segments
            double segmentRange = range * (i + 1) / 5.0;
            Vec3 segmentCenter = userPos.add(lookDirection.scale(segmentRange));
            double segmentSize = hitboxSize * (i + 1) / 5.0; // Growing cone

            AABB segmentBox = new AABB(
                    segmentCenter.x - segmentSize/2, segmentCenter.y - segmentSize/2, segmentCenter.z - segmentSize/2,
                    segmentCenter.x + segmentSize/2, segmentCenter.y + segmentSize/2, segmentCenter.z + segmentSize/2
            );
            coneBoxes.add(segmentBox);
        }

        if (user.level().isClientSide) {
            AttackHitboxRenderer.addHitboxes(new HashSet<>(coneBoxes));
        } else {
            for (AABB hitbox : coneBoxes) {
                NichirinPacketRegistry.sendHitboxToTracking(user, hitbox, 2500L);
            }
        }

        return validTargets;
    }

    /**
     * Create a fan/sweep attack that covers an arc in front of the user
     */
    protected List<LivingEntity> getTargetsInSweep(float sweepAngle, float sweepRange, int hitboxCount) {
        return getTargetsInCone(sweepAngle, hitboxCount); // Reuse cone logic
    }

    /**
     * FIXED: Thrust attack using line detection
     */
    protected List<LivingEntity> getTargetsInThrust() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDirection = user.getLookAngle();
        Vec3 endPos = userPos.add(lookDirection.scale(range));

        return getTargetsInLine(userPos, endPos, hitboxSize);
    }

    /**
     * Create a 360-degree circular attack around the user
     * FIXED: Uses simple distance checking
     */
    protected List<LivingEntity> getTargetsInCircle(float radius, int hitboxCount) {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        double searchRadius = radius + hitboxSize;
        AABB searchArea = new AABB(
                userPos.x - searchRadius, userPos.y - searchRadius, userPos.z - searchRadius,
                userPos.x + searchRadius, userPos.y + searchRadius, userPos.z + searchRadius
        );

        List<LivingEntity> potentialTargets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity != user && entity.isAlive());

        List<LivingEntity> validTargets = new ArrayList<>();

        for (LivingEntity target : potentialTargets) {
            Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
            double distance = userPos.distanceTo(targetPos);

            if (distance <= radius) {
                validTargets.add(target);
            }
        }

        // Simple circular visual representation
        AABB circleBox = new AABB(
                userPos.x - radius, userPos.y - hitboxSize/2, userPos.z - radius,
                userPos.x + radius, userPos.y + hitboxSize/2, userPos.z + radius
        );

        if (user.level().isClientSide) {
            AttackHitboxRenderer.addHitbox(circleBox);
        } else {
            NichirinPacketRegistry.sendHitboxToTracking(user, circleBox, 2500L);
        }

        return validTargets;
    }

    // Abstract methods that must be implemented by subclasses

    /**
     * Called when attack starts
     * Implement visual/audio startup effects here
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
     * Register this attack for automatic ticking - USES UUID for NPC support
     */
    private void registerForTicking() {
        if (user != null) {
            selfTickingAttacks.computeIfAbsent(user.getUUID(), k -> new ArrayList<>()).add(this);
        }
    }

    /**
     * Unregister this attack from automatic ticking
     */
    private void unregisterFromTicking() {
        if (user != null) {
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
     * Tick all self-registered attacks - REFACTORED for NPC support
     */
    public static void tickAllActiveAttacks(MinecraftServer server) {
        if (selfTickingAttacks.isEmpty()) {
            return;
        }

        List<UUID> uuidsToClean = new ArrayList<>();

        for (var entry : selfTickingAttacks.entrySet()) {
            UUID entityUUID = entry.getKey();
            var attacks = entry.getValue();

            // Find the entity by UUID (could be player or NPC)
            LivingEntity entity = findEntityByUUID(server, entityUUID);

            if (entity == null || !entity.isAlive()) {
                uuidsToClean.add(entityUUID);
                continue;
            }

            List<AbstractDemonAttack<?, ?>> toRemove = new ArrayList<>();

            synchronized (attacks) {
                for (var attack : new ArrayList<>(attacks)) {
                    try {
                        if (attack.isActive()) {
                            attack.tick();
                        } else {
                            toRemove.add(attack);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error ticking demon attack: {}", e.getMessage(), e);
                        toRemove.add(attack);
                    }
                }

                // Remove inactive attacks
                attacks.removeAll(toRemove);
            }
        }

        // Clean up disconnected/dead entities
        for (UUID uuid : uuidsToClean) {
            selfTickingAttacks.remove(uuid);
        }
    }

    /**
     * Helper method to find a LivingEntity by UUID across all levels
     */
    private static LivingEntity findEntityByUUID(MinecraftServer server, UUID entityUUID) {
        for (var level : server.getAllLevels()) {
            var entity = level.getEntity(entityUUID);
            if (entity instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
        }
        return null;
    }

    /**
     * Clear all self-ticking attacks for a LivingEntity (on disconnect, death, etc.)
     * REFACTORED: Now accepts LivingEntity for NPC support
     */
    public static void clearSelfTickingAttacks(LivingEntity entity) {
        var attacks = selfTickingAttacks.remove(entity.getUUID());
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

    /**
     * Legacy Player version for backwards compatibility
     */
    public static void clearSelfTickingAttacks(Player player) {
        clearSelfTickingAttacks((LivingEntity) player);
    }

    // Blood restoration configuration methods
    public void setHitsForBlood(int hitsForBlood) {
        this.hitsForBlood = Math.max(1, hitsForBlood);
    }

    public void setMaxBloodPerAttack(int maxBloodPerAttack) {
        this.maxBloodPerAttack = Math.max(0, maxBloodPerAttack);
    }

    public void setBloodOnKill(int bloodOnKill) {
        this.bloodOnKill = Math.max(0, bloodOnKill);
    }

}