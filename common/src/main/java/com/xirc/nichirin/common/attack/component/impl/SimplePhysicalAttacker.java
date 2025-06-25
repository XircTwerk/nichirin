package com.xirc.nichirin.common.attack.component.impl;

import com.xirc.nichirin.common.attack.component.IPhysicalAttacker;
import com.xirc.nichirin.common.system.StaminaSystem;
import lombok.Getter;
import net.minecraft.world.entity.player.Player;

/**
 * Simple implementation of IPhysicalAttacker for players
 */
@Getter
public class SimplePhysicalAttacker implements IPhysicalAttacker<SimplePhysicalAttacker, Void> {

    private final Player player;
    private final StaminaSystem staminaSystem;

    public SimplePhysicalAttacker(Player player) {
        this.player = player;
        // Initialize stamina system with default values
        this.staminaSystem = new StaminaSystem(player, 100.0f, 2.0f, 20);
    }

    @Override
    public float getStamina() {
        return staminaSystem.getStamina();
    }

    @Override
    public void setStamina(float stamina) {
        staminaSystem.setStamina(stamina);
    }

    @Override
    public float getMaxStamina() {
        return staminaSystem.getMaxStamina();
    }

    @Override
    public SimplePhysicalAttacker getThis() {
        return this;
    }

    /**
     * Ticks the attacker, updating stamina regeneration
     */
    public void tick() {
        staminaSystem.tick();
    }
}