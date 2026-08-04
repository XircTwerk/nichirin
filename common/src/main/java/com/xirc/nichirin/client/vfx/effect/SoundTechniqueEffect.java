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

/** Beat-driven Sound Breathing geometry rendered through the shared pixel pass. */
public final class SoundTechniqueEffect implements VfxEffect {
    public enum Style {
        RESONDING(34), RHYTHMIC(34), ROAR(28), STRINGS(42), TEMPO(30), IMPACT(18);
        private final int lifetime;
        Style(int lifetime) { this.lifetime = lifetime; }
    }

    private static final int INK = 0x392E33;
    private static final int CORAL = 0xFF5D3A;
    private static final int ORANGE = 0xFF9135;
    private static final int GOLD = 0xF9CD75;
    private static final int CYAN = 0xACDFDF;
    private static final int PALE = 0xDDFFFF;
    private static final int WHITE = 0xFFFFFF;

    private final Style style;
    private float reveal;
    private Vec3 up = new Vec3(0, 1, 0);

    public SoundTechniqueEffect(Style style) { this.style = style; }

    @Override
    public int lifetimeTicks() { return style.lifetime; }

    @Override
    public void render(VfxInstance instance, PoseStack poseStack, Camera camera, float partialTick) {
        float age = instance.ageTicks() + partialTick;
        reveal = smooth(clamp(age / (style == Style.STRINGS ? 15.0f : 8.0f)));
        float fade = 1.0f - clamp((age - style.lifetime * 0.64f) / (style.lifetime * 0.36f));
        if (fade <= 0.0f) return;
        Vec3 o = instance.origin(partialTick).subtract(camera.getPosition());
        Vec3 f = instance.direction().normalize();
        Vec3 r = rightOf(f);
        up = r.cross(f).normalize();
        float s = instance.scale();
        Matrix4f m = poseStack.last().pose();
        BufferBuilder b = VfxPixelRender.beginQuads();
        if (style == Style.RHYTHMIC || style == Style.STRINGS) drawHistory(b, m, instance, camera, r, s, fade);
        switch (style) {
            case RESONDING -> drawResounding(b, m, o, f, r, s, fade, age);
            case RHYTHMIC -> drawRhythmic(b, m, o, f, r, s, fade, age);
            case ROAR -> drawRoar(b, m, o, f, r, s, fade, age);
            case STRINGS -> drawStrings(b, m, o, f, r, s, fade, age);
            case TEMPO -> drawTempo(b, m, o, f, r, s, fade, age);
            case IMPACT -> drawImpact(b, m, o, f, r, s, fade, age);
        }
        VfxPixelRender.finish(b);
    }

    private void drawResounding(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                float s, float fade, float age) {
        Vec3 center = o.add(up.scale(0.82 * s));
        ring(b, m, center, f, r, 2.65f * s, 0.48f * s, 28, color(INK, fade));
        ring(b, m, center.add(up.scale(0.08 * s)), f, r, 2.48f * s, 0.20f * s, 28, color(CORAL, fade));
        ring(b, m, center.add(up.scale(0.14 * s)), f, r, 2.18f * s, 0.10f * s, 24, color(PALE, fade));
        for (int blade = 0; blade < 2; blade++) {
            final int side = blade == 0 ? -1 : 1;
            ribbon(b, m, 24, t -> {
                double angle = side * (t * Math.PI * 2.2 + age * 0.12);
                double radius = (0.55 + t * 2.35) * s;
                return center.add(f.scale(Math.cos(angle) * radius)).add(r.scale(Math.sin(angle) * radius))
                        .add(up.scale(Math.sin(t * Math.PI) * 0.34 * s));
            }, r, t -> (float) ((0.18 - t * 0.08) * s), color(side < 0 ? CORAL : CYAN, fade));
        }
    }

    private void drawRhythmic(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                              float s, float fade, float age) {
        Vec3 behind = o.subtract(f.scale(0.35 * s)).add(up.scale(0.28 * s));
        for (int beat = 0; beat < 4; beat++) {
            float local = clamp((reveal - beat * 0.14f) / (1.0f - beat * 0.14f));
            if (local <= 0.0f) continue;
            Vec3 center = behind.subtract(f.scale(beat * 1.15 * s));
            float radius = (0.55f + beat * 0.28f) * s;
            float saved = reveal; reveal = local;
            ring(b, m, center, r, up, radius, 0.13f * s, 16,
                    color(beat % 2 == 0 ? CORAL : CYAN, fade * (1.0f - beat * 0.14f)));
            reveal = saved;
        }
        ribbon(b, m, 20, t -> o.subtract(f.scale(t * 6.8 * s))
                        .add(r.scale(Math.sin(t * Math.PI * 5.0 + age * 0.10) * 0.34 * s))
                        .add(up.scale((0.35 + Math.sin(t * Math.PI) * 0.42) * s)),
                r, t -> (float) ((0.24 - t * 0.12) * s), color(PALE, fade * 0.86f));
    }

    private void drawRoar(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                          float s, float fade, float age) {
        Vec3 center = o.add(up.scale(0.10 * s));
        for (int wave = 0; wave < 5; wave++) {
            float local = clamp((reveal - wave * 0.09f) / (1.0f - wave * 0.09f));
            float saved = reveal; reveal = local;
            ring(b, m, center.add(up.scale(wave * 0.06 * s)), f, r,
                    (1.25f + wave * 1.50f) * s, (0.38f - wave * 0.045f) * s, 30,
                    color(wave == 0 ? GOLD : wave % 2 == 0 ? CORAL : ORANGE,
                            fade * (1.0f - wave * 0.11f)));
            reveal = saved;
        }
        burst(b, m, center, f, r, 0.35f * s, 7.2f * s, 16, color(GOLD, fade * 0.82f));

        // Ground fractures spread out from the twin-blade impact instead of reading as a clean ring.
        for (int crack = 0; crack < 12; crack++) {
            final int crackIndex = crack;
            final double angle = Math.PI * 2.0 * crack / 12.0 + (crack % 3) * 0.08;
            Vec3 radial = f.scale(Math.cos(angle)).add(r.scale(Math.sin(angle)));
            Vec3 tangent = f.scale(-Math.sin(angle)).add(r.scale(Math.cos(angle)));
            double length = (3.8 + (crack % 4) * 0.85) * s * reveal;
            ribbon(b, m, 12, t -> center.add(radial.scale(t * length))
                            .add(tangent.scale(Math.sin(t * Math.PI * 3.0 + crackIndex) * 0.18 * s))
                            .add(up.scale(0.035 * s)),
                    tangent, t -> (float) ((0.13 - t * 0.07) * s),
                    color(crackIndex % 3 == 0 ? PALE : INK, fade * 0.88f));
        }

        // Deterministic chunks follow ballistic arcs so the slam throws readable rubble, not noise.
        float flight = Math.min(age, 18.0f);
        for (int chunk = 0; chunk < 20; chunk++) {
            double angle = chunk * 2.399963229728653;
            double speed = (0.16 + (chunk % 5) * 0.022) * s;
            double lift = (0.22 + (chunk % 4) * 0.045) * s;
            double y = lift * flight - 0.0125 * flight * flight;
            if (y < 0.0) continue;
            Vec3 radial = f.scale(Math.cos(angle)).add(r.scale(Math.sin(angle)));
            Vec3 p = center.add(radial.scale(speed * flight)).add(up.scale(y + 0.18 * s));
            float size = (0.13f + (chunk % 4) * 0.035f) * s;
            diamond(b, m, p, r, up, size, size * 1.25f,
                    color(chunk % 4 == 0 ? GOLD : chunk % 2 == 0 ? INK : CORAL, fade * 0.92f));
        }
    }

    private void drawStrings(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                             float s, float fade, float age) {
        float length = 10.5f * s;
        for (int string = -3; string <= 3; string++) {
            final int lane = string;
            ribbon(b, m, 28, t -> o.add(f.scale(t * length)).add(r.scale(lane * 0.42 * s))
                            .add(up.scale((0.42 + Math.sin(t * Math.PI * 5.0 + lane + age * 0.12) * 0.16) * s)),
                    r, t -> (float) (0.045 * s), color(lane == 0 ? WHITE : CYAN, fade * (lane == 0 ? 1.0f : 0.76f)));
        }
        for (int node = 1; node <= 5; node++) {
            Vec3 center = o.add(f.scale(node * 1.75 * s)).add(up.scale(0.42 * s));
            diamond(b, m, center, r, up, 0.16f * s, 0.24f * s,
                    color(node % 2 == 0 ? CORAL : PALE, fade * 0.88f));
        }
    }

    private void drawTempo(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                           float s, float fade, float age) {
        Vec3 center = o.add(f.scale(2.2 * s)).add(up.scale(0.72 * s));
        arc(b, m, center, f, r, 3.4f * s, 0.48f * s, -125, 250, 28, color(CORAL, fade));
        float saved = reveal; reveal = clamp((saved - 0.20f) / 0.80f);
        arc(b, m, center.add(up.scale(0.10 * s)), f, r, 2.95f * s, 0.16f * s,
                -125, 250, 26, color(WHITE, fade));
        for (int beat = -1; beat <= 1; beat++) {
            Vec3 mark = center.add(f.scale((2.4 + Math.abs(beat) * 0.55) * s)).add(r.scale(beat * 1.6 * s));
            ring(b, m, mark, f, r, 0.72f * s, 0.16f * s, 14,
                    color(beat == 0 ? GOLD : ORANGE, fade * 0.84f));
        }
        reveal = saved;
    }

    private void drawImpact(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                            float s, float fade, float age) {
        ring(b, m, o, f, r, 3.8f * s, 0.38f * s, 28, color(CORAL, fade));
        ring(b, m, o.add(up.scale(0.08 * s)), f, r, 2.75f * s, 0.16f * s, 24, color(PALE, fade));
        burst(b, m, o, f, r, 0.28f * s, 4.2f * s, 12, color(GOLD, fade * 0.82f));
    }

    private void drawHistory(BufferBuilder b, Matrix4f m, VfxInstance instance, Camera camera,
                             Vec3 right, float s, float fade) {
        var points = instance.originHistory();
        Vec3 current = instance.origin();
        for (int i = 1; i < points.size(); i++) {
            if (points.get(i).subtract(current).dot(instance.direction()) > 0.12) continue;
            Vec3 a = points.get(i - 1).subtract(camera.getPosition()).add(up.scale(0.46 * s));
            Vec3 c = points.get(i).subtract(camera.getPosition()).add(up.scale(0.46 * s));
            if (a.distanceToSqr(c) < 0.0025) continue;
            float life = i / (float) points.size();
            float width = (0.14f + life * 0.30f) * s;
            quad(b, m, a.subtract(right.scale(width)), c.subtract(right.scale(width * 0.8)),
                    c.add(right.scale(width * 0.8)), a.add(right.scale(width)),
                    color(i % 2 == 0 ? CORAL : CYAN, fade * life * 0.68f));
        }
    }

    private void ribbon(BufferBuilder b, Matrix4f m, int segments, DoubleFunction<Vec3> path,
                        Vec3 widthAxis, DoubleFunction<Float> width, int color) {
        int count = Math.min(30, segments), end = Math.min(count, (int) Math.ceil(reveal * count));
        for (int i = 0; i < end; i++) {
            double t0 = i / (double) count, t1 = (i + Math.min(1.0f, reveal * count - i)) / count;
            Vec3 p0 = path.apply(t0), p1 = path.apply(t1);
            Vec3 w0 = widthAxis.scale(width.apply(t0)), w1 = widthAxis.scale(width.apply(t1));
            quad(b, m, p0.subtract(w0), p1.subtract(w1), p1.add(w1), p0.add(w0), shade(color, i, count));
        }
    }

    private void ring(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 a, Vec3 c,
                      float radius, float width, int segments, int color) {
        arc(b, m, center, a, c, radius, width, -90, 360, segments, color);
    }

    private void arc(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 a, Vec3 c, float radius,
                     float width, float start, float sweep, int segments, int color) {
        int count = Math.min(30, segments), end = Math.min(count, (int) Math.ceil(reveal * count));
        float inner = radius - width * 0.5f, outer = radius + width * 0.5f;
        for (int i = 0; i < end; i++) {
            float part = Math.min(1.0f, reveal * count - i);
            double a0 = Math.toRadians(start + sweep * i / count);
            double a1 = Math.toRadians(start + sweep * (i + part) / count);
            quad(b, m, ellipse(center, a, c, inner, a0), ellipse(center, a, c, inner, a1),
                    ellipse(center, a, c, outer, a1), ellipse(center, a, c, outer, a0), shade(color, i, count));
        }
    }

    private void burst(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 a, Vec3 c,
                       float inner, float length, int count, int color) {
        int visible = Math.min(count, (int) Math.ceil(reveal * count));
        for (int i = 0; i < visible; i++) {
            double angle = Math.PI * 2.0 * i / count;
            Vec3 radial = a.scale(Math.cos(angle)).add(c.scale(Math.sin(angle)));
            Vec3 tangent = a.scale(-Math.sin(angle)).add(c.scale(Math.cos(angle)));
            Vec3 root = center.add(radial.scale(inner));
            Vec3 tip = center.add(radial.scale(inner + length * reveal * (i % 3 == 0 ? 1.0 : 0.66)));
            Vec3 side = tangent.scale(length * 0.045);
            quad(b, m, root.subtract(side), tip, tip, root.add(side), color);
        }
    }

    private static void diamond(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 h, Vec3 v,
                                float w, float height, int color) {
        quad(b, m, center.subtract(h.scale(w)), center.add(v.scale(height)),
                center.add(h.scale(w)), center.subtract(v.scale(height)), color);
    }

    private static Vec3 ellipse(Vec3 center, Vec3 a, Vec3 b, float radius, double angle) {
        return center.add(a.scale(Math.cos(angle) * radius)).add(b.scale(Math.sin(angle) * radius));
    }

    private static int shade(int color, int segment, int count) {
        float t = (segment + 0.5f) / count;
        if (t < 0.16f) return VfxPixelRender.mixRgb(color, INK, 0.42f);
        if (t > 0.70f && t < 0.88f) return VfxPixelRender.mixRgb(color, WHITE, 0.24f);
        return color;
    }

    private static Vec3 rightOf(Vec3 f) {
        Vec3 r = f.cross(new Vec3(0, 1, 0));
        return r.lengthSqr() > 1.0E-6 ? r.normalize() : new Vec3(1, 0, 0);
    }

    private static void quad(BufferBuilder b, Matrix4f m, Vec3 a, Vec3 c, Vec3 d, Vec3 e, int color) {
        VfxPixelRender.quad(b, m, (float) a.x, (float) a.y, (float) a.z,
                (float) c.x, (float) c.y, (float) c.z, (float) d.x, (float) d.y, (float) d.z,
                (float) e.x, (float) e.y, (float) e.z, color);
    }

    private static float clamp(float v) { return Math.max(0.0f, Math.min(1.0f, v)); }
    private static float smooth(float v) { return v * v * (3.0f - 2.0f * v); }
    private static int color(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        return (a << 24) | rgb;
    }
}
