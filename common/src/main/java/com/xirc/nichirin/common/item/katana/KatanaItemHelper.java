package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.SimpleAttackBreathingWrapper;
import com.xirc.nichirin.common.util.enums.MoveInputType;

/**
 * Helper methods for AbstractKatanaItem to work with different attack types
 */
public class KatanaItemHelper {

    /**
     * Checks if a move is a light attack (typically left click)
     */
    public static boolean isLightAttack(AbstractKatanaItem katana, MoveInputType inputType) {
        // BASIC input type is typically used for light attacks
        return inputType == MoveInputType.BASIC;
    }

    /**
     * Checks if a breathing attack is actually a wrapped simple attack
     */
    public static boolean isSimpleAttack(AbstractBreathingAttack<?, ?> attack) {
        return attack instanceof SimpleAttackBreathingWrapper;
    }

    /**
     * Gets the wrapped simple attack if this is a wrapper
     */
    public static com.xirc.nichirin.common.attack.component.AbstractSimpleAttack<?, ?> getSimpleAttack(AbstractBreathingAttack<?, ?> attack) {
        if (attack instanceof SimpleAttackBreathingWrapper<?> wrapper) {
            return wrapper.getSimpleAttack();
        }
        return null;
    }
}