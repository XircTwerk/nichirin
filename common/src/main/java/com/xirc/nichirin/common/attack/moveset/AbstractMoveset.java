package com.xirc.nichirin.common.attack.moveset;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.common.item.katana.AbstractKatanaItem;
import com.xirc.nichirin.common.util.enums.MoveClass;
import com.xirc.nichirin.common.util.enums.MoveInputType;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Base class for all movesets that can be used by katanas.
 * Separates move definitions from katana items for better reusability.
 */
@Getter
public abstract class AbstractMoveset {

    private final String movesetId;
    private final String displayName;

    // Moves mapped by their class
    protected final Map<MoveClass, MoveConfiguration> moves = new HashMap<>();

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

        // Build and configure all moves
        builder.moveConfigs.forEach((moveClass, config) -> {
            AbstractBreathingAttack<?, ?> attack = config.attackSupplier.get();

            // Apply configuration to the breathing attack
            attack.withDamage(config.damage * damageMultiplier)
                    .withRange(config.range)
                    .withKnockback(config.knockback)
                    .withTiming(
                            (int)(config.cooldown / speedMultiplier),
                            (int)(config.windup / speedMultiplier),
                            (int)(config.duration / speedMultiplier)
                    )
                    .withHitStun(config.hitStun);

            if (config.hitboxSize > 0) {
                attack.setAreaOfEffect(true, config.maxTargets);
            }

            if (config.userVelocity != null) {
                attack.withUserVelocity(config.userVelocity);
            }

            if (config.lockMovement) {
                attack.lockMovement(true);
            }

            attack.setHoldable(config.holdable);
            attack.setPiercing(config.piercing);

            // Register the configured move
            attack.onRegister(moveClass);
            moves.put(moveClass, config);
        });
    }

    /**
     * Gets the move configuration for a specific input type
     */
    @Nullable
    public MoveConfiguration getMoveConfig(MoveInputType inputType) {
        return moves.get(inputType.getMoveClass());
    }

    /**
     * Gets the move configuration for a specific move class
     */
    @Nullable
    public MoveConfiguration getMoveConfig(MoveClass moveClass) {
        return moves.get(moveClass);
    }

    /**
     * Gets the breathing attack for a specific input type
     */
    @Nullable
    public AbstractBreathingAttack<?, ?> getMove(MoveInputType inputType) {
        MoveConfiguration config = getMoveConfig(inputType);
        return config != null ? config.breathingAttack : null;
    }

    /**
     * Gets the breathing attack for a specific move class
     */
    @Nullable
    public AbstractBreathingAttack<?, ?> getMove(MoveClass moveClass) {
        MoveConfiguration config = getMoveConfig(moveClass);
        return config != null ? config.breathingAttack : null;
    }

    /**
     * Checks if this moveset has a move for the given input
     */
    public boolean hasMove(MoveInputType inputType) {
        return moves.containsKey(inputType.getMoveClass());
    }

    /**
     * Checks if this moveset has a move for the given class
     */
    public boolean hasMove(MoveClass moveClass) {
        return moves.containsKey(moveClass);
    }

    /**
     * Called when a move is triggered - to be called by the katana
     */
    public void performMove(Player player, MoveInputType inputType, IBreathingAttacker<?, ?> attacker) {
        MoveConfiguration config = getMoveConfig(inputType);
        if (config != null && config.breathingAttack != null && !config.breathingAttack.isActive()) {
            // Start the breathing attack
            config.breathingAttack.start(attacker);
        }
    }

    /**
     * Gets the animation for a specific move
     */
    @Nullable
    public ResourceLocation getMoveAnimation(MoveClass moveClass) {
        MoveConfiguration config = getMoveConfig(moveClass);
        return config != null ? config.animationId : null;
    }

    /**
     * Gets the animation priority for a specific move
     */
    public int getMoveAnimationPriority(MoveClass moveClass) {
        MoveConfiguration config = getMoveConfig(moveClass);
        return config != null ? config.animationPriority : 0;
    }

    /**
     * Gets all available moves in this moveset
     */
    public Map<MoveClass, MoveConfiguration> getAllMoves() {
        return new HashMap<>(moves);
    }

    /**
     * Gets the number of moves in this moveset
     */
    public int getMoveCount() {
        return moves.size();
    }

    /**
     * Complete configuration for a moveset move
     * Reusing the same structure from AbstractKatanaItem for compatibility
     */
    @Getter
    public static class MoveConfiguration {
        private final Supplier<AbstractBreathingAttack<?, ?>> attackSupplier;
        public final AbstractBreathingAttack<?, ?> breathingAttack;
        public final ResourceLocation animationId;
        public final int animationPriority;

        // Combat stats
        private final float damage;
        private final float hitboxSize;
        private final Vec3 hitboxOffset;
        private final float range;
        private final int maxTargets;

        // Timing
        private final int cooldown;
        private final int windup;
        private final int duration;
        private final int hitStun;

        // Physics
        private final float knockback;
        private final Vec3 userVelocity;
        private final boolean lockMovement;

        // Behavior
        private final boolean holdable;
        private final boolean piercing;
        private final boolean blockBreak;

        // Icon for UI display
        private final ResourceLocation iconLocation;

        private MoveConfiguration(MoveBuilder builder) {
            this.attackSupplier = builder.attackSupplier;
            this.breathingAttack = builder.attackSupplier.get();
            this.animationId = builder.animationId;
            this.animationPriority = builder.animationPriority;
            this.damage = builder.damage;
            this.hitboxSize = builder.hitboxSize;
            this.hitboxOffset = builder.hitboxOffset;
            this.range = builder.range;
            this.maxTargets = builder.maxTargets;
            this.cooldown = builder.cooldown;
            this.windup = builder.windup;
            this.duration = builder.duration;
            this.hitStun = builder.hitStun;
            this.knockback = builder.knockback;
            this.userVelocity = builder.userVelocity;
            this.lockMovement = builder.lockMovement;
            this.holdable = builder.holdable;
            this.piercing = builder.piercing;
            this.blockBreak = builder.blockBreak;
            this.iconLocation = builder.iconLocation;
        }
    }

    /**
     * Builder for individual moveset moves
     * Extended from AbstractKatanaItem.MoveBuilder for compatibility
     */
    public static class MoveBuilder {
        private final Supplier<AbstractBreathingAttack<?, ?>> attackSupplier;
        private ResourceLocation animationId;
        private int animationPriority = 0;
        private ResourceLocation iconLocation;

        // Combat stats with defaults
        private float damage = 5.0f;
        private float hitboxSize = 1.5f;
        private Vec3 hitboxOffset = new Vec3(0, 0, 1.5);
        private float range = 3.0f;
        private int maxTargets = 1;

        // Timing defaults
        private int cooldown = 20;
        private int windup = 5;
        private int duration = 20;
        private int hitStun = 20;

        // Physics defaults
        private float knockback = 0.4f;
        private Vec3 userVelocity = null;
        private boolean lockMovement = false;

        // Behavior defaults
        private boolean holdable = false;
        private boolean piercing = false;
        private boolean blockBreak = false;

        public MoveBuilder(Supplier<AbstractBreathingAttack<?, ?>> attackSupplier) {
            this.attackSupplier = attackSupplier;
        }

        public MoveBuilder withAnimation(ResourceLocation animationId, int priority) {
            this.animationId = animationId;
            this.animationPriority = priority;
            return this;
        }

        public MoveBuilder withAnimation(String animationId, int priority) {
            return withAnimation(new ResourceLocation(animationId), priority);
        }

        public MoveBuilder withAnimation(String animationId) {
            return withAnimation(animationId, 0);
        }

        public MoveBuilder withIcon(ResourceLocation iconLocation) {
            this.iconLocation = iconLocation;
            return this;
        }

        public MoveBuilder withIcon(String iconPath) {
            return withIcon(new ResourceLocation(iconPath));
        }

        public MoveBuilder withDamage(float damage) {
            this.damage = damage;
            return this;
        }

        public MoveBuilder withHitbox(float size, Vec3 offset) {
            this.hitboxSize = size;
            this.hitboxOffset = offset;
            return this;
        }

        public MoveBuilder withHitbox(float size, double offsetX, double offsetY, double offsetZ) {
            return withHitbox(size, new Vec3(offsetX, offsetY, offsetZ));
        }

        public MoveBuilder withRange(float range) {
            this.range = range;
            return this;
        }

        public MoveBuilder withMaxTargets(int maxTargets) {
            this.maxTargets = maxTargets;
            return this;
        }

        public MoveBuilder withTiming(int cooldown, int windup, int duration) {
            this.cooldown = cooldown;
            this.windup = windup;
            this.duration = duration;
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

        public MoveBuilder withHitStun(int hitStun) {
            this.hitStun = hitStun;
            return this;
        }

        public MoveBuilder withKnockback(float knockback) {
            this.knockback = knockback;
            return this;
        }

        public MoveBuilder withUserVelocity(Vec3 velocity) {
            this.userVelocity = velocity;
            return this;
        }

        public MoveBuilder withUserVelocity(double x, double y, double z) {
            return withUserVelocity(new Vec3(x, y, z));
        }

        public MoveBuilder lockMovement(boolean lock) {
            this.lockMovement = lock;
            return this;
        }

        public MoveBuilder setHoldable(boolean holdable) {
            this.holdable = holdable;
            return this;
        }

        public MoveBuilder setPiercing(boolean piercing) {
            this.piercing = piercing;
            return this;
        }

        public MoveBuilder setBlockBreak(boolean blockBreak) {
            this.blockBreak = blockBreak;
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
        // Optional parameters with defaults
        private ResourceLocation idleAnimation;
        private float damageMultiplier = 1.0f;
        private float speedMultiplier = 1.0f;

        // Move configurations
        final Map<MoveClass, MoveConfiguration> moveConfigs = new HashMap<>();

        public MovesetBuilder() {}

        public MovesetBuilder withIdleAnimation(ResourceLocation idleAnimation) {
            this.idleAnimation = idleAnimation;
            return this;
        }

        public MovesetBuilder withIdleAnimation(String idleAnimation) {
            return withIdleAnimation(new ResourceLocation(idleAnimation));
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
         * Adds a move configuration to a specific input type
         */
        public MovesetBuilder withMove(MoveInputType inputType, MoveBuilder moveBuilder) {
            return withMove(inputType.getMoveClass(), moveBuilder.build());
        }

        /**
         * Adds a move configuration to a specific move class
         */
        public MovesetBuilder withMove(MoveClass moveClass, MoveBuilder moveBuilder) {
            return withMove(moveClass, moveBuilder.build());
        }

        /**
         * Adds a pre-built move configuration
         */
        public MovesetBuilder withMove(MoveClass moveClass, MoveConfiguration config) {
            this.moveConfigs.put(moveClass, config);
            return this;
        }

        /**
         * Removes a move configuration
         */
        public MovesetBuilder removeMove(MoveClass moveClass) {
            this.moveConfigs.remove(moveClass);
            return this;
        }

        /**
         * Clears all move configurations
         */
        public MovesetBuilder clearMoves() {
            this.moveConfigs.clear();
            return this;
        }
    }
}