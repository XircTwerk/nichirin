package com.xirc.nichirin.common.attack.component;

import com.xirc.nichirin.common.util.enums.MoveClass;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages breathing technique moves for an attacker
 */
public class BreathingMoveMap<A extends IBreathingAttacker<A, S>, S extends Enum<?>> {

    private final Map<MoveClass, AbstractBreathingAttack<?, A>> moves = new HashMap<>();

    /**
     * Registers a move to a specific class
     */
    public void registerMove(MoveClass moveClass, AbstractBreathingAttack<?, A> move) {
        move.onRegister(moveClass);
        moves.put(moveClass, move);
    }

    /**
     * Ticks all active moves
     */
    public void tick(A attacker) {
        // Get the player from the attacker
        moves.values().forEach(move -> move.tick(attacker.getPlayer()));
    }

    /**
     * Gets a move by class
     */
    public AbstractBreathingAttack<?, A> getMove(MoveClass moveClass) {
        return moves.get(moveClass);
    }

    /**
     * Checks if a move class is registered
     */
    public boolean hasMove(MoveClass moveClass) {
        return moves.containsKey(moveClass);
    }

    /**
     * Stops all active moves
     */
    public void stopAll() {
        moves.values().forEach(move -> {
            move.stop();
        });
    }

    /**
     * Gets all registered moves
     */
    public Map<MoveClass, AbstractBreathingAttack<?, A>> getAllMoves() {
        return new HashMap<>(moves);
    }
}