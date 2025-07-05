package com.xirc.nichirin.common.attack.component;

import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.util.enums.MoveClass;

import java.util.HashMap;
import java.util.Map;

public class BreathingMoveMap<A extends IBreathingAttacker<A, S>, S extends Enum<?>> {

    private final Map<MoveClass, AbstractBreathingAttack<?, A>> moves = new HashMap<>();

    public void register(MoveClass moveClass, AbstractBreathingAttack<?, A> move) {
        moves.put(moveClass, move);
        move.onRegister(moveClass);
    }

    public AbstractBreathingAttack<?, A> get(MoveClass moveClass) {
        return moves.get(moveClass);
    }

    public boolean has(MoveClass moveClass) {
        return moves.containsKey(moveClass);
    }

    public void tick(A attacker) {
        moves.values().forEach(move -> move.tick((SimpleKatana) attacker));
    }

    /**
     * Starts a breathing move
     */
    public boolean startMove(MoveClass moveClass, A attacker) {
        AbstractBreathingAttack<?, A> move = get(moveClass);
        if (move != null && attacker.canUseBreathing()) {
            // Check if attacker has enough breath
            if (attacker.consumeBreath(move.getBreathCost())) {
                move.start(attacker);
                return true;
            }
        }
        return false;
    }

    /**
     * Stops a breathing move
     */
    public void stopMove(MoveClass moveClass, A attacker) {
        AbstractBreathingAttack<?, A> move = get(moveClass);
        if (move != null) {
            move.stop(attacker);
        }
    }

    /**
     * Checks if a move is currently active
     */
    public boolean isActive(MoveClass moveClass) {
        AbstractBreathingAttack<?, A> move = get(moveClass);
        return move != null && move.isActive();
    }
}