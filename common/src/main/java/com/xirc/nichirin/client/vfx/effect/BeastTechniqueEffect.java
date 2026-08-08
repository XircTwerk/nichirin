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

/** Paired, serrated Beast Breathing blade geometry rendered by the shared pixel pass. */
public final class BeastTechniqueEffect implements VfxEffect {
    public enum Style {
        PIERCE(18), X_SLICE(22), RUSH(30), DEVOUR(22), RAPID(24), CRAZY(32),
        PALISADE(24), AWARENESS(48), BENDY(22), WHIRL(28), THROW(34);

        private final int lifetime;
        Style(int lifetime) { this.lifetime = lifetime; }
    }

    private static final int INK = 0x171D22;
    private static final int STEEL = 0x619FAD;
    private static final int TEAL = 0x6CC1CF;
    private static final int ICE = 0xAAD1E0;
    private static final int WHITE = 0xE7F0ED;
    private static final int BONE = 0xB9B6A9;

    private final Style style;
    private Vec3 up = new Vec3(0, 1, 0);
    private float reveal;

    public BeastTechniqueEffect(Style style) { this.style = style; }

    @Override
    public int lifetimeTicks() { return style.lifetime; }

    @Override
    public void render(VfxInstance instance, PoseStack poseStack, Camera camera, float partialTick) {
        float age = instance.ageTicks() + partialTick;
        float revealTicks = switch (style) {
            case RUSH, THROW -> 15.0f;
            case AWARENESS -> 20.0f;
            default -> 8.0f;
        };
        reveal = smooth(clamp(age / revealTicks));
        float fade = 1.0f - clamp((age - style.lifetime * 0.64f) / (style.lifetime * 0.36f));
        if (fade <= 0.0f) return;

        Vec3 origin = instance.origin(partialTick).subtract(camera.getPosition());
        // A zero direction (aim-independent effects like Spatial Awareness) falls back to a fixed
        // world axis so the basis stays well-defined and the shape lies flat on the ground plane.
        Vec3 forward = instance.direction().lengthSqr() < 1.0E-6
                ? new Vec3(0.0, 0.0, 1.0) : instance.direction().normalize();
        Vec3 right = rightOf(forward);
        up = right.cross(forward).normalize();
        float scale = instance.scale();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = VfxPixelRender.beginQuads();

        if (style == Style.RUSH || style == Style.THROW) drawHistory(buffer, matrix, instance, camera, right, scale, fade);
        switch (style) {
            case PIERCE -> drawPierce(buffer, matrix, origin, forward, right, scale, fade);
            case X_SLICE -> drawX(buffer, matrix, origin, forward, right, scale, fade);
            case RUSH -> drawRush(buffer, matrix, origin, forward, right, scale, fade, age);
            case DEVOUR -> drawDevour(buffer, matrix, origin, forward, right, scale, fade);
            case RAPID -> drawRapid(buffer, matrix, origin, forward, right, scale, fade);
            case CRAZY -> drawCrazy(buffer, matrix, origin, forward, right, scale, fade, age);
            case PALISADE -> drawPalisade(buffer, matrix, origin, forward, right, scale, fade);
            case AWARENESS -> drawAwareness(buffer, matrix, origin, forward, right, scale, fade, age);
            case BENDY -> drawBendy(buffer, matrix, origin, forward, right, scale, fade);
            case WHIRL -> drawWhirl(buffer, matrix, origin, forward, right, scale, fade, age);
            case THROW -> drawThrow(buffer, matrix, origin, forward, right, scale, fade, age);
        }
        VfxPixelRender.finish(buffer);
    }

    private void drawPierce(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r, float s, float fade) {
        for (int side : new int[]{-1, 1}) {
            final int lane = side;
            serratedRibbon(b, m, 24, t -> o.add(r.scale(lane * 0.28 * s)).add(up.scale((0.72 + lane * 0.12) * s))
                            .add(f.scale(t * 7.2 * s)), r,
                    t -> (float) ((0.20 - t * 0.10) * s), fade, lane);
        }
        diamond(b, m, o.add(f.scale(7.2 * s)).add(up.scale(0.72 * s)), r, up,
                0.18f * s, 0.26f * s, color(WHITE, fade));
    }

    private void drawX(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r, float s, float fade) {
        Vec3 center = o.add(f.scale(3.4 * s)).add(up.scale(0.92 * s));
        for (int side : new int[]{-1, 1}) {
            final int diagonal = side;
            serratedRibbon(b, m, 22, t -> center
                            .add(r.scale((t - 0.5) * 5.6 * s))
                            .add(up.scale((t - 0.5) * 4.8 * diagonal * s)),
                    r, t -> (float) ((0.25 - Math.abs(t - 0.5) * 0.16) * s), fade, diagonal);
        }
    }

    private void drawRush(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                          float s, float fade, float age) {
        for (int side : new int[]{-1, 1}) {
            final int lane = side;
            serratedRibbon(b, m, 25, t -> o.add(f.scale((-1.5 + t * 7.8) * s))
                            .add(r.scale((lane * 0.62 + Math.sin(t * 14.0 + age * 0.14) * 0.15) * s))
                            .add(up.scale((0.35 + Math.sin(t * Math.PI) * 0.44) * s)),
                    r, t -> (float) ((0.42 - t * 0.16) * s), fade * 0.88f, lane);
        }
    }

    private void drawDevour(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r, float s, float fade) {
        for (int side : new int[]{-1, 1}) {
            float saved = reveal;
            reveal = clamp((saved - (side > 0 ? 0.12f : 0.0f)) / (side > 0 ? 0.88f : 1.0f));
            final int jaw = side;
            serratedRibbon(b, m, 22, t -> o.add(f.scale((0.5 + t * 4.8) * s))
                            .add(r.scale((1.7 - t * 2.8) * jaw * s))
                            .add(up.scale((0.64 + jaw * 0.38 + Math.sin(t * Math.PI) * 0.32) * s)),
                    r, t -> (float) ((0.30 - t * 0.10) * s), fade, jaw);
            reveal = saved;
        }
    }

    private void drawRapid(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r, float s, float fade) {
        float saved = reveal;
        for (int i = 0; i < 8; i++) {
            reveal = clamp((saved - i * 0.07f) / (1.0f - i * 0.07f));
            if (reveal <= 0.0f) continue;
            final int slash = i;
            int diagonal = i % 2 == 0 ? 1 : -1;
            serratedRibbon(b, m, 13, t -> o.add(f.scale((0.5 + t * 4.5) * s))
                            .add(r.scale(((t - 0.5) * 3.4 * diagonal + (slash - 3.5) * 0.18) * s))
                            .add(up.scale((0.60 + (0.5 - t) * 2.0 * diagonal + slash * 0.10) * s)),
                    r, t -> (float) (0.18 * s), fade * (1.0f - i * 0.045f), diagonal);
        }
        reveal = saved;
    }

    private void drawCrazy(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                           float s, float fade, float age) {
        float saved = reveal;
        for (int i = 0; i < 10; i++) {
            reveal = clamp((saved - i * 0.045f) / (1.0f - i * 0.045f));
            double angle = Math.PI * 2.0 * i / 10.0 + age * 0.025;
            Vec3 radial = f.scale(Math.cos(angle)).add(r.scale(Math.sin(angle)));
            Vec3 tangent = f.scale(-Math.sin(angle)).add(r.scale(Math.cos(angle)));
            final int slash = i;
            serratedRibbon(b, m, 13, t -> o.add(up.scale((0.25 + t * 2.2 + (slash % 3) * 0.25) * s))
                            .add(radial.scale((0.35 + t * 3.6) * s)),
                    tangent, t -> (float) (0.19 * s), fade * 0.78f, i % 2 == 0 ? 1 : -1);
        }
        reveal = saved;
    }

    private void drawPalisade(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r, float s, float fade) {
        float saved = reveal;
        for (int i = 0; i < 4; i++) {
            reveal = clamp((saved - i * 0.12f) / (1.0f - i * 0.12f));
            final int slash = i;
            serratedRibbon(b, m, 20, t -> o.add(f.scale((0.4 + t * 5.8) * s))
                            .add(r.scale(((t - 0.5) * 5.4 + (slash - 1.5) * 0.55) * s))
                            .add(up.scale((0.35 + slash * 0.48 + Math.sin(t * Math.PI) * 0.42) * s)),
                    r, t -> (float) ((0.32 - t * 0.10) * s), fade * (1.0f - i * 0.07f), i % 2 == 0 ? 1 : -1);
        }
        reveal = saved;
    }

    private void drawAwareness(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                               float s, float fade, float age) {
        Vec3 center = o.add(up.scale(0.65 * s));
        float pulse = 0.88f + (float) Math.sin(age * 0.16) * 0.12f;
        for (int layer = 0; layer < 3; layer++) {
            float saved = reveal;
            reveal = clamp((saved - layer * 0.15f) / (1.0f - layer * 0.15f));
            ring(b, m, center.add(up.scale(layer * 0.25 * s)), f, r,
                    (2.0f + layer * 2.0f) * pulse * s, (0.18f - layer * 0.025f) * s,
                    28, color(layer == 2 ? ICE : TEAL, fade * (0.86f - layer * 0.18f)));
            reveal = saved;
        }
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0;
            Vec3 radial = f.scale(Math.cos(angle)).add(r.scale(Math.sin(angle)));
            Vec3 tangent = f.scale(-Math.sin(angle)).add(r.scale(Math.cos(angle)));
            ribbon(b, m, 10, t -> center.add(radial.scale(t * 5.6 * s)).add(up.scale(Math.sin(t * Math.PI * 2.0) * 0.20 * s)),
                    tangent, t -> (float) ((0.09 - t * 0.035) * s), color(ICE, fade * 0.68f));
        }
    }

    private void drawBendy(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r, float s, float fade) {
        for (int side : new int[]{-1, 1}) {
            final int bend = side;
            serratedRibbon(b, m, 27, t -> o.add(f.scale((0.3 + t * 8.8) * s))
                            .add(r.scale((Math.sin(t * Math.PI * 1.6) * 1.3 + bend * 0.32) * s))
                            .add(up.scale((0.45 + Math.sin(t * Math.PI) * (0.75 + bend * 0.10)) * s)),
                    r, t -> (float) ((0.26 - t * 0.12) * s), fade, bend);
        }
    }

    private void drawWhirl(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                           float s, float fade, float age) {
        Vec3 center = o.add(up.scale(0.72 * s));
        for (int layer = 0; layer < 2; layer++) {
            float saved = reveal;
            reveal = clamp((saved - layer * 0.12f) / (1.0f - layer * 0.12f));
            serratedRing(b, m, center.add(up.scale(layer * 0.34 * s)), f, r,
                    (2.65f + layer * 0.55f) * s, (0.36f - layer * 0.08f) * s,
                    30, fade * (1.0f - layer * 0.12f), age * (layer == 0 ? 1 : -1));
            reveal = saved;
        }
    }

    private void drawThrow(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                           float s, float fade, float age) {
        for (int side : new int[]{-1, 1}) {
            final int lane = side;
            serratedRibbon(b, m, 26, t -> o.add(f.scale(t * 9.5 * s))
                            .add(r.scale((lane * (0.58 + t * 0.18) + Math.sin(t * 18.0 + age * 0.22) * 0.10) * s))
                            .add(up.scale((0.72 + Math.sin(t * Math.PI * 2.0) * 0.16) * s)),
                    r, t -> (float) ((0.22 - t * 0.08) * s), fade, lane);
            Vec3 tip = o.add(f.scale(9.5 * reveal * s)).add(r.scale(lane * 0.72 * s)).add(up.scale(0.72 * s));
            diamond(b, m, tip, r, up, 0.34f * s, 0.16f * s, color(BONE, fade));
        }
    }

    private void serratedRibbon(BufferBuilder b, Matrix4f m, int segments, DoubleFunction<Vec3> path,
                                Vec3 widthAxis, DoubleFunction<Float> halfWidth, float fade, int toothSide) {
        ribbon(b, m, segments, path, widthAxis, halfWidth, color(INK, fade * 0.94f));
        ribbon(b, m, segments, t -> path.apply(t).add(up.scale(0.055)), widthAxis,
                t -> halfWidth.apply(t) * 0.65f, color(TEAL, fade), 0.94f);
        ribbon(b, m, segments, t -> path.apply(t).add(widthAxis.scale(toothSide * halfWidth.apply(t) *
                        (0.62 + ((int) (t * segments) % 2) * 0.48))).add(up.scale(0.08)),
                widthAxis, t -> halfWidth.apply(t) * 0.20f, color(WHITE, fade), 0.86f);
    }

    private void serratedRing(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 a, Vec3 c,
                              float radius, float width, int segments, float fade, float age) {
        ring(b, m, center, a, c, radius, width, segments, color(INK, fade));
        ring(b, m, center.add(up.scale(0.06)), a, c, radius, width * 0.58f, segments, color(TEAL, fade));
        ring(b, m, center.add(up.scale(0.10)), a, c, radius + width * 0.45f, width * 0.18f,
                segments, color(WHITE, fade * 0.92f));
    }

    private void drawHistory(BufferBuilder b, Matrix4f m, VfxInstance instance, Camera camera,
                             Vec3 right, float s, float fade) {
        var points = instance.originHistory();
        Vec3 currentOrigin = instance.origin();
        for (int i = 1; i < points.size(); i++) {
            if (points.get(i).subtract(currentOrigin).dot(instance.direction()) > 0.12) continue;
            Vec3 a = points.get(i - 1).subtract(camera.getPosition()).add(up.scale(0.58 * s));
            Vec3 c = points.get(i).subtract(camera.getPosition()).add(up.scale(0.58 * s));
            if (a.distanceToSqr(c) < 0.0025) continue;
            float life = i / (float) points.size();
            for (int side : new int[]{-1, 1}) {
                Vec3 offset = right.scale(side * 0.38 * s);
                float width = (0.08f + life * 0.15f) * s;
                quad(b, m, a.add(offset).subtract(right.scale(width)), c.add(offset).subtract(right.scale(width)),
                        c.add(offset).add(right.scale(width)), a.add(offset).add(right.scale(width)),
                        color(side > 0 ? ICE : TEAL, fade * life * 0.68f));
            }
        }
    }

    private void ribbon(BufferBuilder b, Matrix4f m, int segments, DoubleFunction<Vec3> path,
                        Vec3 widthAxis, DoubleFunction<Float> halfWidth, int color) {
        ribbon(b, m, segments, path, widthAxis, halfWidth, color, 1.0f);
    }

    private void ribbon(BufferBuilder b, Matrix4f m, int segments, DoubleFunction<Vec3> path,
                        Vec3 widthAxis, DoubleFunction<Float> halfWidth, int color, float revealScale) {
        int count = Math.min(segments, 30);
        float visible = reveal * revealScale * count;
        int end = Math.min(count, (int) Math.ceil(visible));
        for (int i = 0; i < end; i++) {
            double t0 = i / (double) count, t1 = (i + Math.min(1.0f, visible - i)) / count;
            Vec3 p0 = path.apply(t0), p1 = path.apply(t1);
            Vec3 w0 = widthAxis.scale(halfWidth.apply(t0)), w1 = widthAxis.scale(halfWidth.apply(t1));
            quad(b, m, p0.subtract(w0), p1.subtract(w1), p1.add(w1), p0.add(w0), shade(color, i, count));
        }
    }

    private void ring(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 a, Vec3 c, float radius,
                      float width, int segments, int color) {
        int count = Math.min(segments, 30);
        float visible = reveal * count;
        int end = Math.min(count, (int) Math.ceil(visible));
        float inner = radius - width * 0.5f, outer = radius + width * 0.5f;
        for (int i = 0; i < end; i++) {
            float part = Math.min(1.0f, visible - i);
            double a0 = -Math.PI / 2.0 + Math.PI * 2.0 * i / count;
            double a1 = -Math.PI / 2.0 + Math.PI * 2.0 * (i + part) / count;
            quad(b, m, ellipse(center, a, c, inner, a0), ellipse(center, a, c, inner, a1),
                    ellipse(center, a, c, outer, a1), ellipse(center, a, c, outer, a0), shade(color, i, count));
        }
    }

    private static int shade(int color, int segment, int count) {
        float t = (segment + 0.5f) / count;
        if (t < 0.16f) return VfxPixelRender.mixRgb(color, INK, 0.48f);
        if (t > 0.58f && t < 0.82f) return VfxPixelRender.mixRgb(color, WHITE, 0.25f);
        return color;
    }

    private static Vec3 ellipse(Vec3 center, Vec3 a, Vec3 b, float radius, double angle) {
        return center.add(a.scale(Math.cos(angle) * radius)).add(b.scale(Math.sin(angle) * radius));
    }

    private static void diamond(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 horizontal,
                                Vec3 vertical, float halfWidth, float halfHeight, int color) {
        quad(b, m, center.subtract(horizontal.scale(halfWidth)), center.add(vertical.scale(halfHeight)),
                center.add(horizontal.scale(halfWidth)), center.subtract(vertical.scale(halfHeight)), color);
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
