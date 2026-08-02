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

/** Needle, wing, and segmented Insect Breathing geometry rendered through the pixel pass. */
public final class InsectTechniqueEffect implements VfxEffect {
    public enum Style {
        QUICK_STING(20), BEE_STING(32), BUTTERFLY(28), BUTTERFLY_DASH(34),
        DRAGONFLY(30), CENTIPEDE(30), IMPACT(18);
        private final int lifetime;
        Style(int lifetime) { this.lifetime = lifetime; }
    }

    private static final int INK = 0x503267;
    private static final int PURPLE = 0x7C5C92;
    private static final int LILAC = 0xA784BF;
    private static final int GREEN = 0x54CF96;
    private static final int MINT = 0xD0FFF0;
    private static final int WHITE = 0xEBFEF8;

    private final Style style;
    private float reveal;
    private Vec3 up = new Vec3(0, 1, 0);

    public InsectTechniqueEffect(Style style) { this.style = style; }

    @Override
    public int lifetimeTicks() { return style.lifetime; }

    @Override
    public void render(VfxInstance instance, PoseStack poseStack, Camera camera, float partialTick) {
        float age = instance.ageTicks() + partialTick;
        reveal = smooth(clamp(age / (style == Style.CENTIPEDE ? 15.0f : 8.0f)));
        float fade = 1.0f - clamp((age - style.lifetime * 0.64f) / (style.lifetime * 0.36f));
        if (fade <= 0.0f) return;
        Vec3 o = instance.origin(partialTick).subtract(camera.getPosition());
        Vec3 f = instance.direction().normalize();
        Vec3 r = rightOf(f);
        up = r.cross(f).normalize();
        float s = instance.scale();
        Matrix4f m = poseStack.last().pose();
        BufferBuilder b = VfxPixelRender.beginQuads();
        if (style == Style.BEE_STING || style == Style.BUTTERFLY_DASH || style == Style.CENTIPEDE) {
            drawHistory(b, m, instance, camera, r, s, fade);
        }
        switch (style) {
            case QUICK_STING -> drawQuickSting(b, m, o, f, r, s, fade);
            case BEE_STING -> drawBee(b, m, o, f, r, s, fade, age);
            case BUTTERFLY -> drawButterflyLeap(b, m, o, f, r, s, fade, age);
            case BUTTERFLY_DASH -> drawButterflyDash(b, m, o, f, r, s, fade, age);
            case DRAGONFLY -> drawDragonfly(b, m, o, f, r, s, fade, age);
            case CENTIPEDE -> drawCentipede(b, m, o, f, r, s, fade, age);
            case IMPACT -> drawImpact(b, m, o, f, r, s, fade);
        }
        VfxPixelRender.finish(b);
    }

    private void drawQuickSting(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                float s, float fade) {
        boolean firstPerson = VfxPixelRender.isOwnFirstPerson();
        float length = (firstPerson ? 5.4f : 7.2f) * s;
        Vec3 start = o.add(f.scale((firstPerson ? 1.35 : 0.0) * s)).add(up.scale(0.80 * s));
        needle(b, m, start, f, r, length, (firstPerson ? 0.14f : 0.26f) * s, fade);
        int ringCount = firstPerson ? 2 : 3;
        for (int ring = 0; ring < ringCount; ring++) {
            Vec3 center = start.add(f.scale((0.8 + ring * 1.35) * s));
            ellipseRing(b, m, center, r, up, (0.52f + ring * 0.20f) * s,
                    (firstPerson ? 0.065f : 0.10f) * s, 16,
                    color(ring == ringCount - 1 ? MINT : PURPLE, fade * (0.82f - ring * 0.10f)));
        }
    }

    private void drawBee(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                         float s, float fade, float age) {
        needle(b, m, o.add(up.scale(0.72 * s)), f, r, 9.0f * s, 0.30f * s, fade);
        Vec3 thorax = o.add(f.scale(0.55 * s)).add(up.scale(0.85 * s));
        for (int side : new int[]{-1, 1}) {
            Vec3 wingCenter = thorax.add(r.scale(side * 0.55 * s));
            arc(b, m, wingCenter, f, up, 1.20f * s, 0.24f * s,
                    side < 0 ? -155 : -25, side < 0 ? 130 : -130, 16,
                    color(MINT, fade * (0.72f + (float) Math.sin(age * 0.4) * 0.12f)));
        }
    }

    private void drawButterflyLeap(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                   float s, float fade, float age) {
        Vec3 center = o.add(up.scale(0.45 * s));
        for (int side : new int[]{-1, 1}) {
            final int wing = side;
            ribbon(b, m, 24, t -> center.add(up.scale((0.25 + t * 4.8) * s))
                            .add(f.scale((0.15 + Math.sin(t * Math.PI) * 1.35) * s))
                            .add(r.scale(wing * Math.sin(t * Math.PI) * (2.35 + Math.sin(age * 0.35) * 0.18) * s)),
                    r, t -> (float) ((0.12 + Math.sin(t * Math.PI) * 0.26) * s),
                    color(wing < 0 ? PURPLE : LILAC, fade * 0.82f));
        }
        for (int i = 0; i < 8; i++) {
            double t = (i + 0.5) / 8.0;
            Vec3 p = center.add(up.scale((0.35 + t * 4.4) * s))
                    .add(r.scale(Math.sin(t * Math.PI * 5.0 + age * 0.16) * 1.45 * s))
                    .add(f.scale(Math.cos(t * Math.PI * 3.0) * 0.65 * s));
            butterflyGlyph(b, m, p, r, up, (0.16f + (i % 3) * 0.035f) * s,
                    color(i % 2 == 0 ? MINT : LILAC, fade * 0.88f));
        }
    }

    private void drawButterflyDash(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                   float s, float fade, float age) {
        Vec3 start = o.add(f.scale(0.65 * s)).add(up.scale(0.72 * s));
        needle(b, m, start, f, r, 7.5f * s, 0.20f * s, fade);
        for (int side : new int[]{-1, 1}) {
            final int wing = side;
            ribbon(b, m, 25, t -> o.subtract(f.scale(t * 6.5 * s))
                            .add(r.scale(wing * (0.35 + Math.sin(t * Math.PI * 4.0 + age * 0.18) * 0.70) * s))
                            .add(up.scale((0.48 + Math.sin(t * Math.PI) * 0.55) * s)),
                    r, t -> (float) ((0.16 - t * 0.07) * s),
                    color(wing < 0 ? PURPLE : GREEN, fade * 0.72f));
        }
        for (int i = 0; i < 6; i++) {
            double t = (i + 1) / 6.0;
            Vec3 p = o.subtract(f.scale(t * 5.5 * s))
                    .add(r.scale(Math.sin(i * 2.3 + age * 0.16) * 0.9 * s))
                    .add(up.scale((0.45 + (i % 3) * 0.32) * s));
            butterflyGlyph(b, m, p, r, up, 0.15f * s, color(i % 2 == 0 ? MINT : LILAC, fade * 0.72f));
        }
    }

    private void drawDragonfly(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                               float s, float fade, float age) {
        Vec3 eye = o.add(f.scale(1.15 * s)).add(up.scale(1.05 * s));
        for (int ring = 0; ring < 3; ring++) {
            ellipseRing(b, m, eye, r, up, (0.72f + ring * 0.42f) * s,
                    0.13f * s, 18, color(ring == 2 ? LILAC : PURPLE, fade * (0.90f - ring * 0.14f)));
        }
        for (int stab = -2; stab <= 2; stab++) {
            Vec3 start = o.add(r.scale(stab * 0.34 * s)).add(up.scale((0.62 + Math.abs(stab) * 0.12) * s));
            needle(b, m, start, f, r, (5.2f + (2 - Math.abs(stab)) * 0.75f) * s,
                    0.13f * s, fade * (0.76f + (2 - Math.abs(stab)) * 0.06f));
        }
    }

    private void drawCentipede(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                               float s, float fade, float age) {
        DoubleFunction<Vec3> path = t -> o.subtract(f.scale(t * 8.0 * s))
                .add(r.scale(Math.sin(t * Math.PI * 4.0) * 1.55 * s))
                .add(up.scale((0.18 + Math.sin(t * Math.PI * 4.0) * 0.08) * s));
        ribbon(b, m, 30, path, r, t -> (float) ((0.28 - t * 0.13) * s), color(INK, fade * 0.92f));
        ribbon(b, m, 30, t -> path.apply(t).add(up.scale(0.07 * s)), r,
                t -> (float) ((0.11 - t * 0.045) * s), color(MINT, fade));
        int visibleSteps = Math.min(12, (int) Math.ceil(reveal * 12));
        for (int step = 0; step < visibleSteps; step++) {
            double t = (step + 0.45) / 12.0;
            Vec3 joint = path.apply(t);
            int side = step % 2 == 0 ? -1 : 1;
            Vec3 diagonal = r.scale(side * (0.50 + (step % 3) * 0.10) * s)
                    .add(f.scale(0.34 * s)).add(up.scale(0.20 * s));
            Vec3 width = r.scale(0.055 * s);
            quad(b, m, joint.subtract(diagonal).subtract(width), joint.add(diagonal).subtract(width),
                    joint.add(diagonal).add(width), joint.subtract(diagonal).add(width),
                    color(step % 3 == 0 ? GREEN : PURPLE, fade * (0.88f - (float) t * 0.35f)));
        }
    }

    private void butterflyGlyph(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 horizontal, Vec3 vertical,
                                float size, int glyphColor) {
        Vec3 body = vertical.scale(size * 0.85);
        Vec3 left = horizontal.scale(size);
        quad(b, m, center, center.add(body).subtract(left), center.add(vertical.scale(size * 0.15)),
                center.subtract(body).subtract(left.scale(0.72)), glyphColor);
        quad(b, m, center, center.add(body).add(left), center.add(vertical.scale(size * 0.15)),
                center.subtract(body).add(left.scale(0.72)), glyphColor);
    }

    private void drawImpact(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                            float s, float fade) {
        ellipseRing(b, m, o, f, r, 2.8f * s, 0.28f * s, 24, color(PURPLE, fade));
        ellipseRing(b, m, o.add(up.scale(0.08 * s)), f, r, 2.15f * s,
                0.12f * s, 22, color(MINT, fade));
        burst(b, m, o, f, r, 0.22f * s, 3.4f * s, 10, color(GREEN, fade * 0.78f));
    }

    private void needle(BufferBuilder b, Matrix4f m, Vec3 start, Vec3 direction, Vec3 side,
                        float length, float width, float fade) {
        ribbon(b, m, 24, t -> start.add(direction.scale(t * length)), side,
                t -> (float) (width * (1.0 - t * 0.82)), color(INK, fade));
        ribbon(b, m, 24, t -> start.add(direction.scale(t * length)).add(up.scale(0.05)), side,
                t -> (float) (width * 0.38 * (1.0 - t * 0.80)), color(MINT, fade));
    }

    private void drawHistory(BufferBuilder b, Matrix4f m, VfxInstance instance, Camera camera,
                             Vec3 right, float s, float fade) {
        var points = instance.originHistory();
        Vec3 current = instance.origin();
        for (int i = 1; i < points.size(); i++) {
            if (points.get(i).subtract(current).dot(instance.direction()) > 0.12) continue;
            Vec3 a = points.get(i - 1).subtract(camera.getPosition()).add(up.scale(0.42 * s));
            Vec3 c = points.get(i).subtract(camera.getPosition()).add(up.scale(0.42 * s));
            if (a.distanceToSqr(c) < 0.0025) continue;
            float life = i / (float) points.size(), width = (0.16f + life * 0.32f) * s;
            quad(b, m, a.subtract(right.scale(width)), c.subtract(right.scale(width * 0.78)),
                    c.add(right.scale(width * 0.78)), a.add(right.scale(width)),
                    color(i % 2 == 0 ? MINT : PURPLE, fade * life * 0.62f));
        }
    }

    private void ribbon(BufferBuilder b, Matrix4f m, int segments, DoubleFunction<Vec3> path,
                        Vec3 widthAxis, DoubleFunction<Float> width, int color) {
        int count = Math.min(30, segments), end = Math.min(count, (int) Math.ceil(reveal * count));
        for (int i = 0; i < end; i++) {
            double t0 = i / (double) count, t1 = (i + Math.min(1.0f, reveal * count - i)) / count;
            Vec3 p0 = path.apply(t0), p1 = path.apply(t1), w0 = widthAxis.scale(width.apply(t0)),
                    w1 = widthAxis.scale(width.apply(t1));
            quad(b, m, p0.subtract(w0), p1.subtract(w1), p1.add(w1), p0.add(w0), shade(color, i, count));
        }
    }

    private void ellipseRing(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 a, Vec3 c,
                             float radius, float width, int segments, int color) {
        arc(b, m, center, a, c, radius, width, -90, 360, segments, color);
    }

    private void arc(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 a, Vec3 c, float radius,
                     float width, float start, float sweep, int segments, int color) {
        int count = Math.min(30, segments), end = Math.min(count, (int) Math.ceil(reveal * count));
        float inner = radius - width * 0.5f, outer = radius + width * 0.5f;
        for (int i = 0; i < end; i++) {
            float part = Math.min(1.0f, reveal * count - i);
            double a0 = Math.toRadians(start + sweep * i / count),
                    a1 = Math.toRadians(start + sweep * (i + part) / count);
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
            Vec3 tip = center.add(radial.scale(inner + length * reveal * (i % 2 == 0 ? 1.0 : 0.68)));
            Vec3 side = tangent.scale(length * 0.04);
            quad(b, m, root.subtract(side), tip, tip, root.add(side), color);
        }
    }

    private static Vec3 ellipse(Vec3 center, Vec3 a, Vec3 b, float radius, double angle) {
        return center.add(a.scale(Math.cos(angle) * radius)).add(b.scale(Math.sin(angle) * radius));
    }

    private static int shade(int color, int segment, int count) {
        float t = (segment + 0.5f) / count;
        if (t < 0.16f) return VfxPixelRender.mixRgb(color, INK, 0.42f);
        if (t > 0.68f && t < 0.88f) return VfxPixelRender.mixRgb(color, WHITE, 0.22f);
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
