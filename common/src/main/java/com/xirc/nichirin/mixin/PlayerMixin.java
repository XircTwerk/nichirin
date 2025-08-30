package com.xirc.nichirin.mixin;

import com.xirc.nichirin.common.util.IComboCounter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixin to add combo tracking to Player entities
 */
@Mixin(Player.class)
public abstract class PlayerMixin implements IComboCounter {

    @Unique
    private int nichirin$comboCount = 0;

    @Unique
    private LivingEntity nichirin$lastAttacked = null;

    @Override
    public LivingEntity nichirin$getLastAttacked() {
        return nichirin$lastAttacked;
    }

    @Override
    public void nichirin$setLastAttacked(LivingEntity entity) {
        this.nichirin$lastAttacked = entity;
    }

    @Override
    public int nichirin$getComboCount() {
        return nichirin$comboCount;
    }

    @Override
    public void nichirin$setComboCount(int count) {
        this.nichirin$comboCount = Math.max(0, count);
    }

    @Override
    public void nichirin$incrementComboCount() {
        this.nichirin$comboCount++;
    }
}