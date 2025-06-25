package com.xirc.nichirin.common.attack.component;

import net.minecraft.world.entity.player.Player;

public class PlayerPhysicalAttacker implements IPhysicalAttacker<PlayerPhysicalAttacker, Object> {
    public static Player create(Player player) {
        return player;
    }

    @Override
    public Player getPlayer() {
        return null;
    }

    @Override
    public float getStamina() {
        return 0;
    }

    @Override
    public void setStamina(float stamina) {

    }

    @Override
    public float getMaxStamina() {
        return 0;
    }

    @Override
    public PlayerPhysicalAttacker getThis() {
        return null;
    }
}
