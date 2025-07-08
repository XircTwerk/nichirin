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
 * Flexible system supporting any number of moves
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
        public final String moveId;
        public final String displayName;
        public final ResourceLocation iconLocation;

        // The action to perform when this move is selected
        public final Consumer<Player> startAction;

        // Additional properties
        public final ResourceLocation animationId;
        public final int animationPriority;
        public final float damage;
        public final float range;
        public final int cooldown;

        private MoveConfiguration(MoveBuilder builder) {
            this.moveId = builder.moveId;
            this.displayName = builder.displayName;
            this.iconLocation = builder.iconLocation;
            this.startAction = builder.startAction;
            this.animationId = builder.animationId;
            this.animationPriority = builder.animationPriority;
            this.damage = builder.damage;
            this.range = builder.range;
            this.cooldown = builder.cooldown;
        }
    }

    /**
     * Builder for individual moves
     */
    public static class MoveBuilder {
        private final String moveId;
        private final String displayName;

        private Consumer<Player> startAction;
        private ResourceLocation iconLocation;
        private ResourceLocation animationId;
        private int animationPriority = 0;
        private float damage = 10.0f;
        private float range = 5.0f;
        private int cooldown = 40;

        public MoveBuilder(String moveId, String displayName) {
            this.moveId = moveId;
            this.displayName = displayName;
        }

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

        public MoveBuilder withStats(float damage, float range, int cooldown) {
            this.damage = damage;
            this.range = range;
            this.cooldown = cooldown;
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
}