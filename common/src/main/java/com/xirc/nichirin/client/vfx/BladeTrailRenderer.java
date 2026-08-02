package com.xirc.nichirin.client.vfx;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.Util;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Frame-sampled ribbons swept by the complete animated Blade bone. */
@Environment(EnvType.CLIENT)
public final class BladeTrailRenderer {
    private static final long TRAIL_MILLIS = 165L;
    private static final int MAX_SAMPLES = 14;
    private static final Map<Key, ArrayDeque<Sample>> TRAILS = new HashMap<>();

    private BladeTrailRenderer() {}

    public static void capture(UUID player, ItemDisplayContext context, Vec3 base, Vec3 tip) {
        if (!isHandContext(context)) return;
        long now = Util.getMillis();
        Key key = new Key(player, context);
        ArrayDeque<Sample> samples = TRAILS.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        Sample previous = samples.peekLast();
        if (previous != null && previous.base.distanceToSqr(base) + previous.tip.distanceToSqr(tip) < 0.00008) return;
        samples.addLast(new Sample(base, tip, now));
        while (samples.size() > MAX_SAMPLES) samples.removeFirst();
    }

    public static boolean hasTrails() {
        prune(Util.getMillis());
        return !TRAILS.isEmpty();
    }

    public static void render(PoseStack poseStack, Camera camera) {
        long now = Util.getMillis();
        prune(now);
        if (TRAILS.isEmpty()) return;
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = VfxPixelRender.beginQuads();
        Vec3 cameraPosition = camera.getPosition();

        for (ArrayDeque<Sample> samples : TRAILS.values()) {
            Sample previous = null;
            for (Sample current : samples) {
                if (previous != null) {
                    long age = now - current.time;
                    float life = 1.0f - Math.min(1.0f, age / (float) TRAIL_MILLIS);
                    if (life > 0.0f && previous.base.distanceToSqr(current.base) < 9.0
                            && previous.tip.distanceToSqr(current.tip) < 16.0) {
                        Vec3 a = previous.base.subtract(cameraPosition);
                        Vec3 b = previous.tip.subtract(cameraPosition);
                        Vec3 c = current.tip.subtract(cameraPosition);
                        Vec3 d = current.base.subtract(cameraPosition);
                        int alpha = Math.max(0, Math.min(255, Math.round(225.0f * life * life)));
                        int white = (alpha << 24) | 0xFFFFFF;
                        VfxPixelRender.quad(buffer, matrix,
                                (float) a.x, (float) a.y, (float) a.z,
                                (float) b.x, (float) b.y, (float) b.z,
                                (float) c.x, (float) c.y, (float) c.z,
                                (float) d.x, (float) d.y, (float) d.z, white);
                    }
                }
                previous = current;
            }
        }
        VfxPixelRender.finish(buffer);
    }

    private static void prune(long now) {
        Iterator<ArrayDeque<Sample>> iterator = TRAILS.values().iterator();
        while (iterator.hasNext()) {
            ArrayDeque<Sample> samples = iterator.next();
            while (!samples.isEmpty() && now - samples.peekFirst().time > TRAIL_MILLIS) samples.removeFirst();
            if (samples.size() < 2 && (samples.isEmpty() || now - samples.peekLast().time > 55L)) iterator.remove();
        }
    }

    private static boolean isHandContext(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    private record Key(UUID player, ItemDisplayContext context) {}
    private record Sample(Vec3 base, Vec3 tip, long time) {}
}
