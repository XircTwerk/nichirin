package com.xirc.nichirin.common.attack.component;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset.MoveConfiguration;
import com.xirc.nichirin.common.util.BreathingManager;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Compatibility layer for AbstractBreathingAttack
 * Maintains old 2-parameter API while using new base system
 */
@Getter
public abstract class AbstractBreathingAttack<T extends AbstractBreathingAttack<T, A>, A extends IBreathingAttacker> extends AbstractAttack<T> {

    // Breath-specific configuration
    protected float breathCost = 15.0f;

    // Breath consumption tracking
    private boolean breathConsumed = false;

    /**
     * Configure this attack with values from the moveset
     * This MUST be called by the moveset before starting the attack
     */
    public void configure(MoveConfiguration config) {
        if (configured) {
            return; // Prevent double-configuration
        }

        // Combat Stats - use sensible defaults if not configured
        this.damage = config.getDamageOrDefault(10.0f);
        this.range = config.getRangeOrDefault(3.0f);
        this.knockback = config.getKnockbackOrDefault(0f);
        this.hitStun = config.getHitStunOrDefault(8);
        this.hitboxSize = config.getHitboxSizeOrDefault(2.0f);

        // Timing
        this.cooldown = config.getCooldownOrDefault(40);
        this.windup = config.getWindupOrDefault(5);
        this.duration = config.getDurationOrDefault(20);

        // Resources
        this.breathCost = config.getBreathCostOrDefault(15.0f);

        // Movement (nullable - only set if configured in moveset)
        this.teleportDistance = config.getTeleportDistance();
        this.dashSpeed = config.getDashSpeed();
        this.teleportWindup = config.getTeleportWindup();

        this.configured = true;
    }

    /**
     * Configure breath cost
     */
    @SuppressWarnings("unchecked")
    public T withBreathCost(float cost) {
        if (!configured) {
            this.breathCost = cost;
        }
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
    public void start(A attacker) {
        Player player = attacker.getPlayer();
        start(player, player.level());
    }

    /**
     * Legacy tick method for backward compatibility
     */
    public void tick(Player player) {
        tick();
    }

    /**
     * Legacy method support for MoveClass registration
     */
    public void onRegister(com.xirc.nichirin.common.util.enums.MoveClass moveClass) {
        // Override if needed - default implementation does nothing
    }
}