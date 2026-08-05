package com.xirc.nichirin.common.attack.moves.demon.destructive;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Connects the client's wheel-button release to the active server-side Compass charge. */
public final class CompassNeedleChargeManager {
    private static final Map<UUID, CompassNeedleAttack> CHARGES = new ConcurrentHashMap<>();

    private CompassNeedleChargeManager() {}

    public static boolean register(UUID playerId, CompassNeedleAttack attack) {
        return CHARGES.putIfAbsent(playerId, attack) == null;
    }

    public static void unregister(UUID playerId, CompassNeedleAttack attack) {
        CHARGES.remove(playerId, attack);
    }

    public static void release(ServerPlayer player) {
        CompassNeedleAttack attack = CHARGES.get(player.getUUID());
        if (attack != null) attack.signalRelease();
    }

    public static boolean isCharging(UUID playerId) {
        return CHARGES.containsKey(playerId);
    }

    public static void clear(UUID playerId) {
        CHARGES.remove(playerId);
    }

    public static void clearAll() {
        CHARGES.clear();
    }
}
