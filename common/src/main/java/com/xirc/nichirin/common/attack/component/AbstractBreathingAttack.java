package com.xirc.nichirin.common.attack.component;

import com.xirc.nichirin.common.util.BreathingManager;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Breathing technique attacks that extend the base attack system
 * Handles breath resource management specifically
 */
@Getter
public abstract class AbstractBreathingAttack<T extends AbstractBreathingAttack<T>> extends AbstractAttack<T> {

    // Breath-specific configuration
    protected float breathCost = 15.0f;

    // Breath consumption tracking
    private boolean breathConsumed = false;

    /**
     * Configure breath cost
     */
    @SuppressWarnings("unchecked")
    public T withBreathCost(float cost) {
        this.breathCost = cost;
        return (T) this;
    }

    /**
     * Check if attack can start - breath version
     */
    @Override
    protected boolean canStart() {
        if (breathCost > 0 && !BreathingManager.hasBreath(user, breathCost)) {
            user.displayClientMessage(
                    Component.literal("Not enough breath!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            return false;
        }
        return true;
    }

    /**
     * Consume breath resources
     */
    @Override
    protected boolean consumeResources() {
        if (breathCost > 0) {
            if (BreathingManager.consume(user, breathCost)) {
                breathConsumed = true;
                return true;
            } else {
                user.displayClientMessage(
                        Component.literal("Failed to consume breath!")
                                .withStyle(style -> style.withColor(0xFF5555)),
                        true
                );
                return false;
            }
        }
        return true; // No resources to consume
    }

    /**
     * Refund breath if attack was cancelled
     */
    @Override
    protected void refundResources() {
        if (breathConsumed) {
            BreathingManager.restore(user, breathCost);
            breathConsumed = false;
        }
    }

    /**
     * Check if breath was consumed (for debugging)
     */
    public boolean wasBreathConsumed() {
        return breathConsumed;
    }

    /**
     * Legacy method support for backward compatibility with IBreathingAttacker
     */
    public void start(IBreathingAttacker attacker) {
        Player player = attacker.getPlayer();
        start(player, player.level());
    }

    /**
     * Legacy method support for MoveClass registration
     */
    public void onRegister(com.xirc.nichirin.common.util.enums.MoveClass moveClass) {
        // Override if needed - default implementation does nothing
    }
}