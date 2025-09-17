package com.xirc.nichirin.common.attack.moveset;

import com.xirc.nichirin.common.network.s2c.PlayerAnimationPacket;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * AbstractMoveset that works with any attack type - breathing techniques and demon arts
 * Flexible system supporting any number of moves with full configuration and followup system
 * Icons are handled by the MoveIcon system, not stored in move configs
 * Includes stun prevention system to prevent move stacking
 */
@Getter
public abstract class AbstractMoveset {

    // UUID for the movement speed modifier
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("A1B2C3D4-E5F6-7890-ABCD-EF1234567890");

    private final String movesetId;
    private final String displayName;
    private final MovesetType movesetType;

    // List of moves - flexible for any count
    protected final List<MoveConfiguration> moves = new ArrayList<>();

    // Optional moveset-wide properties
    @Nullable
    protected final ResourceLocation idleAnimation;

    // Modifiers
    protected final float speedMultiplier;
    protected final float fallDamageMultiplier;    // 0.5 = half damage, 0.0 = no damage
    protected final float healthRegenMultiplier;   // 2.0 = double regen rate
    // Static tracking for followup queues per player
    private static final java.util.concurrent.ConcurrentHashMap<UUID, FollowupQueue> playerFollowupQueues = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Followup queue state for a player
     */
    public static class FollowupQueue {
        private MoveConfiguration currentMove;
        private int currentFollowupIndex = -1;
        private boolean hasQueuedNext = false;
        private long attackStartTime = 0;
        private boolean canQueue = false;

        public void startAttack(MoveConfiguration move) {
            this.currentMove = move;
            this.currentFollowupIndex = -1;
            this.hasQueuedNext = false;
            this.attackStartTime = System.currentTimeMillis();
            this.canQueue = move.hasFollowups();
        }

        public void startFollowup(int followupIndex) {
            this.currentFollowupIndex = followupIndex;
            this.hasQueuedNext = false;
            this.attackStartTime = System.currentTimeMillis();
            this.canQueue = (followupIndex + 1) < currentMove.getFollowupCount();
        }

        public boolean canQueueNext() {
            return canQueue && !hasQueuedNext;
        }

        public void queueNext() {
            if (canQueueNext()) {
                hasQueuedNext = true;
            }
        }

        public boolean hasQueued() {
            return hasQueuedNext;
        }

        public int getNextFollowupIndex() {
            return currentFollowupIndex + 1;
        }

        public FollowupConfiguration getNextFollowup() {
            if (currentMove != null && hasQueuedNext) {
                return currentMove.getFollowup(getNextFollowupIndex());
            }
            return null;
        }

        public void clear() {
            currentMove = null;
            currentFollowupIndex = -1;
            hasQueuedNext = false;
            attackStartTime = 0;
            canQueue = false;
        }

        public boolean isAttackActive(long currentTime) {
            if (currentMove == null) return false;

            long duration;
            if (currentFollowupIndex == -1) {
                // Main attack
                duration = currentMove.getDurationOrDefault(0) * 50; // Convert ticks to ms
            } else {
                // Followup attack
                FollowupConfiguration followup = currentMove.getFollowup(currentFollowupIndex);
                duration = followup != null ? followup.getFollowupDurationOrDefault(0) * 50 : 0;
            }

            return (currentTime - attackStartTime) < duration;
        }
    }

    protected AbstractMoveset(String movesetId, String displayName, MovesetType movesetType, MovesetBuilder builder) {
        this.movesetId = movesetId;
        this.displayName = displayName;
        this.movesetType = movesetType;
        this.idleAnimation = builder.idleAnimation;
        this.speedMultiplier = builder.speedMultiplier;
        this.fallDamageMultiplier = builder.fallDamageMultiplier;
        this.healthRegenMultiplier = builder.healthRegenMultiplier;

        // Add all configured moves
        moves.addAll(builder.moveConfigs);
    }

    /**
     * Enum to distinguish between breathing and demon movesets
     */
    public enum MovesetType {
        BREATHING,
        DEMON
    }

    /**
     * Apply all moveset modifiers to a player
     */
    public void applyAllModifiers(Player player) {
        applySpeedModifier(player);
    }

    /**
     * Remove all moveset modifiers from a player
     */
    public void removeAllModifiers(Player player) {
        removeSpeedModifier(player);
    }

    /**
     * Apply the moveset's speed modifier to a player
     */
    public void applySpeedModifier(Player player) {
        if (speedMultiplier != 1.0f) {
            removeSpeedModifier(player);

            double modifierValue = speedMultiplier - 1.0;
            modifierValue = Math.max(-0.95, Math.min(modifierValue, 10.0));

            AttributeModifier modifier = new AttributeModifier(
                    SPEED_MODIFIER_UUID,
                    "moveset_speed_modifier",
                    modifierValue,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            );

            Objects.requireNonNull(player.getAttribute(Attributes.MOVEMENT_SPEED)).addTransientModifier(modifier);
        }
    }

    /**
     * Remove the moveset's speed modifier from a player
     */
    public void removeSpeedModifier(Player player) {
        Objects.requireNonNull(player.getAttribute(Attributes.MOVEMENT_SPEED)).removeModifier(SPEED_MODIFIER_UUID);
    }

    /**
     * Override the left-click (M1) behavior for SimpleKatana with stun checking
     */
    public boolean handleLeftClick(Player player) {
        if (player.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return true; // Block the move by overriding
        }
        return false;
    }

    /**
     * Override the right-click (M2) behavior for SimpleKatana with stun checking and followup queuing
     */
    public boolean handleRightClick(Player player, boolean isCrouching) {
        if (player.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            // Check if we should queue a followup
            FollowupQueue queue = playerFollowupQueues.get(player.getUUID());
            if (queue != null && queue.isAttackActive(System.currentTimeMillis()) && queue.canQueueNext()) {
                queue.queueNext();

                // Show feedback that followup was queued
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Followup queued!")
                                .withStyle(style -> style.withColor(0x55FF55)),
                        true
                );
            }
            return true; // Block the move by overriding
        }
        return false;
    }

    /**
     * Get the move index to use for right-click
     */
    public int getRightClickMoveIndex(boolean isCrouching) {
        return 0; // First move by default
    }

    /**
     * Called after a move is performed to allow post-move actions
     */
    public void onMovePerformed(Player player, int moveIndex, boolean isCrouching) {
        // Override in subclasses for special behavior
    }

    /**
     * Gets move by index (0-based)
     */
    @Nullable
    public MoveConfiguration getMove(int index) {
        if (index >= 0 && index < moves.size()) {
            return moves.get(index);
        }
        return null;
    }

    /**
     * Gets the number of moves in this moveset
     */
    public int getMoveCount() {
        return moves.size();
    }

    /**
     * Performs a move by index with automatic animation handling, stun prevention and followup queue initialization
     */
    public void performMove(Player player, int moveIndex) {
        System.out.println("[DEBUG] AbstractMoveset.performMove called for moveIndex: " + moveIndex);

        if (player.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            System.out.println("[DEBUG] Player is stunned, blocking move");
            return;
        }

        MoveConfiguration config = getMove(moveIndex);
        if (config != null) {
            System.out.println("[DEBUG] Move config found: " + config.getDisplayName() + " with " + config.getFollowupCount() + " followups");

            // AUTOMATIC ANIMATION HANDLING - Send animation packet if animation is configured
            if (config.animationId != null && player instanceof ServerPlayer serverPlayer) {
                String animationName = config.animationId.getPath();
                PlayerAnimationPacket packet = new PlayerAnimationPacket(serverPlayer.getId(), animationName);
                NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
                System.out.println("[DEBUG] Sent animation packet for: " + animationName);
            }

            // Initialize followup queue for this attack
            if (config.hasFollowups()) {
                System.out.println("[DEBUG] Initializing followup queue for attack with " + config.getFollowupCount() + " followups");
                FollowupQueue queue = new FollowupQueue();
                queue.startAttack(config);
                playerFollowupQueues.put(player.getUUID(), queue);

                // Schedule followup check after attack duration
                int duration = config.getDurationOrDefault(0);
                System.out.println("[DEBUG] Scheduling followup check after " + duration + " ticks");
                scheduleFollowupCheck(player, duration);
            }

            if (config.startAction != null) {
                System.out.println("[DEBUG] Executing move start action");
                config.startAction.accept(player);
            } else {
                System.out.println("[DEBUG] No start action defined for this move");
            }
        } else {
            System.out.println("[DEBUG] No move config found for index: " + moveIndex);
        }
    }

    /**
     * Schedules a followup check after the specified duration
     */
    private void scheduleFollowupCheck(Player player, int durationTicks) {
        // Convert ticks to milliseconds and schedule
        long delayMs = durationTicks * 50L; // 20 ticks = 1000ms

        // Use a simple delay mechanism (you might want to integrate with your mod's tick system)
        java.util.concurrent.CompletableFuture.delayedExecutor(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                .execute(() -> checkAndExecuteFollowup(player));
    }

    /**
     * Checks if a followup should be executed and executes it
     */
    private void checkAndExecuteFollowup(Player player) {
        FollowupQueue queue = playerFollowupQueues.get(player.getUUID());
        if (queue == null || !queue.hasQueued()) {
            // No followup queued, clear the queue
            if (queue != null) {
                queue.clear();
                playerFollowupQueues.remove(player.getUUID());
            }
            return;
        }

        FollowupConfiguration followup = queue.getNextFollowup();
        if (followup != null) {
            // Execute the followup
            executeFollowup(player, followup, queue);
        } else {
            // Clear queue if no valid followup
            queue.clear();
            playerFollowupQueues.remove(player.getUUID());
        }
    }

    /**
     * Executes a followup attack with automatic animation handling
     */
    private void executeFollowup(Player player, FollowupConfiguration followup, FollowupQueue queue) {
        // Update queue state
        queue.startFollowup(queue.getNextFollowupIndex());

        // AUTOMATIC ANIMATION HANDLING FOR FOLLOWUPS - Send animation packet if configured
        if (followup.followupAnimationId != null && player instanceof ServerPlayer serverPlayer) {
            String animationName = followup.followupAnimationId.getPath();
            PlayerAnimationPacket packet = new PlayerAnimationPacket(serverPlayer.getId(), animationName);
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
            System.out.println("[DEBUG] Sent followup animation packet for: " + animationName);
        }

        // Execute followup action (no windup - immediate execution)
        if (followup.followupAction != null) {
            followup.followupAction.accept(player);
        }

        // Schedule next followup check if more followups available
        int followupDuration = followup.getFollowupDurationOrDefault(0);
        if (followupDuration > 0 && queue.canQueue) {
            scheduleFollowupCheck(player, followupDuration);
        } else {
            // No more followups possible, clear queue after duration
            java.util.concurrent.CompletableFuture.delayedExecutor(followupDuration * 50L, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        queue.clear();
                        playerFollowupQueues.remove(player.getUUID());
                    });
        }
    }

    /**
     * Clean up followup queues when player disconnects
     */
    public static void cleanupPlayer(Player player) {
        playerFollowupQueues.remove(player.getUUID());
    }

    /**
     * Force clear all followup queues (for debugging or resets)
     */
    public static void clearAllFollowupQueues() {
        playerFollowupQueues.clear();
    }

    /**
     * Apply stun effect for a move configuration
     */
    protected void applyMoveStun(Player player, MoveConfiguration config) {
        int windupTicks = config.getWindupOrDefault(0);
        int durationTicks = config.getDurationOrDefault(0);
        int totalStunTicks = windupTicks + durationTicks;

        if (totalStunTicks > 0) {
            MobEffectInstance stunEffect = new MobEffectInstance(
                    NichirinEffectRegistry.STUNNED.get(),
                    totalStunTicks,
                    0,
                    false,
                    false,
                    false
            );
            player.addEffect(stunEffect);
        }
    }

    /**
     * Check if the player can perform moves (not stunned)
     */
    public boolean canPerformMoves(Player player) {
        return !player.hasEffect(NichirinEffectRegistry.STUNNED.get());
    }

    /**
     * Get the total stun duration for a move (windup + duration)
     */
    public int getMoveStunDuration(int moveIndex) {
        MoveConfiguration config = getMove(moveIndex);
        if (config != null) {
            return config.getWindupOrDefault(0) + config.getDurationOrDefault(0);
        }
        return 0;
    }

    /**
     * Check if this is a breathing technique moveset
     */
    public boolean isBreathingMoveset() {
        return movesetType == MovesetType.BREATHING;
    }

    /**
     * Check if this is a demon art moveset
     */
    public boolean isDemonMoveset() {
        return movesetType == MovesetType.DEMON;
    }

    /**
     * Get the name of the right-click move for cooldown display
     */
    public String getRightClickMoveName() {
        return "Special Move";
    }

    /**
     * Get the name of the crouch right-click move for cooldown display
     */
    public String getCrouchRightClickMoveName() {
        return "Crouch Special Move";
    }

    /**
     * Complete configuration for a moveset move with followup system
     */
    @Getter
    public static class MoveConfiguration {
        // Basic properties
        public final String moveId;
        public final String displayName;
        public final Consumer<Player> startAction;
        public final ResourceLocation animationId;
        public final int animationPriority;

        // Combat Stats
        public final Float damage;
        public final Float range;
        public final Float knockback;
        public final Integer hitStun;
        public final Float hitboxSize;

        // Timing
        public final Integer cooldown;
        public final Integer windup;
        public final Integer duration;
        public final Integer activeFrames;
        public final Integer recovery;

        // Resources
        public final Float breathCost;
        public final Float staminaCost;

        // Movement
        public final Float teleportDistance;
        public final Float dashSpeed;
        public final Integer teleportWindup;

        // Followup system
        public final List<FollowupConfiguration> followups;

        private MoveConfiguration(MoveBuilder builder) {
            this.moveId = builder.moveId;
            this.displayName = builder.displayName;
            this.startAction = builder.startAction;
            this.animationId = builder.animationId;
            this.animationPriority = builder.animationPriority;

            this.damage = builder.damage;
            this.range = builder.range;
            this.knockback = builder.knockback;
            this.hitStun = builder.hitStun;
            this.hitboxSize = builder.hitboxSize;

            this.cooldown = builder.cooldown;
            this.windup = builder.windup;
            this.duration = builder.duration;
            this.activeFrames = builder.activeFrames;
            this.recovery = builder.recovery;

            this.breathCost = builder.breathCost;
            this.staminaCost = builder.staminaCost;

            this.teleportDistance = builder.teleportDistance;
            this.dashSpeed = builder.dashSpeed;
            this.teleportWindup = builder.teleportWindup;

            this.followups = builder.followups != null ? new ArrayList<>(builder.followups) : new ArrayList<>();
        }

        // Convenience methods for checking if properties are configured
        public boolean hasDamage() { return damage != null; }
        public boolean hasRange() { return range != null; }
        public boolean hasKnockback() { return knockback != null; }
        public boolean hasHitStun() { return hitStun != null; }
        public boolean hasHitboxSize() { return hitboxSize != null; }
        public boolean hasCooldown() { return cooldown != null; }
        public boolean hasWindup() { return windup != null; }
        public boolean hasDuration() { return duration != null; }
        public boolean hasActiveFrames() { return activeFrames != null; }
        public boolean hasRecovery() { return recovery != null; }
        public boolean hasBreathCost() { return breathCost != null; }
        public boolean hasStaminaCost() { return staminaCost != null; }
        public boolean hasTeleportDistance() { return teleportDistance != null; }
        public boolean hasDashSpeed() { return dashSpeed != null; }
        public boolean hasTeleportWindup() { return teleportWindup != null; }

        // Safe getters with fallbacks
        public float getDamageOrDefault(float defaultValue) { return damage != null ? damage : defaultValue; }
        public float getRangeOrDefault(float defaultValue) { return range != null ? range : defaultValue; }
        public float getKnockbackOrDefault(float defaultValue) { return knockback != null ? knockback : defaultValue; }
        public int getHitStunOrDefault(int defaultValue) { return hitStun != null ? hitStun : defaultValue; }
        public float getHitboxSizeOrDefault(float defaultValue) { return hitboxSize != null ? hitboxSize : defaultValue; }
        public int getCooldownOrDefault(int defaultValue) { return cooldown != null ? cooldown : defaultValue; }
        public int getWindupOrDefault(int defaultValue) { return windup != null ? windup : defaultValue; }
        public int getDurationOrDefault(int defaultValue) { return duration != null ? duration : defaultValue; }
        public int getActiveFramesOrDefault(int defaultValue) { return activeFrames != null ? activeFrames : defaultValue; }
        public int getRecoveryOrDefault(int defaultValue) { return recovery != null ? recovery : defaultValue; }
        public float getBreathCostOrDefault(float defaultValue) { return breathCost != null ? breathCost : defaultValue; }
        public float getStaminaCostOrDefault(float defaultValue) { return staminaCost != null ? staminaCost : defaultValue; }
        public float getTeleportDistanceOrDefault(float defaultValue) { return teleportDistance != null ? teleportDistance : defaultValue; }
        public float getDashSpeedOrDefault(float defaultValue) { return dashSpeed != null ? dashSpeed : defaultValue; }
        public int getTeleportWindupOrDefault(int defaultValue) { return teleportWindup != null ? teleportWindup : defaultValue; }

        public int getStunDuration() {
            return getWindupOrDefault(0);
        }

        public boolean causesStun() {
            return getStunDuration() > 0;
        }

        public boolean isResourceFree() {
            return !hasBreathCost() && !hasStaminaCost();
        }

        // Followup methods
        public boolean hasFollowups() {
            return followups != null && !followups.isEmpty();
        }

        public FollowupConfiguration getFollowup(int index) {
            if (followups != null && index >= 0 && index < followups.size()) {
                return followups.get(index);
            }
            return null;
        }

        public int getFollowupCount() {
            return followups != null ? followups.size() : 0;
        }
    }

    /**
     * Configuration for followup attacks (chaining moves)
     */
    @Getter
    public static class FollowupConfiguration {
        public final String followupMoveId;
        public final String followupDisplayName;
        public final Consumer<Player> followupAction;
        public final ResourceLocation followupAnimationId;
        public final int followupAnimationPriority;

        public final Float followupDamage;
        public final Float followupRange;
        public final Float followupKnockback;
        public final Integer followupHitStun;
        public final Float followupHitboxSize;

        public final Integer followupCooldown;
        public final Integer followupWindup;
        public final Integer followupDuration;

        public final Float followupBreathCost;
        public final Float followupStaminaCost;

        public final Float followupTeleportDistance;
        public final Float followupDashSpeed;
        public final Integer followupTeleportWindup;

        private FollowupConfiguration(FollowupBuilder builder) {
            this.followupMoveId = builder.followupMoveId;
            this.followupDisplayName = builder.followupDisplayName;
            this.followupAction = builder.followupAction;
            this.followupAnimationId = builder.followupAnimationId;
            this.followupAnimationPriority = builder.followupAnimationPriority;

            this.followupDamage = builder.followupDamage;
            this.followupRange = builder.followupRange;
            this.followupKnockback = builder.followupKnockback;
            this.followupHitStun = builder.followupHitStun;
            this.followupHitboxSize = builder.followupHitboxSize;

            this.followupCooldown = builder.followupCooldown;
            this.followupWindup = builder.followupWindup;
            this.followupDuration = builder.followupDuration;

            this.followupBreathCost = builder.followupBreathCost;
            this.followupStaminaCost = builder.followupStaminaCost;

            this.followupTeleportDistance = builder.followupTeleportDistance;
            this.followupDashSpeed = builder.followupDashSpeed;
            this.followupTeleportWindup = builder.followupTeleportWindup;
        }

        // Safe getters for followup
        public float getFollowupDamageOrDefault(float defaultValue) { return followupDamage != null ? followupDamage : defaultValue; }
        public float getFollowupRangeOrDefault(float defaultValue) { return followupRange != null ? followupRange : defaultValue; }
        public float getFollowupKnockbackOrDefault(float defaultValue) { return followupKnockback != null ? followupKnockback : defaultValue; }
        public int getFollowupHitStunOrDefault(int defaultValue) { return followupHitStun != null ? followupHitStun : defaultValue; }
        public float getFollowupHitboxSizeOrDefault(float defaultValue) { return followupHitboxSize != null ? followupHitboxSize : defaultValue; }
        public int getFollowupCooldownOrDefault(int defaultValue) { return followupCooldown != null ? followupCooldown : defaultValue; }
        public int getFollowupWindupOrDefault(int defaultValue) { return followupWindup != null ? followupWindup : defaultValue; }
        public int getFollowupDurationOrDefault(int defaultValue) { return followupDuration != null ? followupDuration : defaultValue; }
        public float getFollowupBreathCostOrDefault(float defaultValue) { return followupBreathCost != null ? followupBreathCost : defaultValue; }
        public float getFollowupStaminaCostOrDefault(float defaultValue) { return followupStaminaCost != null ? followupStaminaCost : defaultValue; }
    }

    /**
     * Builder for followup configurations
     */
    public static class FollowupBuilder {
        private final String followupMoveId;
        private final String followupDisplayName;

        private Consumer<Player> followupAction;
        private ResourceLocation followupAnimationId;
        private int followupAnimationPriority = 0;

        private Float followupDamage;
        private Float followupRange;
        private Float followupKnockback;
        private Integer followupHitStun;
        private Float followupHitboxSize;

        private Integer followupCooldown;
        private Integer followupWindup;
        private Integer followupDuration;

        private Float followupBreathCost;
        private Float followupStaminaCost;

        private Float followupTeleportDistance;
        private Float followupDashSpeed;
        private Integer followupTeleportWindup;

        public FollowupBuilder(String followupMoveId, String followupDisplayName) {
            this.followupMoveId = followupMoveId;
            this.followupDisplayName = followupDisplayName;
        }

        public FollowupBuilder withAction(Consumer<Player> action) {
            this.followupAction = action;
            return this;
        }

        public FollowupBuilder withAnimation(String animationId, int priority) {
            this.followupAnimationId = new ResourceLocation(animationId);
            this.followupAnimationPriority = priority;
            return this;
        }

        public FollowupBuilder withDamage(float damage) {
            this.followupDamage = damage;
            return this;
        }

        public FollowupBuilder withRange(float range) {
            this.followupRange = range;
            return this;
        }

        public FollowupBuilder withKnockback(float knockback) {
            this.followupKnockback = knockback;
            return this;
        }

        public FollowupBuilder withHitStun(int hitStun) {
            this.followupHitStun = hitStun;
            return this;
        }

        public FollowupBuilder withHitboxSize(float hitboxSize) {
            this.followupHitboxSize = hitboxSize;
            return this;
        }

        public FollowupBuilder withTiming(int cooldown, int windup, int duration) {
            this.followupCooldown = cooldown;
            this.followupWindup = windup;
            this.followupDuration = duration;
            return this;
        }

        public FollowupBuilder withBreathCost(float breathCost) {
            this.followupBreathCost = breathCost;
            return this;
        }

        public FollowupBuilder withStaminaCost(float staminaCost) {
            this.followupStaminaCost = staminaCost;
            return this;
        }

        public FollowupBuilder withDashSpeed(float dashSpeed) {
            this.followupDashSpeed = dashSpeed;
            return this;
        }

        public FollowupConfiguration build() {
            return new FollowupConfiguration(this);
        }
    }

    /**
     * Builder for individual moves
     */
    public static class MoveBuilder {
        private final String moveId;
        private final String displayName;

        private Consumer<Player> startAction;
        private ResourceLocation animationId;
        private int animationPriority = 0;

        private Float damage;
        private Float range;
        private Float knockback;
        private Integer hitStun;
        private Float hitboxSize;

        private Integer cooldown;
        private Integer windup;
        private Integer duration;
        private Integer activeFrames;
        private Integer recovery;

        private Float breathCost;
        private Float staminaCost;

        private Float teleportDistance;
        private Float dashSpeed;
        private Integer teleportWindup;

        private List<FollowupConfiguration> followups;

        public MoveBuilder(String moveId, String displayName) {
            this.moveId = moveId;
            this.displayName = displayName;
        }

        public MoveBuilder withAction(Consumer<Player> action) {
            this.startAction = action;
            return this;
        }

        public MoveBuilder withAnimation(String animationId, int priority) {
            this.animationId = new ResourceLocation(animationId);
            this.animationPriority = priority;
            return this;
        }

        public MoveBuilder withDamage(float damage) {
            this.damage = damage;
            return this;
        }

        public MoveBuilder withRange(float range) {
            this.range = range;
            return this;
        }

        public MoveBuilder withKnockback(float knockback) {
            this.knockback = knockback;
            return this;
        }

        public MoveBuilder withHitStun(int hitStun) {
            this.hitStun = hitStun;
            return this;
        }

        public MoveBuilder withHitboxSize(float hitboxSize) {
            this.hitboxSize = hitboxSize;
            return this;
        }

        public MoveBuilder withCooldown(int cooldown) {
            this.cooldown = cooldown;
            return this;
        }

        public MoveBuilder withWindup(int windup) {
            this.windup = windup;
            return this;
        }

        public MoveBuilder withDuration(int duration) {
            this.duration = duration;
            return this;
        }

        public MoveBuilder withActiveFrames(int activeFrames) {
            this.activeFrames = activeFrames;
            return this;
        }

        public MoveBuilder withRecovery(int recovery) {
            this.recovery = recovery;
            return this;
        }

        public MoveBuilder withTiming(int cooldown, int windup, int duration) {
            this.cooldown = cooldown;
            this.windup = windup;
            this.duration = duration;
            return this;
        }

        public MoveBuilder withBreathCost(float breathCost) {
            this.breathCost = breathCost;
            return this;
        }

        public MoveBuilder withStaminaCost(float staminaCost) {
            this.staminaCost = staminaCost;
            return this;
        }

        public MoveBuilder withTeleportDistance(float teleportDistance) {
            this.teleportDistance = teleportDistance;
            return this;
        }

        public MoveBuilder withDashSpeed(float dashSpeed) {
            this.dashSpeed = dashSpeed;
            return this;
        }

        public MoveBuilder withTeleportWindup(int teleportWindup) {
            this.teleportWindup = teleportWindup;
            return this;
        }

        public MoveBuilder withTeleport(float distance, int windup) {
            this.teleportDistance = distance;
            this.teleportWindup = windup;
            return this;
        }

        // Followup system
        public MoveBuilder withFollowup(FollowupBuilder followupBuilder) {
            if (this.followups == null) {
                this.followups = new ArrayList<>();
            }
            this.followups.add(followupBuilder.build());
            return this;
        }

        public MoveBuilder withFollowups(FollowupBuilder... followupBuilders) {
            if (this.followups == null) {
                this.followups = new ArrayList<>();
            }
            for (FollowupBuilder builder : followupBuilders) {
                this.followups.add(builder.build());
            }
            return this;
        }

        public MoveConfiguration build() {
            return new MoveConfiguration(this);
        }
    }

    /**
     * Builder for creating movesets
     */
    public static class MovesetBuilder {
        private ResourceLocation idleAnimation;
        private float speedMultiplier = 1.0f;
        private float fallDamageMultiplier = 1.0f;
        private float healthRegenMultiplier = 1.0f;
        private float staminaCostMultiplier = 1.0f;

        final List<MoveConfiguration> moveConfigs = new ArrayList<>();

        public MovesetBuilder withIdleAnimation(String idleAnimation) {
            this.idleAnimation = new ResourceLocation(idleAnimation);
            return this;
        }

        public MovesetBuilder withSpeedMultiplier(float multiplier) {
            this.speedMultiplier = multiplier;
            return this;
        }

        public MovesetBuilder withFallDamageMultiplier(float multiplier) {
            this.fallDamageMultiplier = multiplier;
            return this;
        }

        public MovesetBuilder withHealthRegenMultiplier(float multiplier) {
            this.healthRegenMultiplier = multiplier;
            return this;
        }

        public MovesetBuilder withStaminaCostMultiplier(float multiplier) {
            this.staminaCostMultiplier = multiplier;
            return this;
        }

        public MovesetBuilder withMove(MoveBuilder moveBuilder) {
            this.moveConfigs.add(moveBuilder.build());
            return this;
        }
    }
}