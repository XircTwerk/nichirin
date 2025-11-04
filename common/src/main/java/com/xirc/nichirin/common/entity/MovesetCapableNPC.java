package com.xirc.nichirin.common.entity;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.UUID;

/**
 * Interface for NPCs that can use movesets
 * Allows entities to execute moves from movesets just like players
 */
public interface MovesetCapableNPC {

    /**
     * Gets the moveset for this NPC
     */
    AbstractMoveset getMoveset();

    /**
     * Sets the moveset for this NPC
     */
    void setMoveset(AbstractMoveset moveset);

    /**
     * Gets the entity's UUID for tracking
     */
    UUID getEntityUUID();

    /**
     * Gets the entity's level/world
     */
    Level getEntityLevel();

    /**
     * Returns this as a LivingEntity for moveset execution
     */
    LivingEntity asLivingEntity();

    /**
     * Check if NPC can use a specific move (not blacklisted, has resources, not on cooldown)
     */
    boolean canUseMove(int moveIndex);

    /**
     * Perform a moveset move by index
     */
    void performMovesetMove(int moveIndex);

    /**
     * Trigger animation for this NPC (Azure integration)
     */
    void triggerMovesetAnimation(String animationName);

    /**
     * Get current blood points (for demon NPCs)
     */
    int getBloodPoints();

    /**
     * Set blood points (for demon NPCs)
     */
    void setBloodPoints(int bloodPoints);

    /**
     * Get max blood points (configurable per NPC)
     */
    int getMaxBloodPoints();

    /**
     * Get current breath gauge (for breathing NPCs)
     */
    float getBreathGauge();

    /**
     * Set breath gauge (for breathing NPCs)
     */
    void setBreathGauge(float breath);

    /**
     * Get max breath gauge (configurable per NPC)
     */
    float getMaxBreathGauge();

    /**
     * Get aggression level (0.2 to 1.0, where 1.0 = 100% aggressive)
     */
    float getAggression();

    /**
     * Get damage multiplier for this NPC
     */
    float getDamageMultiplier();

    /**
     * Get attack speed multiplier
     */
    float getAttackSpeedMultiplier();

    /**
     * Get move speed multiplier
     */
    float getMoveSpeedMultiplier();

    /**
     * Check if this NPC can regenerate blood
     */
    boolean canRegenBlood();

    /**
     * Get blood regen multiplier
     */
    float getBloodRegenMultiplier();

    /**
     * Get breath regen multiplier
     */
    float getBreathRegenMultiplier();

    /**
     * Get the set of blacklisted move indices
     */
    Set<Integer> getBlacklistedMoves();

    /**
     * Check if a move is blacklisted
     */
    default boolean isMoveBlacklisted(int moveIndex) {
        return getBlacklistedMoves().contains(moveIndex);
    }

    /**
     * Tick NPC systems (blood regen, breath regen, cooldowns, etc.)
     */
    void tickMovesetSystems();
}