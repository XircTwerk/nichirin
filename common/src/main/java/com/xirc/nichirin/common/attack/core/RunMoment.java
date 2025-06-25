package com.xirc.nichirin.common.attack.core;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public enum RunMoment {
    AT_INIT,
    ON_STRIKE,
    ON_HIT,
    EVERY_TICK,
    AT_END;

    public boolean shouldRun(AbstractBreathingAttack<?, ?> move, IBreathingAttacker<?, ?> attacker,
                             LivingEntity user, int ticksActive, Set<LivingEntity> targets) {
        return switch (this) {
            case AT_INIT -> ticksActive == 0;
            case ON_STRIKE -> ticksActive == move.getWindupPoint();
            case ON_HIT -> !targets.isEmpty();
            case EVERY_TICK -> true;
            case AT_END -> ticksActive == move.getDuration() - 1;
        };
    }
}
