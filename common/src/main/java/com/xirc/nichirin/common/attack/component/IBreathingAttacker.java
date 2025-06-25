package com.xirc.nichirin.common.attack.component;

import net.minecraft.world.entity.player.Player;

/**
 * Interface for entities that can use breathing attacks
 */
public interface IBreathingAttacker<A extends IBreathingAttacker<A, S>, S extends Enum<?>> {

    /**
     * Gets the player associated with this attacker
     */
    Player getPlayer();

    /**
     * Gets the current state of the attacker
     */
    S getState();

    /**
     * Sets the state of the attacker
     */
    void setState(S state);

    /**
     * Gets the breathing move map for this attacker
     */
    BreathingMoveMap<A, S> getMoveMap();

    /**
     * Checks if the attacker can use breathing techniques
     */
    default boolean canUseBreathing() {
        return getPlayer() != null && getPlayer().isAlive();
    }

    /**
     * Gets the current breath level/stamina
     */
    default float getBreathLevel() {
        return 100.0f; // Default implementation
    }

    /**
     * Consumes breath/stamina
     */
    default boolean consumeBreath(float amount) {
        // Default implementation - always successful
        return true;
    }
}