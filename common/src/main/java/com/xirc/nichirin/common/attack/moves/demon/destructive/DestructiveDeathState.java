package com.xirc.nichirin.common.attack.moves.demon.destructive;

import com.xirc.nichirin.common.network.s2c.DestructiveDeathStateSyncPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative state for the Destructive Death BDA per player.
 *
 * <p>Holds the persistent toggles (shockwave-on-CQC, overdrive) plus the transient Compass Needle
 * activation window. Cleared on logout via {@link #cleanup(UUID)}.</p>
 */
public final class DestructiveDeathState {

    private static final ConcurrentHashMap<UUID, State> STATES = new ConcurrentHashMap<>();

    private DestructiveDeathState() {}

    public static State get(UUID playerId) {
        return STATES.computeIfAbsent(playerId, id -> new State());
    }

    public static void cleanup(UUID playerId) {
        STATES.remove(playerId);
    }

    public static boolean isShockwaveEnabled(UUID playerId) {
        return get(playerId).shockwaveEnabled;
    }

    public static boolean isOverdriveEnabled(UUID playerId) {
        return get(playerId).overdriveEnabled;
    }

    public static boolean isCompassActive(UUID playerId, long worldTime) {
        State s = get(playerId);
        return s.compassActive && worldTime < s.compassExpiryTick;
    }

    public static boolean isCompassOverdriveActive(UUID playerId, long worldTime) {
        State s = get(playerId);
        return s.compassOverdrive && isCompassActive(playerId, worldTime);
    }

    public static void setShockwave(ServerPlayer player, boolean enabled) {
        get(player.getUUID()).shockwaveEnabled = enabled;
        DestructiveDeathStateSyncPacket.send(player);
    }

    public static void setOverdrive(ServerPlayer player, boolean enabled) {
        get(player.getUUID()).overdriveEnabled = enabled;
        DestructiveDeathStateSyncPacket.send(player);
    }

    public static void activateCompass(ServerPlayer player, int durationTicks, boolean overdrive) {
        State s = get(player.getUUID());
        s.compassActive = true;
        s.compassExpiryTick = player.level().getGameTime() + durationTicks;
        s.compassOverdrive = overdrive;
        DestructiveDeathStateSyncPacket.send(player);
    }

    public static void deactivateCompass(ServerPlayer player) {
        State s = get(player.getUUID());
        s.compassActive = false;
        s.compassOverdrive = false;
        DestructiveDeathStateSyncPacket.send(player);
    }

    public static final class State {
        public boolean shockwaveEnabled = false;
        public boolean overdriveEnabled = false;
        public boolean compassActive = false;
        public boolean compassOverdrive = false;
        public long compassExpiryTick = 0L;
    }
}
