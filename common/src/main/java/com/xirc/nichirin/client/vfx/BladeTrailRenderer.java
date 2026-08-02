package com.xirc.nichirin.client.vfx;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
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
    private static final Map<Key, ArrayDeque<Sample>> TRAILS = new HashMap<>();

    private BladeTrailRenderer() {}

    public static void capture(UUID player, ItemDisplayContext context, Vec3 base, Vec3 tip,
                               BladeTrailProfiles.Profile profile) {
        if (!isHandContext(context)) return;
        long now = Util.getMillis();
        Key key = new Key(player, context);
        ArrayDeque<Sample> samples = TRAILS.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        Sample previous = samples.peekLast();
        if (previous != null && !previous.profile.equals(profile)) {
            samples.clear();
            previous = null;
        }
        Vec3 extendedTip = base.add(tip.subtract(base).scale(profile.heightMultiplier()));
        if (previous != null && previous.base.distanceToSqr(base) + previous.tip.distanceToSqr(extendedTip) < 0.00008) return;
        samples.addLast(new Sample(base, extendedTip, now, profile));
        while (samples.size() > profile.maxSamples()) samples.removeFirst();
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

        Minecraft minecraft = Minecraft.getInstance();
        for (Map.Entry<Key, ArrayDeque<Sample>> entry : TRAILS.entrySet()) {
            ArrayDeque<Sample> samples = entry.getValue();
            boolean ownFirstPerson = minecraft.player != null
                    && minecraft.options.getCameraType().isFirstPerson()
                    && entry.getKey().player().equals(minecraft.player.getUUID());
            VfxPixelRender.setRenderContext(1.0f, ownFirstPerson);
            Sample previous = null;
            for (Sample current : samples) {
                if (previous != null) {
                    long age = now - current.time;
                    BladeTrailProfiles.Profile profile = current.profile;
                    float life = 1.0f - Math.min(1.0f, age / (float) profile.lifetimeMillis());
                    if (life > 0.0f && previous.base.distanceToSqr(current.base) < 9.0
                            && previous.tip.distanceToSqr(current.tip) < 16.0 * profile.heightMultiplier() * profile.heightMultiplier()) {
                        Vec3 a = previous.base.subtract(cameraPosition);
                        Vec3 b = previous.tip.subtract(cameraPosition);
                        Vec3 c = current.tip.subtract(cameraPosition);
                        Vec3 d = current.base.subtract(cameraPosition);
                        int alpha = Math.max(0, Math.min(255,
                                Math.round(255.0f * profile.opacity() * life * life)));
                        drawThemedTrail(buffer, matrix, a, b, c, d, profile.theme(), alpha);
                    }
                }
                previous = current;
            }
        }
        VfxPixelRender.clearRenderContext();
        VfxPixelRender.finish(buffer);
    }

    private static void prune(long now) {
        Iterator<ArrayDeque<Sample>> iterator = TRAILS.values().iterator();
        while (iterator.hasNext()) {
            ArrayDeque<Sample> samples = iterator.next();
            while (!samples.isEmpty()
                    && now - samples.peekFirst().time > samples.peekFirst().profile.lifetimeMillis()) {
                samples.removeFirst();
            }
            if (samples.size() < 2 && (samples.isEmpty() || now - samples.peekLast().time > 55L)) iterator.remove();
        }
    }

    private static boolean isHandContext(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    private static void drawThemedTrail(BufferBuilder buffer, Matrix4f matrix,
                                        Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                                        BladeTrailProfiles.Theme theme, int alpha) {
        Vec3 aShadow = a.lerp(b, 0.18);
        Vec3 dShadow = d.lerp(c, 0.18);
        Vec3 bHighlight = a.lerp(b, 0.78);
        Vec3 cHighlight = d.lerp(c, 0.78);
        quad(buffer, matrix, a, aShadow, dShadow, d, color(theme.shadow(), alpha, 0.76f));
        quad(buffer, matrix, aShadow, bHighlight, cHighlight, dShadow,
                color(theme.body(), alpha, 0.94f));
        quad(buffer, matrix, bHighlight, b, c, cHighlight,
                color(theme.highlight(), alpha, 1.0f));
    }

    private static void quad(BufferBuilder buffer, Matrix4f matrix,
                             Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        VfxPixelRender.quad(buffer, matrix,
                (float) a.x, (float) a.y, (float) a.z,
                (float) b.x, (float) b.y, (float) b.z,
                (float) c.x, (float) c.y, (float) c.z,
                (float) d.x, (float) d.y, (float) d.z, color);
    }

    private static int color(int rgb, int alpha, float multiplier) {
        int adjustedAlpha = Math.max(0, Math.min(255, Math.round(alpha * multiplier)));
        return (adjustedAlpha << 24) | rgb;
    }

    private record Key(UUID player, ItemDisplayContext context) {}
    private record Sample(Vec3 base, Vec3 tip, long time, BladeTrailProfiles.Profile profile) {}
}
