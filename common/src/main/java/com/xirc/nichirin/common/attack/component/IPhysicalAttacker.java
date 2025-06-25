package com.xirc.nichirin.common.attack.component;

import net.minecraft.world.entity.player.Player;

/**
 * Interface for entities that can perform physical attacks.
 * Handles stamina consumption and physical attack execution.
 */
public interface IPhysicalAttacker<A extends IPhysicalAttacker<A, S>, S> {

    /**
     * Gets the player performing the attack
     */
    Player getPlayer();

    /**
     * Gets the current stamina of the attacker
     */
    float getStamina();

    /**
     * Sets the stamina of the attacker
     */
    void setStamina(float stamina);

    /**
     * Gets the maximum stamina of the attacker
     */
    float getMaxStamina();

    /**
     * Consumes stamina for an attack
     * @return true if stamina was successfully consumed, false if insufficient
     */
    default boolean consumeStamina(float amount) {
        float current = getStamina();
        if (current >= amount) {
            setStamina(current - amount);
            return true;
        }
        return false;
    }

    /**
     * Checks if the attacker has enough stamina
     */
    default boolean hasStamina(float amount) {
        return getStamina() >= amount;
    }

    /**
     * Gets the attacker instance (for chaining)
     */
    A getThis();
}