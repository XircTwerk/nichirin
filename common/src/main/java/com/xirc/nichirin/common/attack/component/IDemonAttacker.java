package com.xirc.nichirin.common.attack.component;

import net.minecraft.world.entity.player.Player;

/**
 * Interface for entities that can use demon arts
 * Similar to IBreathingAttacker but for demon abilities
 */
public interface IDemonAttacker<A extends IDemonAttacker<A, S>, S extends Enum<?>> {

    /**
     * Get the player entity
     */
    Player getPlayer();

    /**
     * Get the current demon state/form (if applicable)
     * This can represent different demon transformations or power levels
     */
    S getCurrentState();

    /**
     * Set the current demon state/form
     */
    void setCurrentState(S state);

    /**
     * Check if this attacker can use demon arts
     * Override to add conditions like transformation requirements
     */
    default boolean canUseDemonArts() {
        return true;
    }

    /**
     * Called when a demon art is successfully executed
     * Override for custom behavior like state changes or visual effects
     */
    default void onDemonArtUsed(AbstractDemonAttack<?, A> attack) {
        // Default implementation does nothing
    }

    /**
     * Get the demon art move map for this attacker
     * Override to provide access to the attacker's demon moves
     */
    default DemonMoveMap<A, S> getDemonMoveMap() {
        return null; // Override in implementing classes
    }
}