package com.xirc.nichirin.common.attack.moveset;

import com.xirc.nichirin.common.entity.MovesetCapableNPC;
import com.xirc.nichirin.common.network.s2c.PlayerAnimationPacket;
import com.xirc.nichirin.common.util.EntityResources;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * AbstractMoveset that works with ANY LivingEntity - players AND NPCs
 * Flexible system supporting any number of moves with full configuration and followup system
 * Icons are handled by the MoveIcon system, not stored in move configs
 * Includes stun prevention system to prevent move stacking
 * Supports custom left-click attacks.
 */
@Getter
public abstract class  AbstractMoveset {

    private static final ResourceLocation SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("nichirin", "moveset_speed_modifier");

    // Simple getters for AbstractMoveset fields (Lombok @Getter not working)
    private final String movesetId;
    private final String displayName;
    private final MovesetType movesetType;

    // List of moves - flexible for any count
    protected final List<MoveConfiguration> moves = new ArrayList<>();

    // Click moves
    protected final MoveConfiguration leftClickMove;
    protected final MoveConfiguration rightClickMove;
    protected final MoveConfiguration crouchRightClickMove;

    // Static storage for captured configurations per moveset type
    private static final Map<String, MoveConfiguration> capturedLeftClickConfigs = new HashMap<>();
    private static final Map<String, MoveConfiguration> capturedRightClickConfigs = new HashMap<>();
    private static final Map<String, MoveConfiguration> capturedCrouchRightClickConfigs = new HashMap<>();

    // Optional moveset-wide properties
    @Nullable
    protected final ResourceLocation idleAnimation;

    // Modifiers
    protected final float speedMultiplier;
    protected final float fallDamageMultiplier;
    protected final float healthRegenMultiplier;

    // Static tracking for followup queues per entity
    private static final ConcurrentHashMap<UUID, FollowupQueue> entityFollowupQueues = new ConcurrentHashMap<>();

    /**
     * Followup queue state for an entity
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

        /** The move this queue is tracking, or null if the queue has been cleared. */
        @Nullable
        public MoveConfiguration getCurrentMove() {
            return currentMove;
        }

        public boolean isAttackActive(long currentTime) {
            if (currentMove == null) return false;

            long duration;
            if (currentFollowupIndex == -1) {
                duration = currentMove.getDurationOrDefault(0) * 50;
            } else {
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
        this.leftClickMove = builder.leftClickMove;
        this.rightClickMove = builder.rightClickMove;
        this.crouchRightClickMove = builder.crouchRightClickMove;

        moves.addAll(builder.moveConfigs);
    }

    /**
     * Distinguishes moveset resource type: breathing styles use breath gauge,
     * demon arts use blood points, neutral uses neither.
     */
    public enum MovesetType {
        BREATHING,
        DEMON,
        NEUTRAL
    }

    /**
     * Apply all moveset modifiers to an entity
     */
    public void applyAllModifiers(LivingEntity entity) {
        applySpeedModifier(entity);
    }

    /**
     * Remove all moveset modifiers from an entity
     */
    public void removeAllModifiers(LivingEntity entity) {
        removeSpeedModifier(entity);
    }

    /**
     * Apply the moveset's speed modifier to an entity
     */
    public void applySpeedModifier(LivingEntity entity) {
        if (speedMultiplier != 1.0f) {
            removeSpeedModifier(entity);

            double modifierValue = speedMultiplier - 1.0;
            modifierValue = Math.max(-0.95, Math.min(modifierValue, 10.0));

            AttributeModifier modifier = new AttributeModifier(
                    SPEED_MODIFIER_ID,
                    modifierValue,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );

            Objects.requireNonNull(entity.getAttribute(Attributes.MOVEMENT_SPEED)).addTransientModifier(modifier);
        }
    }

    /**
     * Remove the moveset's speed modifier from an entity
     */
    public void removeSpeedModifier(LivingEntity entity) {
        Objects.requireNonNull(entity.getAttribute(Attributes.MOVEMENT_SPEED)).removeModifier(SPEED_MODIFIER_ID);
    }

    /**
     * Override the left-click (M1) behavior - works for both Player and NPC
     */
    public boolean handleLeftClick(LivingEntity entity) {
        if (entity.hasEffect(NichirinEffectRegistry.stunned())) {
            return true;
        }

        if (leftClickMove != null && leftClickMove.startAction != null) {
            if (!hasResourcesForMove(entity, leftClickMove)) return true;
            if (shouldAutoStunClickMoves()) applyMoveStun(entity, leftClickMove);

            // AUTOMATIC ANIMATION HANDLING - Player or NPC
            if (leftClickMove.animationId != null) {
                triggerAnimation(entity, leftClickMove.animationId.getPath());
            }

            // Executing a new move always breaks any pending followup chain from a different move.
            resetFollowupQueueIfDifferent(entity, leftClickMove);

            // Initialize followup queue
            if (leftClickMove.hasFollowups()) {
                FollowupQueue queue = new FollowupQueue();
                queue.startAttack(leftClickMove);
                entityFollowupQueues.put(entity.getUUID(), queue);

                int duration = leftClickMove.getDurationOrDefault(0);
                scheduleFollowupCheck(entity, duration);
            }

            // Execute the move action
            leftClickMove.startAction.accept(entity);

            return true;
        }

        return false; // No configured move, let subclass handle it
    }

    /**
     * Override the right-click (M2) behavior for SimpleKatana with stun checking and followup queuing
     */
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (entity.hasEffect(NichirinEffectRegistry.stunned())) {
            // Check if we should queue a followup
            FollowupQueue queue = entityFollowupQueues.get(entity.getUUID());
            if (queue != null && queue.isAttackActive(System.currentTimeMillis()) && queue.canQueueNext()) {
                queue.queueNext();

                EntityResources.sendMessage(entity,
                        Component.literal("Followup queued!").withStyle(style -> style.withColor(0x55FF55)), true);
            }
            return true; // Block the move by overriding
        }

        // NOT STUNNED - Try to execute configured move if available
        MoveConfiguration config = isCrouching ? crouchRightClickMove : rightClickMove;

        if (config != null && config.startAction != null) {
            if (!hasResourcesForMove(entity, config)) return true;
            if (shouldAutoStunClickMoves()) applyMoveStun(entity, config);

            // AUTOMATIC ANIMATION HANDLING
            if (config.animationId != null) {
                triggerAnimation(entity, config.animationId.getPath());
            }

            // Executing a new move always breaks any pending followup chain from a different move.
            resetFollowupQueueIfDifferent(entity, config);

            // Initialize followup queue
            if (config.hasFollowups()) {
                FollowupQueue queue = new FollowupQueue();
                queue.startAttack(config);
                entityFollowupQueues.put(entity.getUUID(), queue);

                int duration = config.getDurationOrDefault(0);
                scheduleFollowupCheck(entity, duration);
            }

            // Execute the move action
            config.startAction.accept(entity);
            return true;
        }

        return false; // No configured move, let subclass handle it
    }

    /**
     * Trigger animation - handles both Player (PlayerAnimationPacket) and NPC (Azure)
     */
    public void triggerAnimation(LivingEntity entity, String animationName) {
        if (entity instanceof ServerPlayer serverPlayer) {
            // Player animation — broadcast to all players in the same level so others can see it
            PlayerAnimationPacket packet = new PlayerAnimationPacket(serverPlayer.getId(), animationName);
            NichirinPacketRegistry.broadcastPlayerAnimation(serverPlayer, packet);
        } else if (entity instanceof MovesetCapableNPC npc) {
            // NPC animation using Azure
            npc.triggerMovesetAnimation(animationName);
        }
    }

    /**
     * Capture a move configuration for later retrieval
     */
    public void captureLeftClickConfig(MoveConfiguration config) {
        capturedLeftClickConfigs.put(this.movesetId, config);
    }

    public void captureRightClickConfig(MoveConfiguration config, boolean isCrouch) {
        String key = this.movesetId;
        if (isCrouch) {
            capturedCrouchRightClickConfigs.put(key, config);
        } else {
            capturedRightClickConfigs.put(key, config);
        }
    }

    /**
     * Get the move index to use for left click
     */
    public int getLeftClickMoveIndex() {
        return -3;
    }

    /**
     * Get the move index to use for right-click
     */
    public int getRightClickMoveIndex(boolean isCrouching) {
        return isCrouching ? -2 : -1;
    }

    /**
     * Called after a move is performed
     */
    public void onMovePerformed(LivingEntity entity, int moveIndex, boolean isCrouching) {
        // Override in subclasses
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
     * How many ticks an animation should occupy, derived from the matching move's configured
     * windup+duration. NPCs use this to time client-side animation playback instead of
     * hardcoded per-animation tables. Falls back to {@code fallback} for animations that aren't
     * moveset moves (dash, backstep, double jump, etc.).
     */
    public int getAnimationDurationTicks(String animationName, int fallback) {
        if (animationName == null || animationName.isEmpty()) return fallback;
        for (int i = 0; i < getMoveCount(); i++) {
            int d = animMatchDuration(getMove(i), animationName);
            if (d >= 0) return d;
        }
        int d;
        if ((d = animMatchDuration(getLeftClickConfiguration(), animationName)) >= 0) return d;
        if ((d = animMatchDuration(getRightClickConfiguration(), animationName)) >= 0) return d;
        if ((d = animMatchDuration(getCrouchRightClickConfiguration(), animationName)) >= 0) return d;
        return fallback;
    }

    private int animMatchDuration(MoveConfiguration cfg, String animationName) {
        if (cfg == null || cfg.animationId == null) return -1;
        if (!cfg.animationId.getPath().equals(animationName)) return -1;
        return cfg.getWindupOrDefault(0) + cfg.getDurationOrDefault(0);
    }

    /**
     * Gets the number of moves in this moveset
     */
    public int getMoveCount() {
        return moves.size();
    }

    /**
     * Performs a move by index with automatic animation handling - WORKS FOR ENTITIES
     */
    public void performMove(LivingEntity entity, int moveIndex) {
        if (entity.hasEffect(NichirinEffectRegistry.stunned())) {
            return;
        }

        MoveConfiguration config = getMove(moveIndex);
        if (config != null) {
            if (!hasResourcesForMove(entity, config)) return;
            // AUTOMATIC ANIMATION HANDLING
            if (config.animationId != null) {
                triggerAnimation(entity, config.animationId.getPath());
            }

            // Executing a new move always breaks any pending followup chain from a different move.
            resetFollowupQueueIfDifferent(entity, config);

            // Initialize followup queue
            if (config.hasFollowups()) {
                FollowupQueue queue = new FollowupQueue();
                queue.startAttack(config);
                entityFollowupQueues.put(entity.getUUID(), queue);

                int duration = config.getDurationOrDefault(0);
                scheduleFollowupCheck(entity, duration);
            }

            if (config.startAction != null) {
                config.startAction.accept(entity);
            }
        }
    }

    /**
     * Clears any existing followup queue if it's tracking a different move than {@code newMove}.
     * Called at the start of every move so executing a different move always breaks the chain.
     * (The "queue next followup" stunned-input path is unaffected — it doesn't go through here.)
     */
    private void resetFollowupQueueIfDifferent(LivingEntity entity, MoveConfiguration newMove) {
        FollowupQueue existing = entityFollowupQueues.get(entity.getUUID());
        if (existing != null && existing.getCurrentMove() != newMove) {
            existing.clear();
            entityFollowupQueues.remove(entity.getUUID());
        }
    }

    /**
     * Schedules a followup check
     */
    private void scheduleFollowupCheck(LivingEntity entity, int durationTicks) {
        long delayMs = durationTicks * 50L;

        CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS)
                .execute(() -> checkAndExecuteFollowup(entity));
    }

    /**
     * Checks if a followup should be executed
     */
    private void checkAndExecuteFollowup(LivingEntity entity) {
        FollowupQueue queue = entityFollowupQueues.get(entity.getUUID());
        if (queue == null || !queue.hasQueued()) {
            if (queue != null) {
                queue.clear();
                entityFollowupQueues.remove(entity.getUUID());
            }
            return;
        }

        FollowupConfiguration followup = queue.getNextFollowup();
        if (followup != null) {
            executeFollowup(entity, followup, queue);
        } else {
            queue.clear();
            entityFollowupQueues.remove(entity.getUUID());
        }
    }

    /**
     * Executes a followup attack with automatic animation handling
     */
    private void executeFollowup(LivingEntity entity, FollowupConfiguration followup, FollowupQueue queue) {
        queue.startFollowup(queue.getNextFollowupIndex());

        // AUTOMATIC ANIMATION HANDLING FOR FOLLOWUPS
        if (followup.followupAnimationId != null) {
            triggerAnimation(entity, followup.followupAnimationId.getPath());
        }

        if (followup.followupAction != null) {
            followup.followupAction.accept(entity);
        }

        int followupDuration = followup.getFollowupDurationOrDefault(0);
        if (followupDuration > 0 && queue.canQueue) {
            scheduleFollowupCheck(entity, followupDuration);
        } else {
            CompletableFuture.delayedExecutor(followupDuration * 50L, TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        queue.clear();
                        entityFollowupQueues.remove(entity.getUUID());
                    });
        }
    }

    /**
     * Clean up followup queues when entity is removed
     */
    public static void cleanupEntity(LivingEntity entity) {
        entityFollowupQueues.remove(entity.getUUID());
    }

    /**
     * Force clear all followup queues
     */
    public static void clearAllFollowupQueues() {
        entityFollowupQueues.clear();
    }

    /**
     * Apply stun effect for a move configuration
     */
    /**
     * Whether the default {@link #handleLeftClick}/{@link #handleRightClick} should auto-apply
     * the STUNNED effect for the move's windup+duration before running its action.
     * <p>Defaults to {@code true} — the historical behavior. Breathing movesets override to
     * {@code false} because their attack classes manage their own pacing (the original
     * pre-refactor handlers never applied the stun, so opting out preserves that feel).</p>
     */
    protected boolean shouldAutoStunClickMoves() {
        return true;
    }

    protected void applyMoveStun(LivingEntity entity, MoveConfiguration config) {
        int windupTicks = config.getWindupOrDefault(0);
        int durationTicks = config.getDurationOrDefault(0);
        int totalStunTicks = windupTicks + durationTicks;

        if (totalStunTicks > 0) {
            MobEffectInstance stunEffect = new MobEffectInstance(
                    NichirinEffectRegistry.stunned(),
                    totalStunTicks,
                    0,
                    false,
                    false,
                    false
            );
            entity.addEffect(stunEffect);
        }
    }

    /**
     * Check if the entity can perform moves (not stunned)
     */
    public boolean canPerformMoves(LivingEntity entity) {
        return !entity.hasEffect(NichirinEffectRegistry.stunned());
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

    public boolean isNeutralMoveset() {
        return movesetType == MovesetType.NEUTRAL;
    }

    /**
     * Get the name of the left-click move for cooldown display
     */
    public String getLeftClickMoveName() {
        MoveConfiguration config = getLeftClickConfiguration();
        if (config != null) return config.getDisplayName();
        return leftClickMove != null ? leftClickMove.getDisplayName() : "Basic Attack";
    }

    /**
     * Get the name of the right-click move for cooldown display
     */
    public String getRightClickMoveName() {
        MoveConfiguration config = getRightClickConfiguration();
        if (config != null) return config.getDisplayName();
        return rightClickMove != null ? rightClickMove.getDisplayName() : "Special Move";
    }

    /**
     * Get the name of the crouch right-click move for cooldown display
     */
    public String getCrouchRightClickMoveName() {
        MoveConfiguration config = getCrouchRightClickConfiguration();
        if (config != null) return config.getDisplayName();
        return crouchRightClickMove != null ? crouchRightClickMove.getDisplayName() : "Crouch Special Move";
    }

    /**
     * Get description for left-click move
     */
    public String getLeftClickDescription() {
        MoveConfiguration config = getLeftClickConfiguration();
        if (config != null) return config.getDescription();
        return leftClickMove != null ? leftClickMove.getDescription() : null;
    }

    /**
     * Get description for right-click move
     */
    public String getRightClickDescription() {
        MoveConfiguration config = getRightClickConfiguration();
        if (config != null) return config.getDescription();
        return rightClickMove != null ? rightClickMove.getDescription() : null;
    }

    /**
     * Get description for crouch right-click move
     */
    public String getCrouchRightClickDescription() {
        MoveConfiguration config = getCrouchRightClickConfiguration();
        if (config != null) return config.getDescription();
        return crouchRightClickMove != null ? crouchRightClickMove.getDescription() : null;
    }

    /**
     * Get the configuration for left-click move
     */
    public MoveConfiguration getLeftClickConfiguration() {
        MoveConfiguration captured = capturedLeftClickConfigs.get(this.movesetId);
        return captured != null ? captured : leftClickMove;
    }

    /**
     * Get the configuration for right-click move
     */
    public MoveConfiguration getRightClickConfiguration() {
        MoveConfiguration captured = capturedRightClickConfigs.get(this.movesetId);
        return captured != null ? captured : rightClickMove;
    }

    /**
     * Get the configuration for crouch right-click move
     */
    public MoveConfiguration getCrouchRightClickConfiguration() {
        MoveConfiguration captured = capturedCrouchRightClickConfigs.get(this.movesetId);
        return captured != null ? captured : crouchRightClickMove;
    }

    /**
     * Check if entity has enough breath/resources for a move - WORKS FOR NPCs TOO
     */
    public boolean hasResourcesForMove(LivingEntity entity, MoveConfiguration config) {
        if (config == null) return false;

        if (config.hasBreathCost()) {
            if (!EntityResources.hasBreath(entity, config.getBreathCostOrDefault(0f))) return false;
        }
        if (config.hasStaminaCost()) {
            if (!EntityResources.hasStamina(entity, config.getStaminaCostOrDefault(0f))) return false;
        }

        return true;
    }

    public boolean consumeResourcesForMove(LivingEntity entity, MoveConfiguration config) {
        if (config == null) return false;

        if (config.hasBreathCost()) {
            if (!EntityResources.consumeBreath(entity, config.getBreathCostOrDefault(0f))) return false;
        }

        return true;
    }

    /**
     * Complete configuration for a moveset move with followup system
     */
    @Getter
    public static class MoveConfiguration {
        public final String moveId;
        public final String displayName;
        public final String description;
        public final Consumer<LivingEntity> startAction;
        public final ResourceLocation animationId;
        public final int animationPriority;

        public final Float damage;
        public final Float range;
        public final Float knockback;
        public final Integer hitStun;
        public final Integer armor;
        public final boolean hyperArmor;
        public final Float hitboxSize;

        public final Integer cooldown;
        public final Integer windup;
        public final Integer duration;
        public final Integer activeFrames;
        public final Integer recovery;

        public final Float breathCost;
        public final Float staminaCost;

        public final Float teleportDistance;
        public final Float dashSpeed;
        public final Integer teleportWindup;

        public final List<FollowupConfiguration> followups;

        private MoveConfiguration(MoveBuilder builder) {
            this.moveId = builder.moveId;
            this.displayName = builder.displayName;
            this.description = builder.description;
            this.startAction = builder.startAction;
            this.animationId = builder.animationId;
            this.animationPriority = builder.animationPriority;

            this.damage = builder.damage;
            this.range = builder.range;
            this.knockback = builder.knockback;
            this.hitStun = builder.hitStun;
            this.armor = builder.armor;
            this.hyperArmor = builder.hyperArmor;
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

        // Simple getters for all fields
        public String getMoveId() { return moveId; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public Consumer<LivingEntity> getStartAction() { return startAction; }
        public ResourceLocation getAnimationId() { return animationId; }
        public int getAnimationPriority() { return animationPriority; }

        public Float getDamage() { return damage; }
        public Float getRange() { return range; }
        public Float getKnockback() { return knockback; }
        public Integer getHitStun() { return hitStun; }
        public Integer getArmor() { return armor; }
        public boolean hasHyperArmor() { return hyperArmor; }
        public Float getHitboxSize() { return hitboxSize; }

        public Integer getCooldown() { return cooldown; }
        public Integer getWindup() { return windup; }
        public Integer getDuration() { return duration; }
        public Integer getActiveFrames() { return activeFrames; }
        public Integer getRecovery() { return recovery; }

        public Float getBreathCost() { return breathCost; }
        public Float getStaminaCost() { return staminaCost; }

        public Float getTeleportDistance() { return teleportDistance; }
        public Float getDashSpeed() { return dashSpeed; }
        public Integer getTeleportWindup() { return teleportWindup; }

        public List<FollowupConfiguration> getFollowups() { return followups; }

        // Convenience methods
        public boolean hasDamage() { return damage != null; }
        public boolean hasRange() { return range != null; }
        public boolean hasKnockback() { return knockback != null; }
        public boolean hasHitStun() { return hitStun != null; }
        public boolean hasArmor() { return armor != null; }
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

        // Safe getters
        public float getDamageOrDefault(float defaultValue) { return damage != null ? damage : defaultValue; }
        public float getRangeOrDefault(float defaultValue) { return range != null ? range : defaultValue; }
        public float getKnockbackOrDefault(float defaultValue) { return knockback != null ? knockback : defaultValue; }
        public int getHitStunOrDefault(int defaultValue) { return hitStun != null ? hitStun : defaultValue; }
        public int getArmorOrDefault(int defaultValue) { return armor != null ? armor : defaultValue; }
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
     * Configuration for followup attacks
     */
    @Getter
    public static class FollowupConfiguration {
        public final String followupMoveId;
        public final String followupDisplayName;
        public final Consumer<LivingEntity> followupAction;
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
     * Builder for followup configurations - NOW uses Consumer<LivingEntity>
     */
    public static class FollowupBuilder {
        private final String followupMoveId;
        private final String followupDisplayName;

        private Consumer<LivingEntity> followupAction;
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

        public FollowupBuilder withAction(Consumer<LivingEntity> action) {
            this.followupAction = action;
            return this;
        }

        public FollowupBuilder withAnimation(String animationId, int priority) {
            this.followupAnimationId = ResourceLocation.parse(animationId);
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
     * Builder for individual moves - NOW uses Consumer<LivingEntity>
     */
    public static class MoveBuilder {
        private final String moveId;
        private final String displayName;

        private String description;
        private Consumer<LivingEntity> startAction;
        private ResourceLocation animationId;
        private int animationPriority = 0;

        private Float damage;
        private Float range;
        private Float knockback;
        private Integer hitStun;
        private Integer armor;
        private boolean hyperArmor;
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

        public MoveBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public MoveBuilder withAction(Consumer<LivingEntity> action) {
            this.startAction = action;
            return this;
        }

        public MoveBuilder withAnimation(String animationId, int priority) {
            this.animationId = ResourceLocation.parse(animationId);
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

        public MoveBuilder withArmor(int armor) {
            if (hyperArmor) {
                throw new IllegalStateException("Move " + moveId + " cannot use both armor and hyper armor");
            }
            if (armor < 1) {
                throw new IllegalArgumentException("Move " + moveId + " armor must be at least 1 hit");
            }
            this.armor = armor;
            return this;
        }

        public MoveBuilder withHyperArmor() {
            if (armor != null) {
                throw new IllegalStateException("Move " + moveId + " cannot use both armor and hyper armor");
            }
            this.hyperArmor = true;
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
            if (armor != null && hyperArmor) {
                throw new IllegalStateException("Move " + moveId + " cannot use both armor and hyper armor");
            }
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

        private MoveConfiguration leftClickMove;
        private MoveConfiguration rightClickMove;
        private MoveConfiguration crouchRightClickMove;

        final List<MoveConfiguration> moveConfigs = new ArrayList<>();

        public MovesetBuilder withIdleAnimation(String idleAnimation) {
            this.idleAnimation = ResourceLocation.parse(idleAnimation);
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

        public MovesetBuilder withLeftClickMove(MoveBuilder moveBuilder) {
            this.leftClickMove = moveBuilder.build();
            return this;
        }

        public MovesetBuilder withRightClickMove(MoveBuilder moveBuilder) {
            this.rightClickMove = moveBuilder.build();
            return this;
        }

        public MovesetBuilder withCrouchRightClickMove(MoveBuilder moveBuilder) {
            this.crouchRightClickMove = moveBuilder.build();
            return this;
        }

        // Overloads that take an already-built MoveConfiguration. Use these when the same config
        // needs to be referenced from outside the builder (e.g. a combo executor that reads the
        // exact same stats) — guarantees a single source of truth across declaration and use.
        public MovesetBuilder withLeftClickMove(MoveConfiguration config) {
            this.leftClickMove = config;
            return this;
        }

        public MovesetBuilder withRightClickMove(MoveConfiguration config) {
            this.rightClickMove = config;
            return this;
        }

        public MovesetBuilder withCrouchRightClickMove(MoveConfiguration config) {
            this.crouchRightClickMove = config;
            return this;
        }

        public MovesetBuilder withMove(MoveBuilder moveBuilder) {
            this.moveConfigs.add(moveBuilder.build());
            return this;
        }
    }

    // These provide Player-specific convenience methods that delegate to LivingEntity versions

    /**
     * Performs a move by index - PLAYER VERSION (wrapper)
     */
    public void performMove(Player player, int moveIndex) {
        performMove((LivingEntity) player, moveIndex);
    }

    /**
     * Override the left-click (M1) behavior - PLAYER VERSION (wrapper)
     */
    public boolean handleLeftClick(Player player) {
        return handleLeftClick((LivingEntity) player);
    }

    /**
     * Override the right-click (M2) behavior - PLAYER VERSION (wrapper)
     */
    public boolean handleRightClick(Player player, boolean isCrouching) {
        return handleRightClick((LivingEntity) player, isCrouching);
    }

    /**
     * Check if the player can perform moves - PLAYER VERSION (wrapper)
     */
    public boolean canPerformMoves(Player player) {
        return canPerformMoves((LivingEntity) player);
    }

    /**
     * Check if player has enough resources for a move - PLAYER VERSION (wrapper)
     */
    public boolean hasResourcesForMove(Player player, MoveConfiguration config) {
        return hasResourcesForMove((LivingEntity) player, config);
    }

    /**
     * Consume resources for a move - PLAYER VERSION (wrapper)
     */
    public boolean consumeResourcesForMove(Player player, MoveConfiguration config) {
        return consumeResourcesForMove((LivingEntity) player, config);
    }

    /**
     * Called after a move is performed - PLAYER VERSION
     * Default implementation does nothing - override in subclasses like FlameBreathingMoveset
     */
    public void onMovePerformed(Player player, int moveIndex, boolean isCrouching) {
        // Default: do nothing
        // Subclasses can override for player-specific post-move effects
    }
}