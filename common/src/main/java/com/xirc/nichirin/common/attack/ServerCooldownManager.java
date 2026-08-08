package com.xirc.nichirin.common.attack;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single server-authoritative cooldown store for ALL movesets (breathing, CQC, katana, …). Moves are
 * keyed per entity by their move id and expire on the world game-time clock. The check is the sole
 * authority — client HUD gating is only a convenience — so cooldowns can't be bypassed by firing a
 * move through a path that skips the client gate (which is how the katana used to ignore them).
 *
 * <p>A small grace window absorbs client/server latency so a move that the client believes is ready
 * isn't rejected a tick or two early.</p>
 */
public final class ServerCooldownManager {

    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new ConcurrentHashMap<>();
    /** Latency tolerance (ticks): a move within this many ticks of ready is allowed through. */
    private static final long GRACE_TICKS = 2L;

    private ServerCooldownManager() {}

    public static boolean isOnCooldown(LivingEntity entity, String key) {
        return getRemaining(entity, key) > GRACE_TICKS;
    }

    /** Remaining cooldown in ticks (0 if ready). Prunes the entry once elapsed. */
    public static int getRemaining(LivingEntity entity, String key) {
        Map<String, Long> playerCooldowns = COOLDOWNS.get(entity.getUUID());
        if (playerCooldowns == null || key == null) return 0;
        long now = entity.level().getGameTime();
        long endTime = playerCooldowns.getOrDefault(key, 0L);
        int remaining = Math.max(0, (int) (endTime - now));
        if (remaining == 0 && endTime > 0) {
            playerCooldowns.remove(key);
            if (playerCooldowns.isEmpty()) {
                COOLDOWNS.remove(entity.getUUID());
            }
        }
        return remaining;
    }

    public static void set(LivingEntity entity, String key, int cooldownTicks) {
        if (cooldownTicks <= 0 || key == null) return;
        COOLDOWNS.computeIfAbsent(entity.getUUID(), id -> new ConcurrentHashMap<>())
                .put(key, entity.level().getGameTime() + cooldownTicks);
    }

    public static void clear(UUID entityId) {
        COOLDOWNS.remove(entityId);
    }

    public static void clearAll() {
        COOLDOWNS.clear();
    }
}
