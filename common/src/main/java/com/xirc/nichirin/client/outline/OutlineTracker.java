package com.xirc.nichirin.client.outline;

import com.xirc.nichirin.common.outline.OutlineInstance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side mirror of OutlineManager. Populated by packet handlers, consumed by the
 * renderer each frame. Tick removes expired instances.
 */
@Environment(EnvType.CLIENT)
public final class OutlineTracker {
    private static final Map<UUID, List<OutlineInstance>> OUTLINES = new ConcurrentHashMap<>();

    private OutlineTracker() {}

    public static void addOutline(UUID entityId, OutlineInstance instance) {
        instance.setStartTimeMs(System.currentTimeMillis());
        OUTLINES.computeIfAbsent(entityId, k -> new ArrayList<>()).add(instance);
    }

    public static void removeOutline(UUID entityId, UUID instanceId) {
        List<OutlineInstance> list = OUTLINES.get(entityId);
        if (list == null) return;
        list.removeIf(o -> o.id().equals(instanceId));
        if (list.isEmpty()) OUTLINES.remove(entityId);
    }

    public static void clearOutlines(UUID entityId) {
        OUTLINES.remove(entityId);
    }

    public static void clearAll() {
        OUTLINES.clear();
    }

    public static List<OutlineInstance> getOutlines(UUID entityId) {
        return OUTLINES.getOrDefault(entityId, List.of());
    }

    public static Map<UUID, List<OutlineInstance>> all() {
        return OUTLINES;
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, List<OutlineInstance>>> it = OUTLINES.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            entry.getValue().removeIf(o -> {
                if (o.lifetimeTicks() < 0) return false;
                long ageMs = now - o.startTimeMs();
                long lifetimeMs = o.lifetimeTicks() * 50L;
                return ageMs >= lifetimeMs;
            });
            if (entry.getValue().isEmpty()) it.remove();
        }
    }
}
