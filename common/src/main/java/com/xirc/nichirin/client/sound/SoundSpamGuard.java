package com.xirc.nichirin.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drops near-duplicate sound instances so combat spam can't exhaust the OpenAL channel pool.
 *
 * <p>Minecraft has a fixed number of sound channels; once attacks flood them (multi-hit AoE moves
 * play several sounds per target per tick, shockwave volleys all impact within the same tick),
 * every later sound silently fails to start — which reads as "sounds randomly stop playing".
 * Allowing at most {@link #MAX_CONCURRENT_SAME} instances of the same sound event within
 * {@link #WINDOW_MS} and {@link #RADIUS} blocks is inaudible (identical overlapping samples just
 * sum to a louder version of the same sound) but keeps channels free.</p>
 */
@Environment(EnvType.CLIENT)
public final class SoundSpamGuard {

    private static final int MAX_CONCURRENT_SAME = 4;
    private static final long WINDOW_MS = 120;
    private static final double RADIUS = 12.0;
    private static final double RADIUS_SQR = RADIUS * RADIUS;

    private record Recent(long timeMs, double x, double y, double z) {}

    // Thread-safe: SoundEngine.play normally runs on the render thread, but mods can route
    // sounds in from other threads — a corrupted plain HashMap can spin forever on resize.
    private static final Map<ResourceLocation, ArrayDeque<Recent>> RECENT = new ConcurrentHashMap<>();

    private SoundSpamGuard() {}

    /** True when the sound may start; false when it's a redundant duplicate to drop. */
    public static boolean allow(SoundInstance sound) {
        if (sound == null) return true;
        // Position can be relative (UI/music) — those never spam, let them through.
        if (sound.isRelative()) return true;

        long now = System.currentTimeMillis();
        ArrayDeque<Recent> deque = RECENT.computeIfAbsent(sound.getLocation(), k -> new ArrayDeque<>(8));

        synchronized (deque) {
            // Prune expired entries for this sound.
            while (!deque.isEmpty() && now - deque.peekFirst().timeMs > WINDOW_MS) {
                deque.pollFirst();
            }

            double x = sound.getX(), y = sound.getY(), z = sound.getZ();
            int nearby = 0;
            for (Recent recent : deque) {
                double dx = recent.x - x, dy = recent.y - y, dz = recent.z - z;
                if (dx * dx + dy * dy + dz * dz <= RADIUS_SQR) {
                    nearby++;
                    if (nearby >= MAX_CONCURRENT_SAME) {
                        return false;
                    }
                }
            }

            deque.addLast(new Recent(now, x, y, z));
        }

        // Opportunistic global cleanup so the map can't grow without bound across sound ids.
        if (RECENT.size() > 256) {
            Iterator<Map.Entry<ResourceLocation, ArrayDeque<Recent>>> it = RECENT.entrySet().iterator();
            while (it.hasNext()) {
                ArrayDeque<Recent> d = it.next().getValue();
                synchronized (d) {
                    while (!d.isEmpty() && now - d.peekFirst().timeMs > WINDOW_MS) {
                        d.pollFirst();
                    }
                    if (d.isEmpty()) {
                        it.remove();
                    }
                }
            }
        }
        return true;
    }
}
