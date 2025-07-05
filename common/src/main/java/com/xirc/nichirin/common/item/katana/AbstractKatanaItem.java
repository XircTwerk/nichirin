package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.util.enums.MoveClass;
import com.xirc.nichirin.common.util.enums.MoveInputType;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for all katana items.
 * Now uses movesets for flexibility and reusability instead of hardcoded moves.
 */
@Getter
public abstract class AbstractKatanaItem extends Item {

    // Combat stats
    protected final float baseAttackDamage;
    protected final float attackSpeed;
    protected final Tier tier;

    // The moveset this katana uses
    @Nullable
    protected final AbstractMoveset moveset;

    // Item properties
    protected final boolean isEnchantable;
    protected final int enchantability;

    protected AbstractKatanaItem(Properties properties, KatanaBuilder builder) {
        super(properties);
        this.baseAttackDamage = builder.baseAttackDamage;
        this.attackSpeed = builder.attackSpeed;
        this.tier = builder.tier;
        this.moveset = builder.moveset;
        this.isEnchantable = builder.isEnchantable;
        this.enchantability = builder.enchantability;
    }

    /**
     * Gets the move configuration for a specific input type
     */
    @Nullable
    public AbstractMoveset.MoveConfiguration getMoveConfig(MoveInputType inputType) {
        return moveset != null ? moveset.getMoveConfig(inputType) : null;
    }

    /**
     * Gets the move configuration for a specific move class
     */
    @Nullable
    public AbstractMoveset.MoveConfiguration getMoveConfig(MoveClass moveClass) {
        return moveset != null ? moveset.getMoveConfig(moveClass) : null;
    }

    /**
     * Gets the breathing attack for a specific input type
     */
    @Nullable
    public AbstractBreathingAttack<?, ?> getMove(MoveInputType inputType) {
        return moveset != null ? moveset.getMove(inputType) : null;
    }

    /**
     * Gets the breathing attack for a specific move class
     */
    @Nullable
    public AbstractBreathingAttack<?, ?> getMove(MoveClass moveClass) {
        return moveset != null ? moveset.getMove(moveClass) : null;
    }

    /**
     * Checks if this katana has a move for the given input
     */
    public boolean hasMove(MoveInputType inputType) {
        return moveset != null && moveset.hasMove(inputType);
    }

    /**
     * Checks if this katana has a move for the given class
     */
    public boolean hasMove(MoveClass moveClass) {
        return moveset != null && moveset.hasMove(moveClass);
    }

    /**
     * Gets the number of available moves
     */
    public int getMoveCount() {
        return moveset != null ? moveset.getMoveCount() : 0;
    }

    /**
     * Called when a move is triggered
     */
    public void performMove(Player player, MoveInputType inputType, IBreathingAttacker<?, ?> attacker) {
        if (moveset != null) {
            // Get animation info from moveset
            ResourceLocation animationId = moveset.getMoveAnimation(inputType.getMoveClass());
            int animationPriority = moveset.getMoveAnimationPriority(inputType.getMoveClass());

            // Play the animation if configured
            if (animationId != null) {
                playAnimation(player, animationId, animationPriority);
            }

            // Delegate to moveset to perform the move
            moveset.performMove(player, inputType, attacker);
        }
    }

    /**
     * Gets the idle animation for this katana
     */
    @Nullable
    public ResourceLocation getIdleAnimation() {
        return moveset != null ? moveset.getIdleAnimation() : null;
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
     * Gets the effective attack damage including moveset multipliers
     */
    public float getEffectiveAttackDamage() {
        float damage = baseAttackDamage;
        if (moveset != null) {
            damage *= moveset.getDamageMultiplier();
        }
        return damage;
    }

    /**
     * Gets the effective attack speed including moveset multipliers
     */
    public float getEffectiveAttackSpeed() {
        float speed = attackSpeed;
        if (moveset != null) {
            speed *= moveset.getSpeedMultiplier();
        }
        return speed;
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

        // The moveset for this katana
        @Nullable
        private AbstractMoveset moveset;

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
         * Sets the moveset for this katana
         */
        public KatanaBuilder withMoveset(AbstractMoveset moveset) {
            this.moveset = moveset;
            return this;
        }
    }
}