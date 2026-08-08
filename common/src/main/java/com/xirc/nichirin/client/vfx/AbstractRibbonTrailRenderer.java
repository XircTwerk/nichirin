package com.xirc.nichirin.client.vfx;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Shared frame-sampled ribbon-trail engine: keeps a short world-space history of (base, tip) pairs
 * per key and sweeps textured ribbons between consecutive samples. Subclasses only choose the key
 * type and how to read the owning player from a key.
 */
@Environment(EnvType.CLIENT)
public abstract class AbstractRibbonTrailRenderer<K> {

    protected final Map<K, ArrayDeque<Sample>> trails = new HashMap<>();

    /** The player that owns a given key — used to detect the first-person self case. */
    protected abstract UUID keyOwner(K key);

    /** Records one frame's segment (world-space base → tip) for {@code key}. */
    public void capture(K key, Vec3 base, Vec3 tip, BladeTrailProfiles.Profile profile) {
        long now = Util.getMillis();
        ArrayDeque<Sample> samples = trails.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        Sample previous = samples.peekLast();
        // Don't hard-clear on a profile change (e.g. combo/animation transition) — that pops the whole
        // trail out at once (a visible flicker). Each sample carries its own profile and the renderer
        // draws per-segment, so simply let the old samples keep fading while new ones append.
        Vec3 extendedTip = base.add(tip.subtract(base).scale(profile.heightMultiplier()));
        if (previous != null && previous.base.distanceToSqr(base) + previous.tip.distanceToSqr(extendedTip) < 0.00008) return;
        samples.addLast(new Sample(base, extendedTip, now, profile));
        while (samples.size() > profile.maxSamples()) samples.removeFirst();
    }

    public boolean hasTrails() {
        prune(Util.getMillis());
        return !trails.isEmpty();
    }

    /** Catmull-Rom sub-steps per sample pair — smooths the ribbon so it flows instead of faceting. */
    private static final int SUBDIVISIONS = 4;

    public void render(PoseStack poseStack, Camera camera) {
        long now = Util.getMillis();
        prune(now);
        if (trails.isEmpty()) return;
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = VfxPixelRender.beginQuads();
        // Trails stay smooth — don't quantize them to the pixel grid like the effect layer.
        VfxPixelRender.setSnapEnabled(false);
        Vec3 cameraPosition = camera.getPosition();

        Minecraft minecraft = Minecraft.getInstance();
        for (Map.Entry<K, ArrayDeque<Sample>> entry : trails.entrySet()) {
            ArrayDeque<Sample> samples = entry.getValue();
            if (samples.size() < 2) continue;
            boolean ownFirstPerson = minecraft.player != null
                    && minecraft.options.getCameraType().isFirstPerson()
                    && minecraft.player.getUUID().equals(keyOwner(entry.getKey()));
            VfxPixelRender.setRenderContext(1.0f, ownFirstPerson);

            Sample[] arr = samples.toArray(new Sample[0]);
            for (int i = 0; i + 1 < arr.length; i++) {
                Sample s1 = arr[i];
                Sample s2 = arr[i + 1];
                BladeTrailProfiles.Profile profile = s2.profile;
                float segLife = 1.0f - Math.min(1.0f, (now - s2.time) / (float) profile.lifetimeMillis());
                if (segLife <= 0.0f) continue;
                float maxTip = 16.0f * profile.heightMultiplier() * profile.heightMultiplier();
                if (s1.base.distanceToSqr(s2.base) >= 9.0 || s1.tip.distanceToSqr(s2.tip) >= maxTip) continue;

                // Neighbours for the Catmull-Rom tangents (clamped at the ends).
                Sample s0 = arr[Math.max(0, i - 1)];
                Sample s3 = arr[Math.min(arr.length - 1, i + 2)];
                float tipTravel = (float) Math.sqrt(s1.tip.distanceToSqr(s2.tip));
                float baseTravel = (float) Math.sqrt(s1.base.distanceToSqr(s2.base));
                float energy = Math.max(0.0f, Math.min(1.0f, (tipTravel + baseTravel - 0.055f) / 0.72f));
                int flowPhase = (int) ((s2.time / 42L) % 4L);

                Vec3 baseAt0 = catmull(s0.base, s1.base, s2.base, s3.base, 0.0).subtract(cameraPosition);
                Vec3 tipAt0 = catmull(s0.tip, s1.tip, s2.tip, s3.tip, 0.0).subtract(cameraPosition);
                for (int step = 0; step < SUBDIVISIONS; step++) {
                    double t1 = (step + 1) / (double) SUBDIVISIONS;
                    Vec3 baseAt1 = catmull(s0.base, s1.base, s2.base, s3.base, t1).subtract(cameraPosition);
                    Vec3 tipAt1 = catmull(s0.tip, s1.tip, s2.tip, s3.tip, t1).subtract(cameraPosition);
                    // Smoothly fade alpha along the segment instead of stepping per sample.
                    double midT = (step + 0.5) / SUBDIVISIONS;
                    long midTime = (long) (s1.time + (s2.time - s1.time) * midT);
                    float life = 1.0f - Math.min(1.0f, (now - midTime) / (float) profile.lifetimeMillis());
                    int alpha = Math.max(0, Math.min(255,
                            Math.round(255.0f * profile.opacity() * life * life)));
                    if (alpha > 0) {
                        drawThemedTrail(buffer, matrix, baseAt0, tipAt0, tipAt1, baseAt1,
                                profile.theme(), alpha, energy, flowPhase);
                    }
                    baseAt0 = baseAt1;
                    tipAt0 = tipAt1;
                }
            }
        }
        VfxPixelRender.clearRenderContext();
        VfxPixelRender.finish(buffer);
    }

    /** Centripetal-ish Catmull-Rom interpolation between p1 and p2 (t in [0,1]). */
    private static Vec3 catmull(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        double x = 0.5 * (2 * p1.x + (-p0.x + p2.x) * t
                + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2
                + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3);
        double y = 0.5 * (2 * p1.y + (-p0.y + p2.y) * t
                + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2
                + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3);
        double z = 0.5 * (2 * p1.z + (-p0.z + p2.z) * t
                + (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2
                + (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3);
        return new Vec3(x, y, z);
    }

    private void prune(long now) {
        Iterator<ArrayDeque<Sample>> iterator = trails.values().iterator();
        while (iterator.hasNext()) {
            ArrayDeque<Sample> samples = iterator.next();
            while (!samples.isEmpty()
                    && now - samples.peekFirst().time > samples.peekFirst().profile.lifetimeMillis()) {
                samples.removeFirst();
            }
            if (samples.size() < 2 && (samples.isEmpty() || now - samples.peekLast().time > 55L)) iterator.remove();
        }
    }

    private static void drawThemedTrail(BufferBuilder buffer, Matrix4f matrix,
                                        Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                                        BladeTrailProfiles.Theme theme, int alpha,
                                        float energy, int flowPhase) {
        Vec3 aShadow = a.lerp(b, 0.18);
        Vec3 dShadow = d.lerp(c, 0.18);
        Vec3 bHighlight = a.lerp(b, 0.78);
        Vec3 cHighlight = d.lerp(c, 0.78);
        quad(buffer, matrix, a, aShadow, dShadow, d, color(theme.shadow(), alpha, 0.76f));
        int flowingBody = color(theme.body(), alpha, 0.94f);
        if (flowPhase == 1 || flowPhase == 2) {
            flowingBody = VfxPixelRender.mixRgb(flowingBody, theme.highlight(),
                    0.10f + energy * 0.14f);
        } else if (flowPhase == 3) {
            flowingBody = VfxPixelRender.mixRgb(flowingBody, theme.shadow(), 0.12f);
        }
        quad(buffer, matrix, aShadow, bHighlight, cHighlight, dShadow, flowingBody);
        quad(buffer, matrix, bHighlight, b, c, cHighlight,
                color(theme.highlight(), alpha, 1.0f));

        if (energy > 0.22f) {
            Vec3 previousInner = a.lerp(b, 0.56);
            Vec3 previousOuter = a.lerp(b, 0.88);
            Vec3 currentOuter = d.lerp(c, 0.88);
            Vec3 currentInner = d.lerp(c, 0.56);
            int coreAlpha = Math.max(0, Math.min(255, Math.round(alpha * energy * 0.48f)));
            quad(buffer, matrix, previousInner, previousOuter, currentOuter, currentInner,
                    color(theme.highlight(), coreAlpha, 1.0f));
        }
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

    protected record Sample(Vec3 base, Vec3 tip, long time, BladeTrailProfiles.Profile profile) {}
}
