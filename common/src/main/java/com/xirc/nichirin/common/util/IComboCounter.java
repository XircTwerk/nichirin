package com.xirc.nichirin.common.util;

import net.minecraft.world.entity.LivingEntity;

/**
 * Interface for combo tracking on players
 */
public interface IComboCounter {

    LivingEntity nichirin$getLastAttacked();

    void nichirin$setLastAttacked(LivingEntity entity);

    int nichirin$getComboCount();

    void nichirin$setComboCount(int count);

    void nichirin$incrementComboCount();

    /**
     * Reset combo state completely
     */
    default void nichirin$resetCombo() {
        nichirin$setLastAttacked(null);
        nichirin$setComboCount(0);
    }
}