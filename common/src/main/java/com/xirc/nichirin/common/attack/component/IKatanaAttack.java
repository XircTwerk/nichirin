package com.xirc.nichirin.common.attack.component;

import com.xirc.nichirin.common.util.enums.AttackType;
import com.xirc.nichirin.common.util.enums.MoveClass;
import net.minecraft.world.entity.player.Player;

/**
 * Common interface for all katana attacks
 */
public interface IKatanaAttack {

    /**
     * Gets the type of this attack
     */
    AttackType getAttackType();

    /**
     * Starts the attack
     */
    void start(Player player, Object attacker);

    /**
     * Ticks the attack
     */
    void tick(Player player, Object attacker);

    /**
     * Checks if the attack is currently active
     */
    boolean isActive();

    /**
     * Checks if the attack can be started
     */
    boolean canStart(Player player, Object attacker);

    /**
     * Called when the attack is registered to a move class
     */
    void onRegister(MoveClass moveClass);

    /**
     * Gets the damage value
     */
    float getDamage();

    /**
     * Gets the range value
     */
    float getRange();

    /**
     * Gets the knockback value
     */
    float getKnockback();
}