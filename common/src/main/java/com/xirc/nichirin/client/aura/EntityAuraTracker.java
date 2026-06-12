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
    private static final Map<UUID, List<AuraInstance>> AURAS = new ConcurrentHashMap<>();

    private EntityAuraTracker() {}

    public static void addAura(UUID entityId, AuraInstance instance) {
        instance.setStartTimeMs(System.currentTimeMillis());
        AURAS.computeIfAbsent(entityId, k -> new ArrayList<>()).add(instance);
    }

    public static void removeAura(UUID entityId, UUID instanceId) {
        List<AuraInstance> list = AURAS.get(entityId);
        if (list == null) return;
        list.removeIf(a -> a.id().equals(instanceId));
        if (list.isEmpty()) AURAS.remove(entityId);
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

    /** Tick: remove auras whose lifetime expired. */
    public static void tick() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, List<AuraInstance>>> it = AURAS.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            entry.getValue().removeIf(a -> {
                if (a.lifetimeTicks() < 0) return false;
                long ageMs = now - a.startTimeMs();
                long lifetimeMs = a.lifetimeTicks() * 50L;
                return ageMs >= lifetimeMs;
            });
            if (entry.getValue().isEmpty()) it.remove();
        }
    }
}
