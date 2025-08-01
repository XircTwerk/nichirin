package com.xirc.nichirin.common.attack.component;

import com.xirc.nichirin.common.attack.moves.BasicSlashAttack;
import com.xirc.nichirin.common.util.enums.MoveClass;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Wrapper that allows AbstractSimpleAttack to be used where AbstractBreathingAttack is expected
 */
@SuppressWarnings("rawtypes")
public class SimpleAttackBreathingWrapper<A extends IBreathingAttacker> extends AbstractBreathingAttack<SimpleAttackBreathingWrapper<A>, A> {

    private final AbstractSimpleAttack<?, ?> simpleAttack;

    public SimpleAttackBreathingWrapper(AbstractSimpleAttack<?, ?> simpleAttack) {
        this.simpleAttack = simpleAttack;

        // Copy configuration from simple attack to breathing attack
        // This ensures the wrapper has the same stats as the wrapped attack
        if (simpleAttack instanceof BasicSlashAttack) {
            BasicSlashAttack<?> basicAttack = (BasicSlashAttack<?>) simpleAttack;

            // Set default values based on the simple attack using correct method names
            this.damage = basicAttack.getDamage();
            this.range = basicAttack.getRange();
            this.knockback = basicAttack.getKnockback();
            this.hitStun = basicAttack.getHitStun();

            // Use correct method names from AbstractSimpleAttack
            this.cooldown = basicAttack.getTotalDuration(); // Total duration as cooldown
            this.windup = basicAttack.getStartup(); // Startup frames as windup
            this.duration = basicAttack.getActiveFrames(); // Active frames as duration

            // Simple attacks typically don't use breath
            this.breathCost = 0.0f;
            this.hitboxSize = basicAttack.getHitboxSize(); // Use actual hitbox size

            // Mark as configured to prevent emergency defaults
            this.builderConfigured = true;
        }
    }

    @Override
    protected void onStart() {
        // Initialize the simple attack's state if possible
        if (simpleAttack instanceof BasicSlashAttack) {
            BasicSlashAttack<?> basicAttack = (BasicSlashAttack<?>) simpleAttack;
            basicAttack.setActive(true);
            basicAttack.setCurrentTick(0);
        }
    }

    @Override
    protected void perform() {
        // Delegate to simple attack's hit detection method
        if (simpleAttack instanceof BasicSlashAttack && user != null && world != null) {
            // Call the public performHitDetection method if it exists, otherwise use reflection
            try {
                var method = simpleAttack.getClass().getDeclaredMethod("performHitDetection", Player.class, Level.class);
                method.setAccessible(true);
                method.invoke(simpleAttack, user, world);
            } catch (Exception e) {
                // Fallback: just call the tick method which should handle hit detection
                ((BasicSlashAttack<?>) simpleAttack).tick(user);
            }
        }
    }

    @Override
    protected void onStop() {
        // Clean up the simple attack
        if (simpleAttack instanceof BasicSlashAttack) {
            BasicSlashAttack<?> basicAttack = (BasicSlashAttack<?>) simpleAttack;
            basicAttack.setActive(false);
        }
    }

    @Override
    public void tick() {
        // Call the parent tick method which handles the unified logic
        super.tick();

        // Additional simple attack specific ticking if needed
        if (simpleAttack instanceof BasicSlashAttack && isActive) {
            BasicSlashAttack<?> basicAttack = (BasicSlashAttack<?>) simpleAttack;
            basicAttack.setCurrentTick(tickCount);
        }
    }

    @Override
    public void onRegister(MoveClass moveClass) {
        super.onRegister(moveClass);
        if (simpleAttack != null) {
            simpleAttack.onRegister(moveClass);
        }
    }

    // Override builder methods to update both wrapper and wrapped attack
    @Override
    public SimpleAttackBreathingWrapper<A> withDamage(float damage) {
        super.withDamage(damage);
        if (simpleAttack != null) {
            simpleAttack.withDamage(damage);
        }
        return this;
    }

    @Override
    public SimpleAttackBreathingWrapper<A> withRange(float range) {
        super.withRange(range);
        if (simpleAttack != null) {
            simpleAttack.withRange(range);
        }
        return this;
    }

    @Override
    public SimpleAttackBreathingWrapper<A> withKnockback(float knockback) {
        super.withKnockback(knockback);
        if (simpleAttack != null) {
            simpleAttack.withKnockback(knockback);
        }
        return this;
    }

    @Override
    public SimpleAttackBreathingWrapper<A> withBreathCost(float cost) {
        super.withBreathCost(cost);
        // Simple attacks typically don't use breath, but allow override
        return this;
    }

    @Override
    public SimpleAttackBreathingWrapper<A> withTiming(int cooldown, int windup, int duration) {
        super.withTiming(cooldown, windup, duration);
        if (simpleAttack != null) {
            // Simple attack expects (startup, activeFrames, recovery)
            // Map our parameters: windup = startup, duration = activeFrames
            int recovery = Math.max(1, cooldown - windup - duration);
            simpleAttack.withTiming(windup, duration, recovery);
        }
        return this;
    }

    /**
     * Get the wrapped simple attack
     */
    public AbstractSimpleAttack<?, ?> getSimpleAttack() {
        return simpleAttack;
    }

    /**
     * Check if this wrapper contains a specific type of simple attack
     */
    public boolean wraps(Class<?> attackType) {
        return simpleAttack != null && attackType.isInstance(simpleAttack);
    }

    /**
     * Get the wrapped attack cast to a specific type (unsafe)
     */
    @SuppressWarnings("unchecked")
    public <T> T getWrappedAttack(Class<T> type) {
        if (wraps(type)) {
            return (T) simpleAttack;
        }
        return null;
    }
}