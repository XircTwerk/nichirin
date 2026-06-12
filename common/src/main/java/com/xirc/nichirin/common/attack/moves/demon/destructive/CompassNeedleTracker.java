package com.xirc.nichirin.common.attack.moves.demon.destructive;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side mapping of `compass-owner UUID → set of currently-tracked target UUIDs`.
 * Refreshed each tick the compass is active. Used by attack damage code to apply the
 * Compass Needle damage bonus, and synced to the client for HUD/aura rendering.
 */
public final class CompassNeedleTracker {

    private static final ConcurrentHashMap<UUID, Set<UUID>> TRACKED = new ConcurrentHashMap<>();

    private CompassNeedleTracker() {}

    public static void setTrackedTargets(UUID owner, Set<UUID> targets) {
        TRACKED.put(owner, targets);
    }

    public static Set<UUID> getTrackedTargets(UUID owner) {
        return TRACKED.getOrDefault(owner, Collections.emptySet());
    }

    public static boolean isTracked(UUID owner, UUID target) {
        return TRACKED.getOrDefault(owner, Collections.emptySet()).contains(target);
    }

    public static void clear(UUID owner) {
        TRACKED.remove(owner);
    }
}
