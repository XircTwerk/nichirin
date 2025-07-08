package com.xirc.nichirin.common.attack;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.moves.thunder.ThunderBreathingAttackBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Handles execution of different attack types
 * Keeps track of active attacks and manages their lifecycle
 */
public class MoveExecutor {

    // Store active attacks
    private static final java.util.Map<Player, java.util.List<Object>> activeAttacks = new java.util.HashMap<>();

    /**
     * Execute a Thunder Breathing attack
     */
    public static void executeThunderAttack(Player player, ThunderBreathingAttackBase attack) {
        if (!attack.isActive()) {
            attack.start(player, player.level());
            trackAttack(player, attack);
        }
    }

    /**
     * Execute an AbstractBreathingAttack
     */
    public static void executeBreathingAttack(Player player, AbstractBreathingAttack<?, ?> attack) {
        if (!attack.isActive()) {
            attack.start(player);
            trackAttack(player, attack);
        }
    }

    /**
     * Execute any attack via reflection (for maximum flexibility)
     */
    public static void executeGenericAttack(Player player, Object attack) {
        try {
            // Try to find and invoke a start method
            var startMethod = attack.getClass().getMethod("start", Player.class, Level.class);
            startMethod.invoke(attack, player, player.level());
            trackAttack(player, attack);
        } catch (Exception e1) {
            try {
                // Try alternative start method signature
                var startMethod = attack.getClass().getMethod("start", Player.class);
                startMethod.invoke(attack, player);
                trackAttack(player, attack);
            } catch (Exception e2) {
                System.err.println("Could not execute attack: " + attack.getClass().getName());
            }
        }
    }

    /**
     * Tick all active attacks for a player
     */
    public static void tickAttacks(Player player) {
        var attacks = activeAttacks.get(player);
        if (attacks != null) {
            attacks.removeIf(attack -> {
                try {
                    // Try to tick the attack
                    if (attack instanceof ThunderBreathingAttackBase thunder) {
                        thunder.tick();
                        return !thunder.isActive();
                    } else if (attack instanceof AbstractBreathingAttack<?, ?> breathing) {
                        breathing.tick(player);
                        return !breathing.isActive();
                    } else {
                        // Try reflection for other types
                        var tickMethod = attack.getClass().getMethod("tick");
                        tickMethod.invoke(attack);

                        var isActiveMethod = attack.getClass().getMethod("isActive");
                        return !(boolean)isActiveMethod.invoke(attack);
                    }
                } catch (Exception e) {
                    return true; // Remove if we can't tick it
                }
            });
        }
    }

    private static void trackAttack(Player player, Object attack) {
        activeAttacks.computeIfAbsent(player, k -> new java.util.ArrayList<>()).add(attack);
    }

    /**
     * Clear all attacks for a player (on death, disconnect, etc)
     */
    public static void clearAttacks(Player player) {
        activeAttacks.remove(player);
    }
}