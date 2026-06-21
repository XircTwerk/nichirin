package com.xirc.nichirin.client.aura;

import com.xirc.nichirin.common.aura.AuraInstance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side mirror of {@link com.xirc.nichirin.common.aura.AuraManager}.
 * Populated by packet handlers, consumed by the renderer each frame.
 */
@Environment(EnvType.CLIENT)
public final class EntityAuraTracker {
    /** Spawn/removal transition length — auras ease in and out over this window. */
    public static final long FADE_MS = 350;

    private static final Map<UUID, List<AuraInstance>> AURAS = new ConcurrentHashMap<>();

    private EntityAuraTracker() {}

    public static void addAura(UUID entityId, AuraInstance instance) {
        instance.setStartTimeMs(System.currentTimeMillis());
        AURAS.computeIfAbsent(entityId, k -> new ArrayList<>()).add(instance);
    }

    public static void removeAura(UUID entityId, UUID instanceId) {
        List<AuraInstance> list = AURAS.get(entityId);
        if (list == null) return;
        // Mark for fade-out rather than deleting — tick() purges once the fade finishes.
        long now = System.currentTimeMillis();
        for (AuraInstance instance : list) {
            if (instance.id().equals(instanceId)) {
                instance.markRemoved(now);
            }
        }
    }

    public static void clearAuras(UUID entityId) {
        AURAS.remove(entityId);
    }

    public static void clearAll() {
        AURAS.clear();
    }

    public static List<AuraInstance> getAuras(UUID entityId) {
        return AURAS.getOrDefault(entityId, List.of());
    }

    public static Map<UUID, List<AuraInstance>> all() {
        return AURAS;
    }

    /** Tick: start fade-out on expired lifetimes, purge instances whose fade finished. */
    public static void tick() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, List<AuraInstance>>> it = AURAS.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            for (AuraInstance a : entry.getValue()) {
                if (a.lifetimeTicks() >= 0 && now - a.startTimeMs() >= a.lifetimeTicks() * 50L) {
                    a.markRemoved(now);
                }
            }
            entry.getValue().removeIf(a -> a.fadeComplete(now, FADE_MS));
            if (entry.getValue().isEmpty()) it.remove();
        }
    }
}
