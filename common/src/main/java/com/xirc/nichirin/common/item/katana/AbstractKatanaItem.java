package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.common.util.enums.MoveClass;
import com.xirc.nichirin.common.util.enums.MoveInputType;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Base class for all katana items.
 * Integrates with breathing attacks and PlayerAnimator for smooth combat.
 */
@Getter
public abstract class AbstractKatanaItem extends Item {

    // Combat stats
    protected final float baseAttackDamage;
    protected final float attackSpeed;
    protected final Tier tier;

    // Breathing technique moves mapped by input type
    protected final Map<MoveClass, MoveConfiguration> moves = new HashMap<>();

    // Item properties
    protected final boolean isEnchantable;
    protected final int enchantability;

    // Default animations
    @Nullable
    protected final ResourceLocation idleAnimation;

    protected AbstractKatanaItem(Properties properties, KatanaBuilder builder) {
        super(properties);
        this.baseAttackDamage = builder.baseAttackDamage;
        this.attackSpeed = builder.attackSpeed;
        this.tier = builder.tier;
        this.isEnchantable = builder.isEnchantable;
        this.enchantability = builder.enchantability;
        this.idleAnimation = builder.idleAnimation;

        // Build and configure all moves
        builder.moveConfigs.forEach((moveClass, config) -> {
            AbstractBreathingAttack<?, ?> attack = config.attackSupplier.get();

            // Apply configuration to the breathing attack
            attack.withDamage(config.damage)
                    .withRange(config.range)
                    .withKnockback(config.knockback)
                    .withTiming(config.cooldown, config.windup, config.duration)
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
     * Checks if this katana has a move for the given input
     */
    public boolean hasMove(MoveInputType inputType) {
        return moves.containsKey(inputType.getMoveClass());
    }

    /**
     * Checks if this katana has a move for the given class
     */
    public boolean hasMove(MoveClass moveClass) {
        return moves.containsKey(moveClass);
    }

    /**
     * Called when a move is triggered
     */
    public void performMove(Player player, MoveInputType inputType, IBreathingAttacker<?, ?> attacker) {
        MoveConfiguration config = getMoveConfig(inputType);
        if (config != null && config.breathingAttack != null && !config.breathingAttack.isActive()) {
            // Play the animation if configured
            if (config.animationId != null) {
                playAnimation(player, config.animationId, config.animationPriority);
            }

            // Start the breathing attack
            config.breathingAttack.start(attacker);
        }
    }

    /**
     * Plays a PlayerAnimator animation on the player
     */
    protected abstract void playAnimation(Player player, ResourceLocation animationId, int priority);


    public int getEnchantmentValue(ItemStack stack) {
        return enchantability;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return isEnchantable;
    }

    /**
     * Complete configuration for a katana move
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
        }
    }

    /**
     * Builder for individual katana moves
     */
    public static class MoveBuilder {
        private final Supplier<AbstractBreathingAttack<?, ?>> attackSupplier;
        private ResourceLocation animationId;
        private int animationPriority = 0;

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

        /**
         * Sets the PlayerAnimator animation to play when this move is performed
         * @param animationId The resource location of the animation (e.g., "modid:slash_animation")
         * @param priority Animation priority (higher = overrides lower priority animations)
         */
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
     * Builder class for creating katana items
     */
    public static class KatanaBuilder {
        // Required parameters
        private final Tier tier;

        // Optional parameters with defaults
        private float baseAttackDamage;
        private float attackSpeed = -2.4f;
        private boolean isEnchantable = true;
        private int enchantability = 15;

        // Animations
        private ResourceLocation idleAnimation;

        // Move configurations
        final Map<MoveClass, MoveConfiguration> moveConfigs = new HashMap<>();

        public KatanaBuilder(Tier tier) {
            this.tier = tier;
            // Set damage based on tier
            this.baseAttackDamage = 3.0f + tier.getAttackDamageBonus();
        }

        public KatanaBuilder withBaseAttackDamage(float damage) {
            this.baseAttackDamage = damage;
            return this;
        }

        public KatanaBuilder withAttackSpeed(float speed) {
            this.attackSpeed = speed;
            return this;
        }

        public KatanaBuilder withEnchantability(int enchantability) {
            this.enchantability = enchantability;
            return this;
        }

        public KatanaBuilder setEnchantable(boolean enchantable) {
            this.isEnchantable = enchantable;
            return this;
        }

        /**
         * Sets default animations for idle/walking/blocking states
         */
        public KatanaBuilder withDefaultAnimations(ResourceLocation idle) {
            this.idleAnimation = idle;
            return this;
        }

        /**
         * Adds a move configuration to a specific input type
         */
        public KatanaBuilder withMove(MoveInputType inputType, MoveBuilder moveBuilder) {
            return withMove(inputType.getMoveClass(), moveBuilder.build());
        }

        /**
         * Adds a move configuration to a specific move class
         */
        public KatanaBuilder withMove(MoveClass moveClass, MoveBuilder moveBuilder) {
            return withMove(moveClass, moveBuilder.build());
        }

        /**
         * Adds a pre-built move configuration
         */
        public KatanaBuilder withMove(MoveClass moveClass, MoveConfiguration config) {
            this.moveConfigs.put(moveClass, config);
            return this;
        }

        /**
         * Removes a move configuration
         */
        public KatanaBuilder removeMove(MoveClass moveClass) {
            this.moveConfigs.remove(moveClass);
            return this;
        }

        /**
         * Clears all move configurations
         */
        public KatanaBuilder clearMoves() {
            this.moveConfigs.clear();
            return this;
        }
    }
}