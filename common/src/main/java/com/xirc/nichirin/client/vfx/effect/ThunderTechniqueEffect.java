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

/** Progressive, code-authored Thunder Breathing geometry rendered by the shared pixel pass. */
public final class ThunderTechniqueEffect implements VfxEffect {
    public enum Style {
        DASH(18), GODSPEED(30), RICE_SLASH(14), SWARM_SLASH(18), CHARGE(20),
        HEAT_RISE(24), WARNING(36), STRIKE(14), DRAGON(30), IMPACT(18);

        private final int lifetime;
        Style(int lifetime) { this.lifetime = lifetime; }
    }

    private static final int CHARCOAL = 0x292929;
    private static final int DEEP_GOLD = 0xD8A900;
    private static final int YELLOW = 0xFFFA45;
    private static final int LIGHT = 0xFFFB6B;
    private static final int HOT = 0xFFFC98;
    private static final int WHITE = 0xFFFFFF;

    private final Style style;
    private float reveal;
    private Vec3 up = new Vec3(0, 1, 0);

    public ThunderTechniqueEffect(Style style) {
        this.style = style;
    }

    @Override
    public int lifetimeTicks() {
        return style.lifetime;
    }

    @Override
    public void render(VfxInstance instance, PoseStack poseStack, Camera camera, float partialTick) {
        float age = instance.ageTicks() + partialTick;
        float revealTicks = switch (style) {
            case DASH, STRIKE -> 3.0f;
            case IMPACT -> 5.0f;
            case SWARM_SLASH -> 13.0f;
            case GODSPEED -> 20.0f;
            case WARNING -> 30.0f;
            case DRAGON -> 8.0f;
            default -> Math.min(7.0f, style.lifetime * 0.42f);
        };
        reveal = smooth(clamp(age / revealTicks));
        float fade = 1.0f - clamp((age - style.lifetime * 0.62f) / (style.lifetime * 0.38f));
        if (fade <= 0.0f) return;

        Vec3 origin = instance.origin(partialTick).subtract(camera.getPosition());
        Vec3 forward = switch (style) {
            case WARNING, STRIKE, IMPACT -> horizontal(instance.direction());
            default -> instance.direction().normalize();
        };
        Vec3 right = rightOf(forward);
        up = right.cross(forward).normalize();
        float scale = instance.scale();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = VfxPixelRender.beginQuads();

        switch (style) {
            case DASH -> drawDash(buffer, matrix, instance, camera, origin, forward, right, scale, fade, age, false);
            case GODSPEED -> drawDash(buffer, matrix, instance, camera, origin, forward, right, scale, fade, age, true);
            case RICE_SLASH -> drawRiceSlash(buffer, matrix, origin, forward, right, scale, fade, age);
            case SWARM_SLASH -> drawSwarmSlash(buffer, matrix, origin, forward, right, scale, fade, age);
            case CHARGE -> drawCharge(buffer, matrix, origin, forward, right, scale, fade, age);
            case HEAT_RISE -> drawHeatRise(buffer, matrix, origin, forward, right, scale, fade, age);
            case WARNING -> drawWarning(buffer, matrix, origin, forward, right, scale, fade, age);
            case STRIKE -> drawStrike(buffer, matrix, origin, forward, right, scale, fade, age);
            case DRAGON -> drawDragon(buffer, matrix, instance, camera, origin, forward, right, scale, fade, age);
            case IMPACT -> drawImpact(buffer, matrix, origin, forward, right, scale, fade, age);
        }
        VfxPixelRender.finish(buffer);
    }

    private void drawDash(BufferBuilder b, Matrix4f m, VfxInstance instance, Camera camera, Vec3 o,
                          Vec3 f, Vec3 r, float s, float fade, float age, boolean godspeed) {
        drawHistory(b, m, instance, camera, r, s * (godspeed ? 1.25f : 1.0f), fade);
        float length = (godspeed ? 10.5f : 5.8f) * s;
        Vec3 wakeStart = o.add(up.scale(0.75 * s)).subtract(f.scale(0.10 * s));
        Vec3 backward = f.scale(-1.0);
        jaggedBolt(b, m, wakeStart, backward, r, length,
                (godspeed ? 0.52f : 0.34f) * s, 22, 0.22f * s,
                color(CHARCOAL, fade * 0.92f), age, 17L);
        float saved = reveal;
        reveal = clamp((saved - 0.08f) / 0.92f);
        jaggedBolt(b, m, wakeStart.add(up.scale(0.08 * s)), backward, r, length,
                (godspeed ? 0.34f : 0.22f) * s, 22, 0.11f * s,
                color(YELLOW, fade), age + 2, 31L);
        reveal = clamp((saved - 0.18f) / 0.82f);
        jaggedBolt(b, m, wakeStart.add(up.scale(0.13 * s)), backward, r, length * 0.96f,
                0.16f * s, 20, 0.055f * s, color(HOT, fade * 0.94f), age + 4, 47L);
        reveal = saved;
    }

    private void drawRiceSlash(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                               float s, float fade, float age) {
        arcBand(b, m, o.add(up.scale(0.92 * s)), f, r, 2.15f * s, 0.48f * s,
                -72, 144, 18, color(CHARCOAL, fade));
        float saved = reveal;
        reveal = clamp((saved - 0.10f) / 0.90f);
        arcBand(b, m, o.add(up.scale(0.97 * s)), f, r, 2.03f * s, 0.20f * s,
                -72, 144, 18, color(YELLOW, fade));
        arcBand(b, m, o.add(f.scale(0.18 * s)).add(up.scale(1.02 * s)), up, r,
                1.72f * s, 0.12f * s, -62, 124, 16, color(HOT, fade * 0.90f));
        jaggedBolt(b, m, o.subtract(f.scale(1.6 * s)).add(up.scale(0.82 * s)), f, r,
                3.4f * s, 0.22f * s, 15, 0.06f * s, color(LIGHT, fade * 0.74f), age, 59L);
        reveal = clamp((saved - 0.28f) / 0.72f);
        burst(b, m, o.add(f.scale(1.8 * s)).add(up.scale(0.96 * s)), f, r,
                0.12f * s, 0.85f * s, 5, color(HOT, fade * 0.78f));
        reveal = saved;
    }

    private void drawSwarmSlash(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                float s, float fade, float age) {
        Vec3 head = o.add(f.scale(16.0 * s * reveal));
        float saved = reveal;
        reveal = 1.0f;
        jaggedBolt(b, m, o, f, r, (float) o.distanceTo(head), 0.26f * s, 24,
                0.07f * s, color(YELLOW, fade * 0.70f), age, 73L);
        reveal = saved;
        arcBand(b, m, head, up, r, 1.45f * s, 0.34f * s, -66, 132, 16,
                color(CHARCOAL, fade));
        arcBand(b, m, head.add(f.scale(0.05 * s)), up, r, 1.35f * s, 0.14f * s,
                -66, 132, 16, color(LIGHT, fade));
        for (int wing = -2; wing <= 2; wing++) {
            if (wing == 0) continue;
            Vec3 wingCenter = head.subtract(f.scale(Math.abs(wing) * 0.22 * s))
                    .add(r.scale(wing * 0.52 * s)).add(up.scale((2 - Math.abs(wing)) * 0.18 * s));
            arcBand(b, m, wingCenter, up, r, (0.72f + Math.abs(wing) * 0.14f) * s,
                    0.10f * s, wing < 0 ? -58 : 122, wing < 0 ? 116 : -116, 12,
                    color(wing % 2 == 0 ? YELLOW : HOT, fade * 0.82f));
        }
    }

    private void drawCharge(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                            float s, float fade, float age) {
        Vec3 center = o.add(up.scale(0.9 * s));
        for (int layer = 0; layer < 3; layer++) {
            float local = clamp((reveal - layer * 0.10f) / (1.0f - layer * 0.10f));
            float saved = reveal;
            reveal = local;
            arcBand(b, m, center.add(up.scale(layer * 0.38 * s)), f, r,
                    (1.0f + layer * 0.48f) * s, 0.10f * s, -150 + age * 4,
                    300, 20, color(layer == 1 ? HOT : YELLOW, fade * (0.82f - layer * 0.10f)));
            reveal = saved;
        }
        jaggedBolt(b, m, center.subtract(f.scale(2.2 * s)), f, r, 4.4f * s,
                0.28f * s, 18, 0.07f * s, color(HOT, fade * 0.84f), age, 91L);
    }

    private void drawHeatRise(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                              float s, float fade, float age) {
        Vec3 center = o.add(f.scale(1.55 * s)).add(up.scale(0.45 * s));
        arcBand(b, m, center, f, up, 3.2f * s, 0.52f * s,
                -72, 155, 22, color(CHARCOAL, fade));
        float saved = reveal;
        reveal = clamp((saved - 0.08f) / 0.92f);
        arcBand(b, m, center, f, up, 3.05f * s, 0.20f * s,
                -72, 155, 22, color(YELLOW, fade));
        reveal = clamp((saved - 0.24f) / 0.76f);
        jaggedBolt(b, m, center.add(f.scale(0.8 * s)), up, r, 4.5f * s,
                0.25f * s, 18, 0.08f * s, color(HOT, fade * 0.88f), age, 113L);
        jaggedBolt(b, m, center.add(f.scale(0.36 * s)).subtract(r.scale(0.42 * s)), up, f,
                3.9f * s, 0.34f * s, 17, 0.075f * s, color(YELLOW, fade * 0.82f), age + 2, 117L);
        jaggedBolt(b, m, center.add(f.scale(1.08 * s)).add(r.scale(0.48 * s)), up, f,
                3.5f * s, 0.28f * s, 16, 0.055f * s, color(WHITE, fade * 0.74f), age + 4, 121L);
        burst(b, m, center.add(up.scale(0.35 * s)), f, r, 0.20f * s,
                1.55f * s, 8, color(LIGHT, fade * 0.70f));
        reveal = saved;
    }

    private void drawWarning(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                             float s, float fade, float age) {
        Vec3 center = o.add(up.scale(0.06 * s));
        float pulse = 0.88f + 0.12f * (float) Math.sin(age * 0.72f);
        arcBand(b, m, center, f, r, 1.45f * s * pulse, 0.15f * s,
                -90, 360, 24, color(reveal > 0.78f ? HOT : YELLOW, fade));
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI * 0.5 * i + age * 0.025;
            Vec3 base = center.add(f.scale(Math.cos(angle) * 0.72 * s))
                    .add(r.scale(Math.sin(angle) * 0.72 * s));
            jaggedBolt(b, m, base, up, r, (1.2f + reveal * 3.2f) * s,
                    0.12f * s, 12, 0.035f * s,
                    color(i == 0 ? HOT : LIGHT, fade * (0.44f + reveal * 0.42f)), age + i, 127L + i);
        }
    }

    private void drawStrike(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                            float s, float fade, float age) {
        Vec3 top = o.add(up.scale(9.0 * s));
        jaggedBolt(b, m, top, up.scale(-1), r, 9.0f * s,
                0.72f * s, 26, 0.24f * s, color(CHARCOAL, fade), age, 151L);
        float saved = reveal;
        reveal = clamp((saved - 0.08f) / 0.92f);
        jaggedBolt(b, m, top, up.scale(-1), r, 9.0f * s,
                0.46f * s, 26, 0.11f * s, color(YELLOW, fade), age + 2, 163L);
        reveal = clamp((saved - 0.18f) / 0.82f);
        jaggedBolt(b, m, top, up.scale(-1), r, 9.0f * s,
                0.22f * s, 24, 0.055f * s, color(WHITE, fade * 0.92f), age + 3, 179L);
        reveal = clamp((saved - 0.60f) / 0.40f);
        arcBand(b, m, o.add(up.scale(0.08 * s)), f, r, 3.1f * s, 0.28f * s,
                -90, 360, 24, color(HOT, fade));
        burst(b, m, o.add(up.scale(0.12 * s)), f, r, 0.25f * s,
                2.5f * s, 9, color(YELLOW, fade * 0.84f));
        reveal = saved;
    }

    private void drawDragon(BufferBuilder b, Matrix4f m, VfxInstance instance, Camera camera, Vec3 o,
                            Vec3 f, Vec3 r, float s, float fade, float age) {
        drawHistory(b, m, instance, camera, r, 1.4f * s, fade);
        float length = 17.0f * s;
        DoubleFunction<Vec3> spine = t -> o.add(f.scale(t * length))
                .add(r.scale(Math.sin(t * Math.PI * 5.0 + age * 0.10) * (0.25 + t * 1.15) * s))
                .add(up.scale((0.65 + t * 2.1 + Math.sin(t * Math.PI * 3.0) * 0.5) * s));
        ribbon(b, m, 36, spine, r, t -> (float) ((0.26 + t * 0.72) * s),
                color(CHARCOAL, fade));
        float saved = reveal;
        reveal = clamp((saved - 0.07f) / 0.93f);
        ribbon(b, m, 36, t -> spine.apply(t).add(up.scale(0.10 * s)), r,
                t -> (float) ((0.12 + t * 0.32) * s), color(YELLOW, fade));
        if (saved > 0.66f) {
            float headReveal = clamp((saved - 0.66f) / 0.34f);
            Vec3 head = spine.apply(saved);
            diamond(b, m, head, r, up, 1.15f * s, 0.82f * s,
                    color(HOT, fade * headReveal));
            reveal = headReveal;
            burst(b, m, head, r, up, 0.48f * s, 2.0f * s, 8,
                    color(YELLOW, fade * headReveal * 0.82f));
            Vec3 eyes = head.add(f.scale(0.24 * s)).add(up.scale(0.12 * s));
            diamond(b, m, eyes.subtract(r.scale(0.30 * s)), r, up,
                    0.08f * s, 0.06f * s, color(WHITE, fade * headReveal));
            diamond(b, m, eyes.add(r.scale(0.30 * s)), r, up,
                    0.08f * s, 0.06f * s, color(WHITE, fade * headReveal));
        }
        reveal = saved;
    }

    private void drawImpact(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                            float s, float fade, float age) {
        arcBand(b, m, o.add(up.scale(0.08 * s)), f, r, 6.8f * s, 0.42f * s,
                -90, 360, 28, color(CHARCOAL, fade));
        float saved = reveal;
        reveal = clamp((saved - 0.08f) / 0.92f);
        arcBand(b, m, o.add(up.scale(0.12 * s)), f, r, 5.4f * s, 0.22f * s,
                -90, 360, 26, color(YELLOW, fade));
        reveal = clamp((saved - 0.24f) / 0.76f);
        burst(b, m, o.add(up.scale(0.16 * s)), f, r, 0.42f * s,
                5.5f * s, 12, color(HOT, fade * 0.88f));
        reveal = saved;
    }

    private void jaggedBolt(BufferBuilder b, Matrix4f m, Vec3 start, Vec3 direction, Vec3 side,
                            float length, float jaggedness, int segments, float halfWidth,
                            int color, float age, long salt) {
        int count = Math.min(segments, 28);
        float visible = reveal * count;
        int visibleSegments = Math.min(count, (int) Math.ceil(visible));
        for (int i = 0; i < visibleSegments; i++) {
            float part = Math.min(1.0f, visible - i);
            double t0 = i / (double) count;
            double t1 = (i + part) / count;
            Vec3 p0 = boltPoint(start, direction, side, length, jaggedness, t0, i, salt, age);
            Vec3 p1 = boltPoint(start, direction, side, length, jaggedness, t1, i + 1, salt, age);
            float width = halfWidth * (float) (1.0 - t0 * 0.55);
            int shaded = thunderShade(color, (i + 0.5f) / count);
            quad(b, m, p0.subtract(side.scale(width)), p1.subtract(side.scale(width * 0.72)),
                    p1.add(side.scale(width * 0.72)), p0.add(side.scale(width)), shaded);
        }
    }

    private Vec3 boltPoint(Vec3 start, Vec3 direction, Vec3 side, float length, float jaggedness,
                           double t, int index, long salt, float age) {
        double step = Math.sin((index * 12.9898 + salt * 0.017) * 2.13) * jaggedness;
        double lift = Math.cos((index * 7.731 + salt * 0.031) * 1.71) * jaggedness * 0.48;
        double flow = Math.sin(age * 0.20 + index * 1.8) * jaggedness * 0.10;
        return start.add(direction.scale(t * length)).add(side.scale(step + flow)).add(up.scale(lift));
    }

    private void ribbon(BufferBuilder b, Matrix4f m, int segments, DoubleFunction<Vec3> path,
                        Vec3 widthAxis, DoubleFunction<Float> halfWidth, int color) {
        int count = Math.min(segments, 30);
        float visible = reveal * count;
        int visibleSegments = Math.min(count, (int) Math.ceil(visible));
        for (int i = 0; i < visibleSegments; i++) {
            double t0 = i / (double) count;
            double t1 = (i + Math.min(1.0f, visible - i)) / count;
            Vec3 p0 = path.apply(t0);
            Vec3 p1 = path.apply(t1);
            Vec3 w0 = widthAxis.scale(halfWidth.apply(t0));
            Vec3 w1 = widthAxis.scale(halfWidth.apply(t1));
            quad(b, m, p0.subtract(w0), p1.subtract(w1), p1.add(w1), p0.add(w0),
                    thunderShade(color, (i + 0.5f) / count));
        }
    }

    private void arcBand(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 axisA, Vec3 axisB,
                         float radius, float width, float startDegrees, float sweepDegrees,
                         int segments, int color) {
        int count = Math.min(segments, 28);
        float visible = reveal * count;
        int visibleSegments = Math.min(count, (int) Math.ceil(visible));
        float inner = Math.max(0.0f, radius - width * 0.5f);
        float outer = radius + width * 0.5f;
        for (int i = 0; i < visibleSegments; i++) {
            float part = Math.min(1.0f, visible - i);
            double a0 = Math.toRadians(startDegrees + sweepDegrees * i / count);
            double a1 = Math.toRadians(startDegrees + sweepDegrees * (i + part) / count);
            quad(b, m, ellipse(center, axisA, axisB, inner, a0),
                    ellipse(center, axisA, axisB, inner, a1),
                    ellipse(center, axisA, axisB, outer, a1),
                    ellipse(center, axisA, axisB, outer, a0),
                    thunderShade(color, (i + 0.5f) / count));
        }
    }

    private void burst(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 axisA, Vec3 axisB,
                       float inner, float length, int count, int color) {
        float contact = clamp((reveal - 0.45f) / 0.55f);
        int visible = Math.min(count, (int) Math.ceil(contact * count));
        for (int i = 0; i < visible; i++) {
            double angle = Math.PI * 2.0 * i / count;
            Vec3 radial = axisA.scale(Math.cos(angle)).add(axisB.scale(Math.sin(angle)));
            Vec3 tangent = axisA.scale(-Math.sin(angle)).add(axisB.scale(Math.cos(angle)));
            float reach = length * contact * (i % 3 == 0 ? 1.0f : 0.68f);
            Vec3 root = center.add(radial.scale(inner));
            Vec3 tip = center.add(radial.scale(inner + reach));
            Vec3 side = tangent.scale(length * 0.045f);
            quad(b, m, root.subtract(side), tip, tip, root.add(side), color);
        }
    }

    private void drawHistory(BufferBuilder b, Matrix4f m, VfxInstance instance, Camera camera,
                             Vec3 right, float scale, float fade) {
        var points = instance.originHistory();
        Vec3 currentOrigin = instance.origin();
        for (int i = 1; i < points.size(); i++) {
            if (points.get(i).subtract(currentOrigin).dot(instance.direction()) > 0.12) continue;
            Vec3 a = points.get(i - 1).subtract(camera.getPosition()).add(up.scale(0.78 * scale));
            Vec3 c = points.get(i).subtract(camera.getPosition()).add(up.scale(0.78 * scale));
            if (a.distanceToSqr(c) < 0.0025) continue;
            float life = i / (float) points.size();
            float width = (0.10f + life * 0.28f) * scale;
            quad(b, m, a.subtract(right.scale(width)), c.subtract(right.scale(width * 0.72)),
                    c.add(right.scale(width * 0.72)), a.add(right.scale(width)),
                    color(i % 3 == 0 ? HOT : YELLOW, fade * life * 0.76f));
        }
    }

    private static void diamond(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 horizontal,
                                Vec3 vertical, float halfWidth, float halfHeight, int color) {
        quad(b, m, center.subtract(horizontal.scale(halfWidth)), center.add(vertical.scale(halfHeight)),
                center.add(horizontal.scale(halfWidth)), center.subtract(vertical.scale(halfHeight)), color);
    }

    private static Vec3 ellipse(Vec3 center, Vec3 a, Vec3 b, float radius, double angle) {
        return center.add(a.scale(Math.cos(angle) * radius)).add(b.scale(Math.sin(angle) * radius));
    }

    private static int thunderShade(int color, float position) {
        if (position < 0.12f) return VfxPixelRender.mixRgb(color, CHARCOAL, 0.72f);
        if (position < 0.34f) return VfxPixelRender.mixRgb(color, DEEP_GOLD, 0.30f);
        if (position < 0.68f) return VfxPixelRender.mixRgb(color, HOT, 0.36f);
        if (position < 0.84f) return VfxPixelRender.mixRgb(color, WHITE, 0.26f);
        return VfxPixelRender.mixRgb(color, CHARCOAL, 0.46f);
    }

    private static Vec3 rightOf(Vec3 forward) {
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        return right.lengthSqr() > 1.0E-6 ? right.normalize() : new Vec3(1, 0, 0);
    }

    private static Vec3 horizontal(Vec3 direction) {
        Vec3 flat = new Vec3(direction.x, 0.0, direction.z);
        return flat.lengthSqr() > 1.0E-6 ? flat.normalize() : new Vec3(0, 0, 1);
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
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
