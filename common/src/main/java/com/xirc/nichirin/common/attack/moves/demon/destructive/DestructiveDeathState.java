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

    public static boolean isOverdriveEnabled(UUID playerId, long worldTime) {
        State s = get(playerId);
        return s.overdriveEnabled && worldTime < s.overdriveExpiryTick;
    }

    public static boolean isCompassActive(UUID playerId, long worldTime) {
        State s = get(playerId);
        return s.compassActive && worldTime < s.compassExpiryTick;
    }

    /** True until the tracker has finalized the session and started its post-use cooldown. */
    public static boolean hasCompassSession(UUID playerId) {
        return get(playerId).compassActive;
    }

    public static boolean isCompassOnCooldown(UUID playerId, long worldTime) {
        return worldTime < get(playerId).compassCooldownExpiryTick;
    }

    public static int compassCooldownRemaining(UUID playerId, long worldTime) {
        return (int) Math.max(0L, get(playerId).compassCooldownExpiryTick - worldTime);
    }

    public static int compassActiveRemaining(UUID playerId, long worldTime) {
        return (int) Math.max(0L, get(playerId).compassExpiryTick - worldTime);
    }

    public static boolean isCompassOverdriveActive(UUID playerId, long worldTime) {
        State s = get(playerId);
        return s.compassOverdrive && isCompassActive(playerId, worldTime);
    }

    public static void setShockwave(ServerPlayer player, boolean enabled) {
        get(player.getUUID()).shockwaveEnabled = enabled;
        DestructiveDeathStateSyncPacket.send(player);
    }

    public static void activateOverdrive(ServerPlayer player, int durationTicks) {
        State s = get(player.getUUID());
        s.overdriveEnabled = true;
        s.overdriveExpiryTick = player.level().getGameTime() + durationTicks;
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

    public static void startCompassCooldown(ServerPlayer player, int durationTicks) {
        State s = get(player.getUUID());
        s.compassCooldownExpiryTick = player.level().getGameTime() + Math.max(0, durationTicks);
    }

    public static void clearCompassCooldown(ServerPlayer player) {
        get(player.getUUID()).compassCooldownExpiryTick = 0L;
    }

    public static final class State {
        public boolean shockwaveEnabled = false;
        public boolean overdriveEnabled = false;
        public long overdriveExpiryTick = 0L;
        public boolean compassActive = false;
        public boolean compassOverdrive = false;
        public long compassExpiryTick = 0L;
        public long compassCooldownExpiryTick = 0L;
    }
}
