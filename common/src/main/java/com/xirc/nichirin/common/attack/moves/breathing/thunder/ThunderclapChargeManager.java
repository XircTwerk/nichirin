package com.xirc.nichirin.common.attack.moves.breathing.thunder;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active Thunderclap charges keyed by player UUID. The attack instance registers itself on
 * start; the client's RMB-release packet looks up the active attack and signals it to fire.
 */
public final class ThunderclapChargeManager {

    private static final ConcurrentHashMap<UUID, ThunderClapFlashAttack> CHARGES = new ConcurrentHashMap<>();

    private ThunderclapChargeManager() {}

    public static void register(UUID playerId, ThunderClapFlashAttack attack) {
        CHARGES.put(playerId, attack);
    }

    public static void unregister(UUID playerId) {
        CHARGES.remove(playerId);
    }

    public static boolean isCharging(UUID playerId) {
        return CHARGES.containsKey(playerId);
    }

    /** Called when the client signals LMB (or RMB) release. */
    public static void releaseCharge(ServerPlayer player) {
        ThunderClapFlashAttack attack = CHARGES.get(player.getUUID());
        if (attack != null) {
            attack.signalRelease();
        }
    }
}
