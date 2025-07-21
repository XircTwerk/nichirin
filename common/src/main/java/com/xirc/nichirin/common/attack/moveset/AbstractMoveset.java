package com.xirc.nichirin.common.attack.moveset;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * AbstractMoveset that works with any attack type
 * Flexible system supporting any number of moves with full configuration
 */
@Getter
public abstract class AbstractMoveset {

    private final String movesetId;
    private final String displayName;

    // List of moves - flexible for any count
    protected final List<MoveConfiguration> moves = new ArrayList<>();

    // Optional moveset-wide properties
    @Nullable
    protected final ResourceLocation idleAnimation;
    protected final float damageMultiplier;
    protected final float speedMultiplier;

    protected AbstractMoveset(String movesetId, String displayName, MovesetBuilder builder) {
        this.movesetId = movesetId;
        this.displayName = displayName;
        this.idleAnimation = builder.idleAnimation;
        this.damageMultiplier = builder.damageMultiplier;
        this.speedMultiplier = builder.speedMultiplier;

        // Add all configured moves
        moves.addAll(builder.moveConfigs);
    }

    /**
     * Override the left-click (M1) behavior for SimpleKatana
     * Return true to override default behavior, false to use default
     */
    public boolean handleLeftClick(Player player) {
        // Default: don't override - use SimpleKatana's default combo system
        return false;
    }

    /**
     * Override the right-click (M2) behavior for SimpleKatana
     * Return true to override default behavior, false to use default
     */
    public boolean handleRightClick(Player player, boolean isCrouching) {
        // Default: don't override - use SimpleKatana's default special attacks
        return false;
    }

    /**
     * Get the move index to use for right-click
     * Default is move 0 (first move)
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
     * Performs a move by index
     */
    public void performMove(Player player, int moveIndex) {
        MoveConfiguration config = getMove(moveIndex);
        if (config != null && config.startAction != null) {
            config.startAction.accept(player);
        }
    }

    /**
     * Complete configuration for a moveset move
     */
    @Getter
    public static class MoveConfiguration {
        // Basic properties
        public final String moveId;
        public final String displayName;
        public final ResourceLocation iconLocation;
        public final Consumer<Player> startAction;
        public final ResourceLocation animationId;
        public final int animationPriority;

        // Combat Stats (nullable - only present if configured)
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

        private MoveConfiguration(MoveBuilder builder) {
            // Basic
            this.moveId = builder.moveId;
            this.displayName = builder.displayName;
            this.iconLocation = builder.iconLocation;
            this.startAction = builder.startAction;
            this.animationId = builder.animationId;
            this.animationPriority = builder.animationPriority;

            // Combat Stats
            this.damage = builder.damage;
            this.range = builder.range;
            this.knockback = builder.knockback;
            this.hitStun = builder.hitStun;
            this.hitboxSize = builder.hitboxSize;

            // Timing
            this.cooldown = builder.cooldown;
            this.windup = builder.windup;
            this.duration = builder.duration;
            this.activeFrames = builder.activeFrames;
            this.recovery = builder.recovery;

            // Resources
            this.breathCost = builder.breathCost;
            this.staminaCost = builder.staminaCost;

            // Movement
            this.teleportDistance = builder.teleportDistance;
            this.dashSpeed = builder.dashSpeed;
            this.teleportWindup = builder.teleportWindup;
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
    }

    /**
     * Builder for individual moves
     */
    public static class MoveBuilder {
        private final String moveId;
        private final String displayName;

        // Basic properties
        private Consumer<Player> startAction;
        private ResourceLocation iconLocation;
        private ResourceLocation animationId;
        private int animationPriority = 0;

        // Combat Stats
        private Float damage;
        private Float range;
        private Float knockback;
        private Integer hitStun;
        private Float hitboxSize;

        // Timing
        private Integer cooldown;
        private Integer windup;
        private Integer duration;
        private Integer activeFrames;
        private Integer recovery;

        // Resources
        private Float breathCost;
        private Float staminaCost;

        // Movement
        private Float teleportDistance;
        private Float dashSpeed;
        private Integer teleportWindup;

        public MoveBuilder(String moveId, String displayName) {
            this.moveId = moveId;
            this.displayName = displayName;
        }

        // Basic methods
        public MoveBuilder withAction(Consumer<Player> action) {
            this.startAction = action;
            return this;
        }

        public MoveBuilder withIcon(String iconPath) {
            this.iconLocation = new ResourceLocation(iconPath);
            return this;
        }

        public MoveBuilder withAnimation(String animationId, int priority) {
            this.animationId = new ResourceLocation(animationId);
            this.animationPriority = priority;
            return this;
        }

        // Combat Stats
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

        // Timing
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

        // Convenience method for timing
        public MoveBuilder withTiming(int cooldown, int windup, int duration) {
            this.cooldown = cooldown;
            this.windup = windup;
            this.duration = duration;
            return this;
        }

        // Resources
        public MoveBuilder withBreathCost(float breathCost) {
            this.breathCost = breathCost;
            return this;
        }

        public MoveBuilder withStaminaCost(float staminaCost) {
            this.staminaCost = staminaCost;
            return this;
        }

        // Movement
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

        // Convenience method for teleport
        public MoveBuilder withTeleport(float distance, int windup) {
            this.teleportDistance = distance;
            this.teleportWindup = windup;
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
        private float damageMultiplier = 1.0f;
        private float speedMultiplier = 1.0f;

        final List<MoveConfiguration> moveConfigs = new ArrayList<>();

        public MovesetBuilder withIdleAnimation(String idleAnimation) {
            this.idleAnimation = new ResourceLocation(idleAnimation);
            return this;
        }

        public MovesetBuilder withDamageMultiplier(float multiplier) {
            this.damageMultiplier = multiplier;
            return this;
        }

        public MovesetBuilder withSpeedMultiplier(float multiplier) {
            this.speedMultiplier = multiplier;
            return this;
        }

        /**
         * Adds a move to the moveset
         */
        public MovesetBuilder withMove(MoveBuilder moveBuilder) {
            this.moveConfigs.add(moveBuilder.build());
            return this;
        }
    }

    /**
     * Get the name of the right-click move for cooldown display
     */
    public String getRightClickMoveName() {
        // Override in each breathing style moveset
        return "Special Move";
    }

    /**
     * Get the name of the crouch right-click move for cooldown display
     */
    public String getCrouchRightClickMoveName() {
        // Override in each breathing style moveset
        return "Crouch Special Move";
    }
}