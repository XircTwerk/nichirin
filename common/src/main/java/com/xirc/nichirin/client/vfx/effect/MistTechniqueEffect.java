package com.xirc.nichirin.client.vfx.effect;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.client.vfx.VfxEffect;
import com.xirc.nichirin.client.vfx.VfxInstance;
import com.xirc.nichirin.client.vfx.VfxPixelRender;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.function.DoubleFunction;

/** Connected Mist Breathing shapes rendered through the shared pixel pass. */
public final class MistTechniqueEffect implements VfxEffect {
    public enum Style {
        THRUST(24), EIGHT_LAYER(20), CIRCULAR(26), SHIFTING_FLOW(32), SEA_OF_HAZE(38),
        LUNAR(34), FINISHER(22), OBSCURING(52);

        private final int lifetime;
        Style(int lifetime) { this.lifetime = lifetime; }
    }

    private static final int INK = 0x1D383F;
    private static final int DEEP = 0x274E54;
    private static final int MID = 0x4E9DA9;
    private static final int PALE = 0xBDE9F0;
    private static final int WHITE = 0xE4FEFF;

    private final Style style;
    private Vec3 up = new Vec3(0, 1, 0);
    private float reveal;

    public MistTechniqueEffect(Style style) { this.style = style; }

    @Override
    public int lifetimeTicks() { return style.lifetime; }

    @Override
    public void render(VfxInstance instance, PoseStack poseStack, Camera camera, float partialTick) {
        float age = instance.ageTicks() + partialTick;
        float revealTicks = switch (style) {
            case SHIFTING_FLOW, SEA_OF_HAZE, LUNAR -> 16.0f;
            case OBSCURING -> 20.0f;
            default -> 8.0f;
        };
        reveal = smooth(clamp(age / revealTicks));
        float fade = 1.0f - clamp((age - style.lifetime * 0.62f) / (style.lifetime * 0.38f));
        if (fade <= 0.0f) return;

        Vec3 origin = instance.origin(partialTick).subtract(camera.getPosition());
        Vec3 forward = instance.direction().normalize();
        Vec3 right = rightOf(forward);
        up = right.cross(forward).normalize();
        float scale = instance.scale();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = VfxPixelRender.beginQuads();

        if (style == Style.THRUST || style == Style.SHIFTING_FLOW || style == Style.SEA_OF_HAZE || style == Style.LUNAR) {
            drawHistory(buffer, matrix, instance, camera, right, scale, fade);
        }
        switch (style) {
            case THRUST -> drawThrust(buffer, matrix, origin, forward, right, scale, fade, age);
            case EIGHT_LAYER -> drawEightLayer(buffer, matrix, origin, forward, right, scale, fade, age);
            case CIRCULAR -> drawCircular(buffer, matrix, origin, forward, right, scale, fade, age);
            case SHIFTING_FLOW -> drawFlow(buffer, matrix, origin, forward, right, scale, fade, age);
            case SEA_OF_HAZE -> drawSea(buffer, matrix, origin, forward, right, scale, fade, age);
            case LUNAR -> drawLunar(buffer, matrix, origin, forward, right, scale, fade, age);
            case FINISHER -> drawFinisher(buffer, matrix, origin, forward, right, scale, fade, age);
            case OBSCURING -> drawObscuring(buffer, matrix, origin, forward, right, scale, fade, age);
        }
        VfxPixelRender.finish(buffer);
    }

    private void drawThrust(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                            float s, float fade, float age) {
        float length = 7.5f * s;
        // Distant Haze: low clouds strung out along the lunge, thinning toward the tip.
        for (int i = 0; i < 4; i++) {
            float t = 0.12f + i * 0.28f;
            Vec3 c = o.add(f.scale(t * length)).add(up.scale(0.5 * s));
            cloud(b, m, c, f, r, (1.15f - t * 0.5f) * s, 14, 71L + i * 17L, fade, age);
        }
        mistRibbon(b, m, 26, t -> o.add(f.scale(t * length))
                        .add(r.scale(Math.sin(t * Math.PI * 3.0 + age * 0.12) * 0.20 * s))
                        .add(up.scale((0.48 + Math.sin(t * Math.PI) * 0.25) * s)),
                r, t -> (float) ((0.64 - t * 0.42) * s), fade, age);
        bladeRibbon(b, m, 24, t -> o.add(up.scale(0.84 * s)).add(f.scale(t * length)),
                r, t -> (float) ((0.18 - t * 0.10) * s), fade);
    }

    private void drawEightLayer(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                float s, float fade, float age) {
        float saved = reveal;
        Vec3 center = o.add(up.scale(1.05 * s));
        float radius = 3.15f * s;
        // Haze body the eight blades fan out of.
        cloud(b, m, center, f, r, radius * 0.95f, 30, 211L, fade, age);
        for (int i = 0; i < 8; i++) {
            reveal = clamp((saved - i * 0.075f) / (1.0f - i * 0.075f));
            if (reveal <= 0.0f) continue;
            double angle = Math.PI * i / 8.0;
            Vec3 radial = f.scale(Math.cos(angle)).add(r.scale(Math.sin(angle)));
            Vec3 tangent = radial.cross(up).normalize();
            float tilt = i % 2 == 0 ? 0.72f : -0.72f;
            bladeRibbon(b, m, 18, t -> {
                        double chord = (t * 2.0 - 1.0) * radius;
                        double vertical = chord * tilt;
                        double normalization = 1.0 / Math.sqrt(1.0 + tilt * tilt);
                        return center.add(radial.scale(chord * normalization))
                                .add(up.scale(vertical * normalization));
                    }, tangent, t -> (float) ((0.25 - Math.abs(t - 0.5) * 0.14) * s),
                    fade * (1.0f - i * 0.035f));
        }
        reveal = saved;
        mistRing(b, m, center, f, r, radius, 0.38f * s, 26, fade * 0.48f, age);
    }

    private void drawCircular(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                              float s, float fade, float age) {
        Vec3 center = o.add(up.scale(0.82 * s));
        // Scattering splash: haze thrown outward filling the disc, densest at the rim.
        cloud(b, m, center, f, r, 3.8f * s, 34, 331L, fade, age);
        cloud(b, m, center.add(up.scale(0.5 * s)), f, r, 2.4f * s, 16, 337L, fade * 0.8f, age);
        mistRing(b, m, center, f, r, 3.8f * s, 0.82f * s, 30, fade, age);
        float saved = reveal;
        reveal = clamp((saved - 0.12f) / 0.88f);
        ring(b, m, center.add(up.scale(0.08 * s)), f, r, 3.25f * s, 0.18f * s,
                28, color(WHITE, fade * 0.94f));
        reveal = saved;
    }

    private void drawFlow(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                          float s, float fade, float age) {
        // Haze streaming along the shifting slash path.
        for (int i = 0; i < 3; i++) {
            float t = 0.15f + i * 0.34f;
            Vec3 c = o.add(f.scale((-1.2 + t * 7.8) * s))
                    .add(r.scale(Math.sin(t * Math.PI * 2.4) * 0.52 * s))
                    .add(up.scale((0.34 + Math.sin(t * Math.PI) * 0.70) * s));
            cloud(b, m, c, f, r, (1.3f - t * 0.4f) * s, 14, 409L + i * 23L, fade, age);
        }
        mistRibbon(b, m, 28, t -> o.add(f.scale((-1.2 + t * 7.8) * s))
                        .add(r.scale(Math.sin(t * Math.PI * 2.4 + age * 0.10) * 0.52 * s))
                        .add(up.scale((0.34 + Math.sin(t * Math.PI) * 0.70) * s)),
                r, t -> (float) ((0.78 - t * 0.36) * s), fade, age);
        float saved = reveal;
        reveal = clamp((saved - 0.35f) / 0.65f);
        bladeRibbon(b, m, 18, t -> o.add(f.scale((1.0 + t * 4.6) * s))
                        .add(r.scale((t - 0.5) * 3.5 * s)).add(up.scale((0.45 + t * 0.65) * s)),
                r, t -> (float) ((0.25 - t * 0.10) * s), fade);
        reveal = saved;
    }

    private void drawSea(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                         float s, float fade, float age) {
        // A low, wide sea of clouds strung out ahead — the haze bank the slashes ride over.
        for (int i = 0; i < 5; i++) {
            Vec3 c = o.add(f.scale((-1.0 + i * 2.0) * s)).add(up.scale(0.25 * s));
            cloud(b, m, c, f, r, 2.2f * s, 22, 523L + i * 29L, fade * 0.9f, age + i * 2);
        }
        for (int layer = -2; layer <= 2; layer++) {
            final int lane = layer;
            float laneFade = fade * (lane == 0 ? 0.92f : 0.58f);
            mistRibbon(b, m, 26, t -> o.add(f.scale((-1.4 + t * 8.2) * s))
                            .add(r.scale((lane * 0.68 + Math.sin(t * Math.PI * 3.0 + lane) * 0.34) * s))
                            .add(up.scale((0.18 + Math.sin(t * Math.PI * 2.0 + lane) * 0.18) * s)),
                    r, t -> (float) ((0.48 + Math.sin(t * Math.PI) * 0.28) * s), laneFade, age + lane * 2);
        }
    }

    private void drawLunar(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                           float s, float fade, float age) {
        Vec3 center = o.add(up.scale(1.15 * s)).subtract(f.scale(0.25 * s));
        // Dispersing mist filling the crescent's swept arc.
        for (int i = 0; i < 6; i++) {
            double ang = Math.toRadians(-155 + 245.0 * (i + 0.5) / 6.0);
            Vec3 c = center.add(f.scale(Math.cos(ang) * 3.6 * s)).add(up.scale(Math.sin(ang) * 3.6 * s));
            cloud(b, m, c, f, r, 1.4f * s, 12, 617L + i * 31L, fade * 0.85f, age);
        }
        arc(b, m, center, f, up, 4.0f * s, 0.74f * s, -155, 245, 28, fade, age);
        float saved = reveal;
        reveal = clamp((saved - 0.14f) / 0.86f);
        arc(b, m, center, f, up, 3.48f * s, 0.16f * s, -155, 245, 26, fade * 0.92f, age + 4);
        reveal = saved;
    }

    private void drawFinisher(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                              float s, float fade, float age) {
        Vec3 center = o.add(up.scale(0.85 * s));
        // Haze erupting around the ring and up the rising column.
        cloud(b, m, center, f, r, 4.6f * s, 30, 743L, fade, age);
        cloud(b, m, center.add(up.scale(1.6 * s)), f, r, 1.6f * s, 12, 751L, fade * 0.8f, age);
        mistRing(b, m, center, f, r, 4.8f * s, 0.92f * s, 30, fade, age);
        bladeRibbon(b, m, 22, t -> center.subtract(up.scale(2.8 * s)).add(up.scale(t * 5.8 * s))
                        .add(r.scale(Math.sin(t * Math.PI) * 0.25 * s)),
                r, t -> (float) ((0.30 - Math.abs(t - 0.5) * 0.18) * s), fade);
    }

    private void drawObscuring(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                               float s, float fade, float age) {
        // A thick, blinding cloud bank — dense overlapping haze at several heights, not thin rings.
        for (int i = 0; i < 4; i++) {
            Vec3 c = o.add(up.scale((0.4 + i * 0.7) * s));
            cloud(b, m, c, f, r, (3.0f + i * 0.6f) * s, 40, 859L + i * 37L, fade, age * (i % 2 == 0 ? 1 : -1));
        }
        float saved = reveal;
        for (int layer = 0; layer < 4; layer++) {
            reveal = clamp((saved - layer * 0.08f) / (1.0f - layer * 0.08f));
            float radius = (1.8f + layer * 1.15f) * s;
            Vec3 center = o.add(up.scale((0.20 + layer * 0.34) * s));
            mistRing(b, m, center, f, r, radius, (0.72f - layer * 0.08f) * s,
                    28, fade * (0.86f - layer * 0.12f), age * (layer % 2 == 0 ? 1 : -1));
        }
        reveal = saved;
    }

    /**
     * Soft volumetric haze — a cluster of low-alpha pixel puffs. This is what makes Mist read as mist
     * (fog/cloud) instead of a thin water ribbon; overlapping translucent puffs build up a soft body.
     */
    private void cloud(BufferBuilder b, Matrix4f m, Vec3 c, Vec3 f, Vec3 r,
                       float radius, int puffs, long seed, float fade, float age) {
        float grow = 0.45f + 0.55f * reveal;
        for (int i = 0; i < puffs; i++) {
            float a1 = hash(seed + i * 2654435761L);
            float a2 = hash(seed + i * 40503L + 7);
            float a3 = hash(seed + i * 92821L + 13);
            float a4 = hash(seed + i * 6151L + 17);
            double dist = Math.sqrt(a1) * radius * grow;
            double ang = a2 * Math.PI * 2.0 + age * 0.02;
            double lift = (a3 - 0.5) * radius * 0.7;
            Vec3 p = c.add(f.scale(Math.cos(ang) * dist))
                    .add(r.scale(Math.sin(ang) * dist))
                    .add(up.scale(lift));
            float pr = (0.22f + a4 * 0.5f) * radius * 0.42f;
            int base = (i % 5 == 0) ? PALE : (i % 2 == 0) ? MID : DEEP;
            int col = color(base, fade * (0.10f + 0.14f * a4));
            Vec3 e1 = f.scale(pr), e2 = r.scale(pr);
            quad(b, m, p.subtract(e1).subtract(e2), p.add(e1).subtract(e2),
                    p.add(e1).add(e2), p.subtract(e1).add(e2), col);
        }
    }

    private static float hash(long n) {
        n ^= n >>> 33; n *= 0xff51afd7ed558ccdL;
        n ^= n >>> 33; n *= 0xc4ceb9fe1a85ec53L;
        n ^= n >>> 33;
        return (n & 0xFFFFFFL) / (float) 0x1000000L;
    }

    private void mistRibbon(BufferBuilder b, Matrix4f m, int segments, DoubleFunction<Vec3> path,
                            Vec3 widthAxis, DoubleFunction<Float> halfWidth, float fade, float age) {
        ribbon(b, m, segments, path, widthAxis, halfWidth, color(INK, fade * 0.56f), 1.0f);
        ribbon(b, m, segments, t -> path.apply(t).add(up.scale(0.08)), widthAxis,
                t -> halfWidth.apply(t) * 0.76f, color(MID, fade * 0.82f), 0.92f);
        ribbon(b, m, segments, t -> path.apply(t).add(up.scale(0.13 + Math.sin(t * 15.0 + age * 0.18) * 0.025)),
                widthAxis, t -> halfWidth.apply(t) * 0.34f, color(PALE, fade), 0.84f);
    }

    private void bladeRibbon(BufferBuilder b, Matrix4f m, int segments, DoubleFunction<Vec3> path,
                             Vec3 widthAxis, DoubleFunction<Float> halfWidth, float fade) {
        ribbon(b, m, segments, path, widthAxis, halfWidth, color(DEEP, fade), 1.0f);
        ribbon(b, m, segments, t -> path.apply(t).add(up.scale(0.045)), widthAxis,
                t -> halfWidth.apply(t) * 0.45f, color(WHITE, fade), 0.94f);
    }

    private void mistRing(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 a, Vec3 c,
                          float radius, float width, int segments, float fade, float age) {
        ring(b, m, center, a, c, radius, width, segments, color(INK, fade * 0.52f));
        ring(b, m, center.add(up.scale(0.07)), a, c, radius, width * 0.66f,
                segments, color(MID, fade * 0.82f));
        ring(b, m, center.add(up.scale(0.12)), a, c, radius * 0.98f, width * 0.24f,
                segments, color(PALE, fade));
    }

    private void arc(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 a, Vec3 c, float radius,
                     float width, float start, float sweep, int segments, float fade, float age) {
        arcBand(b, m, center, a, c, radius, width, start, sweep, segments, color(INK, fade * 0.58f));
        arcBand(b, m, center, a, c, radius, width * 0.64f, start, sweep, segments, color(MID, fade * 0.86f));
        arcBand(b, m, center, a, c, radius, width * 0.22f, start, sweep, segments, color(PALE, fade));
    }

    private void drawHistory(BufferBuilder b, Matrix4f m, VfxInstance instance, Camera camera,
                             Vec3 right, float s, float fade) {
        var points = instance.originHistory();
        Vec3 currentOrigin = instance.origin();
        for (int i = 1; i < points.size(); i++) {
            if (points.get(i).subtract(currentOrigin).dot(instance.direction()) > 0.12) continue;
            Vec3 a = points.get(i - 1).subtract(camera.getPosition()).add(up.scale(0.40 * s));
            Vec3 c = points.get(i).subtract(camera.getPosition()).add(up.scale(0.40 * s));
            if (a.distanceToSqr(c) < 0.0025) continue;
            float life = i / (float) points.size();
            float width = (0.28f + life * 0.52f) * s;
            quad(b, m, a.subtract(right.scale(width)), c.subtract(right.scale(width * 0.78)),
                    c.add(right.scale(width * 0.78)), a.add(right.scale(width)),
                    color(i % 3 == 0 ? PALE : MID, fade * life * 0.52f));
        }
    }

    private void ribbon(BufferBuilder b, Matrix4f m, int segments, DoubleFunction<Vec3> path,
                        Vec3 widthAxis, DoubleFunction<Float> halfWidth, int color, float revealScale) {
        int count = Math.min(segments, 30);
        float visible = reveal * revealScale * count;
        int end = Math.min(count, (int) Math.ceil(visible));
        for (int i = 0; i < end; i++) {
            double t0 = i / (double) count;
            double t1 = (i + Math.min(1.0f, visible - i)) / count;
            Vec3 p0 = path.apply(t0), p1 = path.apply(t1);
            Vec3 w0 = widthAxis.scale(halfWidth.apply(t0)), w1 = widthAxis.scale(halfWidth.apply(t1));
            quad(b, m, p0.subtract(w0), p1.subtract(w1), p1.add(w1), p0.add(w0), shade(color, i, count));
        }
    }

    private void ring(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 a, Vec3 c,
                      float radius, float width, int segments, int color) {
        arcBand(b, m, center, a, c, radius, width, -90, 360, segments, color);
    }

    private void arcBand(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 a, Vec3 c,
                         float radius, float width, float start, float sweep, int segments, int color) {
        int count = Math.min(segments, 30);
        float visible = reveal * count;
        int end = Math.min(count, (int) Math.ceil(visible));
        float inner = Math.max(0.0f, radius - width * 0.5f), outer = radius + width * 0.5f;
        for (int i = 0; i < end; i++) {
            float part = Math.min(1.0f, visible - i);
            double a0 = Math.toRadians(start + sweep * i / count);
            double a1 = Math.toRadians(start + sweep * (i + part) / count);
            quad(b, m, ellipse(center, a, c, inner, a0), ellipse(center, a, c, inner, a1),
                    ellipse(center, a, c, outer, a1), ellipse(center, a, c, outer, a0), shade(color, i, count));
        }
    }

    private static int shade(int color, int segment, int count) {
        float t = (segment + 0.5f) / count;
        if (t < 0.18f) return VfxPixelRender.mixRgb(color, INK, 0.38f);
        if (t > 0.72f && t < 0.88f) return VfxPixelRender.mixRgb(color, WHITE, 0.24f);
        return color;
    }

    private static Vec3 ellipse(Vec3 center, Vec3 a, Vec3 b, float radius, double angle) {
        return center.add(a.scale(Math.cos(angle) * radius)).add(b.scale(Math.sin(angle) * radius));
    }

    private static Vec3 rightOf(Vec3 forward) {
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        return right.lengthSqr() > 1.0E-6 ? right.normalize() : new Vec3(1, 0, 0);
    }

    private static void quad(BufferBuilder b, Matrix4f m, Vec3 a, Vec3 c, Vec3 d, Vec3 e, int color) {
        VfxPixelRender.quad(b, m, (float) a.x, (float) a.y, (float) a.z,
                (float) c.x, (float) c.y, (float) c.z, (float) d.x, (float) d.y, (float) d.z,
                (float) e.x, (float) e.y, (float) e.z, color);
    }

    private static float clamp(float value) { return Math.max(0.0f, Math.min(1.0f, value)); }
    private static float smooth(float value) { return value * value * (3.0f - 2.0f * value); }
    private static int color(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        return (a << 24) | rgb;
    }
}
