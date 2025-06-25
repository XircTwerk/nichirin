package com.xirc.nichirin.common.attack.core;

import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public abstract class BreathingAction<T extends BreathingAction<T, A>, A extends IBreathingAttacker<?, ?>> {

    private RunMoment runMoment = RunMoment.ON_STRIKE;

    public abstract void perform(A attacker, LivingEntity user, Set<LivingEntity> targets);

    public RunMoment getRunMoment() {
        return runMoment;
    }

    public void setRunMoment(RunMoment runMoment) {
        this.runMoment = runMoment;
    }

    public abstract T copy();
}