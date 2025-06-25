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
    private final PlayerPhysicalAttacker physicalAttacker;
    private boolean isActive = false;

    public SimpleAttackBreathingWrapper(AbstractSimpleAttack<?, ?> simpleAttack) {
        this.simpleAttack = simpleAttack;
        this.physicalAttacker = null; // Will be created when needed
    }

    @Override
    protected void perform(Player user, Level world) {
        // Delegate to simple attack's hit check
        if (simpleAttack instanceof BasicSlashAttack) {
            ((BasicSlashAttack<?>) simpleAttack).performHitCheck(user, world);
        }
    }


    public SimpleAttackBreathingWrapper<A> start(IBreathingAttacker<?, ?> attacker) {
        Player player = attacker.getPlayer();
        Player physAttacker = PlayerPhysicalAttacker.create(player);

        if (simpleAttack.canStart(physAttacker)) {
            simpleAttack.canStart(physAttacker);
            isActive = true;
        }

        return this;
    }

    public void tick(A attacker) {
        if (!isActive) return;

        Player player = attacker.getPlayer();
        Player physAttacker = PlayerPhysicalAttacker.create(player);

        simpleAttack.tick(physAttacker);

        if (!simpleAttack.isActive()) {
            isActive = false;
        }
    }

    public boolean isActive() {
        return isActive;
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