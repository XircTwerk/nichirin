package com.xirc.nichirin.common.attack.core;

import com.xirc.nichirin.common.attack.component.IBreathingAttacker;

import java.util.function.Predicate;

public abstract class BreathingCondition<T extends BreathingCondition<T, A>, A extends IBreathingAttacker<?, ?>> implements Predicate<A> {

    @Override
    public abstract boolean test(A attacker);

    public abstract T copy();
}