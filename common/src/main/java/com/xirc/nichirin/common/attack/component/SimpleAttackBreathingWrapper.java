package com.xirc.nichirin.common.attack.component;

import com.xirc.nichirin.common.attack.moves.BasicSlashAttack;
import com.xirc.nichirin.common.util.enums.MoveClass;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Wrapper that allows AbstractSimpleAttack to be used where AbstractBreathingAttack is expected
 */
public class SimpleAttackBreathingWrapper<A extends IBreathingAttacker<A, ?>> extends AbstractBreathingAttack<SimpleAttackBreathingWrapper<A>, A> {

    private final AbstractSimpleAttack<?, ?> simpleAttack;
    private Player currentPlayer;

    public SimpleAttackBreathingWrapper(AbstractSimpleAttack<?, ?> simpleAttack) {
        this.simpleAttack = simpleAttack;
    }

    @Override
    protected void perform(Player user, Level world) {
        // Delegate to simple attack's hit check
        if (simpleAttack instanceof BasicSlashAttack) {
            ((BasicSlashAttack<?>) simpleAttack).performHitCheck(user, world);
        }
    }

    @Override
    public void start(Player player) {
        this.currentPlayer = player;

        // The simple attack expects an IPhysicalAttacker, not a Player directly
        // We need to create or get the appropriate attacker instance

        // Since we can't call start without the proper type, we'll just mark as active
        // and handle the attack logic in perform()
        setActive(true);
        setCurrentUser(player);

        // Initialize the simple attack's state if possible
        if (simpleAttack instanceof BasicSlashAttack) {
            // For now, we'll handle the attack logic directly in perform()
            ((BasicSlashAttack<?>) simpleAttack).setActive(true);
            ((BasicSlashAttack<?>) simpleAttack).setCurrentTick(0);
        }
    }

    @Override
    public void start(A attacker) {
        Player player = attacker.getPlayer();
        start(player);
    }

    @Override
    public void tick(Player player) {
        if (!isActive() || currentPlayer == null) return;

        // Since we can't call tick() directly on the simple attack without the proper attacker type,
        // we'll handle the ticking manually
        if (simpleAttack instanceof BasicSlashAttack) {
            BasicSlashAttack<?> basicAttack = (BasicSlashAttack<?>) simpleAttack;

            // Update tick count
            int currentTick = basicAttack.getCurrentTick();
            basicAttack.setCurrentTick(currentTick + 1);

            // Check if attack should end based on total duration
            if (currentTick >= basicAttack.getTotalDuration()) {
                basicAttack.setActive(false);
                setActive(false);
                setCurrentUser(null);
                currentPlayer = null;
            }
        } else {
            // For other attack types, just deactivate after a default duration
            setActive(false);
            setCurrentUser(null);
            currentPlayer = null;
        }
    }

    @Override
    public void onRegister(MoveClass moveClass) {
        super.onRegister(moveClass);
        simpleAttack.onRegister(moveClass);
    }

    // Delegate configuration methods to the wrapped simple attack
    @Override
    public SimpleAttackBreathingWrapper<A> withDamage(float damage) {
        simpleAttack.withDamage(damage);
        return this;
    }

    @Override
    public SimpleAttackBreathingWrapper<A> withRange(float range) {
        simpleAttack.withRange(range);
        return this;
    }

    @Override
    public SimpleAttackBreathingWrapper<A> withKnockback(float knockback) {
        simpleAttack.withKnockback(knockback);
        return this;
    }

    @Override
    public SimpleAttackBreathingWrapper<A> withHitStun(int hitStun) {
        simpleAttack.withHitStun(hitStun);
        return this;
    }

    @Override
    public SimpleAttackBreathingWrapper<A> withTiming(int cooldown, int windup, int duration) {
        simpleAttack.withTiming(windup, duration - windup, cooldown);
        return this;
    }

    public AbstractSimpleAttack<?, ?> getSimpleAttack() {
        return simpleAttack;
    }
}