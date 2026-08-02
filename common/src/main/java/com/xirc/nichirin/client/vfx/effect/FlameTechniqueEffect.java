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

/** Connected, code-authored Flame Breathing geometry rendered by the shared pixel framebuffer. */
public final class FlameTechniqueEffect implements VfxEffect {
    public enum Style {
        UNKNOWING_FIRE(48), RISING_SUN(64), BLAZING_UNIVERSE(42), BLOOMING(44),
        FLAME_TIGER(46), RENGOKU(58), POMMEL_SLASH(18);

        private final int lifetime;
        Style(int lifetime) { this.lifetime = lifetime; }
    }

    private final Style style;
    private final Palette palette;
    private Vec3 renderUp = new Vec3(0, 1, 0);
    private float shapeReveal;

    public FlameTechniqueEffect(Style style) {
        this.style = style;
        this.palette = switch (style) {
            case UNKNOWING_FIRE -> new Palette(0x692F0F, 0xFC5520, 0xFF831E, 0xFFBD1E, 0xFFF245);
            case RISING_SUN -> new Palette(0x3A0A04, 0xFC5520, 0xFF831E, 0xFEEF24, 0xFFF245);
            case BLAZING_UNIVERSE -> new Palette(0x780404, 0xFF4300, 0xFC5520, 0xFFBD1E, 0xFFFFFF);
            case BLOOMING -> new Palette(0x711B08, 0xFC5520, 0xFF831E, 0xFFBD1E, 0xFEEF24);
            case FLAME_TIGER -> new Palette(0x5D1305, 0xFC5520, 0xFF831E, 0xFFBD1E, 0xFFF678);
            case RENGOKU -> new Palette(0x780404, 0xFC5520, 0xFF831E, 0xFEEF24, 0xFFF678);
            case POMMEL_SLASH -> new Palette(0x692F0F, 0xFC5520, 0xFF831E, 0xFFBD1E, 0xFFF245);
        };
    }

    @Override
    public int lifetimeTicks() { return style.lifetime; }

    @Override
    public void render(VfxInstance instance, PoseStack poseStack, Camera camera, float partialTick) {
        float age = instance.ageTicks() + partialTick;
        float revealTicks = switch (style) {
            case UNKNOWING_FIRE, RENGOKU -> 40.0f;
            case RISING_SUN, BLAZING_UNIVERSE -> 12.0f;
            default -> Math.min(10.0f, style.lifetime * 0.3f);
        };
        float progress = clamp(age / revealTicks);
        shapeReveal = smooth(progress);
        float fade = 1.0f - clamp((age - style.lifetime * 0.68f) / (style.lifetime * 0.32f));
        if (fade <= 0.0f) return;

        Vec3 origin = instance.origin(partialTick).subtract(camera.getPosition());
        Vec3 forward = instance.direction().normalize();
        Vec3 right = rightOf(forward);
        renderUp = right.cross(forward).normalize();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = VfxPixelRender.beginQuads();
        float scale = instance.scale();

        drawMovementTrail(buffer, matrix, instance, camera, right, scale, fade, age);

        switch (style) {
            case UNKNOWING_FIRE -> drawUnknowingFire(buffer, matrix, origin, forward, right, scale, progress, fade, age);
            case RISING_SUN -> drawRisingSun(buffer, matrix, origin, forward, right, scale, progress, fade, age);
            case BLAZING_UNIVERSE -> drawBlazingUniverse(buffer, matrix, origin, forward, right, scale, progress, fade, age);
            case BLOOMING -> drawBlooming(buffer, matrix, origin, forward, right, scale, progress, fade, age);
            case FLAME_TIGER -> drawFlameTiger(buffer, matrix, origin, forward, right, scale, progress, fade, age);
            case RENGOKU -> drawRengoku(buffer, matrix, origin, forward, right, scale, progress, fade, age);
            case POMMEL_SLASH -> drawPommelSlash(buffer, matrix, origin, forward, right, scale, progress, fade, age);
        }
        VfxPixelRender.finish(buffer);
    }

    private void drawUnknowingFire(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                   float s, float p, float fade, float age) {
        float length = (1.0f + 8.0f * ease(p)) * s;
        stripedRibbon(b, m, 28,
                t -> o.add(f.scale(t * length)).add(r.scale(Math.sin(t * Math.PI * 2.0) * 0.34 * s))
                        .add(renderUp.scale((0.17 + Math.sin(t * Math.PI) * 0.48) * s)),
                r, t -> (float) ((0.34 + (1.0 - t) * 0.52) * s),
                color(palette.deep, fade * 0.74f), age + 2, 7);
        stripedRibbon(b, m, 28,
                t -> o.add(f.scale(t * length)).add(r.scale(Math.sin(t * Math.PI * 2.0) * 0.28 * s))
                        .add(renderUp.scale((0.22 + Math.sin(t * Math.PI) * 0.55) * s)),
                r, t -> (float) ((0.18 + (1.0 - t) * 0.42) * s),
                color(palette.ember, fade), age, 6);
        if (shapeReveal > 0.82f) {
            float slash = clamp((shapeReveal - 0.82f) / 0.18f);
            float savedReveal = shapeReveal;
            shapeReveal = slash;
            crescent(b, m, o.add(f.scale(length)).add(renderUp.scale(0.8 * s)), f, r,
                    2.7f * s, 0.46f * s, -78, 78, color(palette.hot, fade * slash), age);
            burst(b, m, o.add(f.scale(length)).add(renderUp.scale(0.82 * s)), f, r,
                    0.55f * s, 1.75f * s, 8, color(palette.gold, fade * slash * 0.82f));
            shapeReveal = savedReveal;
        }
    }

    private void drawRisingSun(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                               float s, float p, float fade, float age) {
        // The blade ignites behind the user, then the leading edge travels below and
        // forward before rising overhead. This is deliberately an open wheel: a full
        // ring appearing at once reads as an aura, not an upward sword technique.
        Vec3 center = o.subtract(f.scale(0.30 * s)).add(renderUp.scale(1.05 * s));
        Vec3 up = renderUp;
        float radius = (3.35f + 0.45f * ease(p)) * s;
        float outerReveal = clamp((shapeReveal - 0.10f) / 0.90f);
        sweptRing(b, m, center, f, up, radius, 0.60f * s, 28,
                color(palette.ember, fade), age, 7, 205, 250, shapeReveal);
        sweptRing(b, m, center.add(r.scale(0.03 * s)), f, up, radius * 0.80f,
                0.20f * s, 24, color(palette.gold, fade * 0.92f), age + 3, 6,
                205, 250, outerReveal);
        float contact = clamp((shapeReveal - 0.72f) / 0.28f);
        Vec3 leadingImpact = center.add(f.scale(radius * 0.35)).add(up.scale(radius * 0.94));
        burst(b, m, leadingImpact, f, r, 0.16f * s, 0.95f * s * contact, 7,
                color(palette.hot, fade * contact * 0.82f));
    }

    private void drawBlazingUniverse(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                     float s, float p, float fade, float age) {
        float length = (1.0f + 5.8f * ease(p)) * s;
        stripedRibbon(b, m, 26,
                t -> o.add(f.scale(t * length)).add(renderUp.scale((3.8 * (1.0 - t) + 0.2) * s)),
                r, t -> (float) ((0.22 + Math.sin(t * Math.PI) * 0.72) * s),
                color(palette.ember, fade), age, 5);
        if (p > 0.48f) {
            float impact = clamp((p - 0.48f) / 0.52f);
            Vec3 hit = o.add(f.scale(length)).add(renderUp.scale(0.12 * s));
            ring(b, m, hit, f, r, 3.4f * s * impact, 0.34f * s, 24,
                    color(palette.gold, fade), age, 6);
            ring(b, m, hit, f, r, 1.9f * s * impact, 0.22f * s, 20,
                    color(palette.hot, fade), age + 2, 5);
            burst(b, m, hit, f, r, 0.45f * s, 3.1f * s * impact, 12,
                    color(palette.orange, fade * 0.92f));
        }
    }

    private void drawBlooming(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                              float s, float p, float fade, float age) {
        Vec3 center = o.add(renderUp.scale(0.85 * s));
        float fullReveal = shapeReveal;
        for (int layer = 0; layer < 3; layer++) {
            shapeReveal = clamp((fullReveal - layer * 0.13f) / (1.0f - layer * 0.13f));
            if (shapeReveal <= 0.0f) continue;
            float radius = (1.2f + layer * 1.05f) * s * p;
            ring(b, m, center.add(renderUp.scale(layer * 0.22 * s)), f, r, radius,
                    (0.50f - layer * 0.08f) * s, 28,
                    color(layer == 0 ? palette.gold : layer == 2 ? palette.hot : palette.ember,
                            fade * (1.0f - layer * 0.12f)),
                    age * (layer % 2 == 0 ? 1 : -1), 6 + layer);
        }
        for (int spiral = 0; spiral < 2; spiral++) {
            final int current = spiral;
            shapeReveal = clamp((fullReveal - 0.22f - spiral * 0.12f) / (0.78f - spiral * 0.12f));
            if (shapeReveal <= 0.0f) continue;
            stripedRibbon(b, m, 24, t -> {
                double angle = t * Math.PI * 2.2 + current * Math.PI + age * 0.08;
                double radius = (0.8 + t * 2.5) * s * p;
                return center.add(f.scale(Math.cos(angle) * radius)).add(r.scale(Math.sin(angle) * radius))
                        .add(renderUp.scale(t * 1.65 * s));
            }, r, t -> 0.14f * s, color(current == 1 ? palette.gold : palette.ember, fade * 0.78f),
                    age + spiral * 2, 6);
        }
        shapeReveal = fullReveal;
    }

    private void drawFlameTiger(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                float s, float p, float fade, float age) {
        float length = (1.5f + 8.5f * ease(p)) * s;
        float fullReveal = shapeReveal;
        for (int claw = -1; claw <= 1; claw++) {
            final int currentClaw = claw;
            shapeReveal = clamp((fullReveal - (claw + 1) * 0.07f) / (1.0f - (claw + 1) * 0.07f));
            if (shapeReveal <= 0.0f) continue;
            stripedRibbon(b, m, 30,
                    t -> o.add(f.scale(t * length))
                            .add(r.scale((currentClaw * 0.48 + Math.sin(t * Math.PI * 3 + currentClaw) * 0.34) * s))
                            .add(renderUp.scale((0.45 + Math.sin(t * Math.PI) * 1.25 + currentClaw * 0.12) * s)),
                    r, t -> (float) ((0.16 + t * 0.28) * s),
                    color(currentClaw == 0 ? palette.gold : palette.ember, fade), age + claw * 2, 5);
        }
        shapeReveal = fullReveal;
        if (fullReveal < 0.62f) return;
        float headReveal = clamp((fullReveal - 0.62f) / 0.38f);
        Vec3 head = o.add(f.scale(length)).add(renderUp.scale(1.65 * s));
        diamond(b, m, head, r, renderUp, 0.72f * s, 0.58f * s,
                color(palette.hot, fade * headReveal));
        Vec3 eyeLift = renderUp.scale(0.12 * s);
        diamond(b, m, head.add(eyeLift).subtract(r.scale(0.28 * s)), r, renderUp,
                0.09f * s, 0.07f * s, color(0xFFFFFF, fade * headReveal));
        diamond(b, m, head.add(eyeLift).add(r.scale(0.28 * s)), r, renderUp,
                0.09f * s, 0.07f * s, color(0xFFFFFF, fade * headReveal));
        shapeReveal = headReveal;
        for (int claw = -1; claw <= 1; claw++) {
            crescent(b, m, head.add(r.scale(claw * 0.48 * s)).subtract(f.scale(0.35 * s)),
                    f, r, 1.1f * s, 0.16f * s, -68, 68,
                    color(palette.gold, fade * headReveal * 0.84f), age + claw);
        }
        shapeReveal = fullReveal;
    }

    private void drawRengoku(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                             float s, float p, float fade, float age) {
        float length = (2.0f + 12.0f * ease(p)) * s;
        DoubleFunction<Vec3> spine = t -> o.add(f.scale(t * length))
                .add(r.scale(Math.sin(t * Math.PI * 4.0 + age * 0.14) * (0.35 + t) * s))
                .add(renderUp.scale((0.65 + Math.sin(t * Math.PI * 2.0) * 0.65 + t * 1.8) * s));
        stripedRibbon(b, m, 40, spine, r, t -> (float) ((0.30 + t * 0.72) * s),
                color(palette.ember, fade), age, 5);
        stripedRibbon(b, m, 40, t -> spine.apply(t).add(renderUp.scale(0.18 * s)), r,
                t -> 0.14f * s, color(palette.gold, fade), age + 2, 7);
        if (shapeReveal < 0.64f) return;
        float fullReveal = shapeReveal;
        float headReveal = clamp((fullReveal - 0.64f) / 0.36f);
        Vec3 head = spine.apply(shapeReveal);
        diamond(b, m, head, r, renderUp, 1.25f * s, 0.92f * s,
                color(palette.hot, fade * headReveal));
        shapeReveal = headReveal;
        crescent(b, m, head, f, r, 2.1f * s, 0.28f * s, -62, 62,
                color(palette.gold, fade * headReveal), age);
        burst(b, m, head, r, renderUp, 0.72f * s, 2.45f * s, 14,
                color(palette.orange, fade * headReveal * 0.88f));
        Vec3 muzzle = head.add(f.scale(0.45 * s));
        diamond(b, m, muzzle.subtract(r.scale(0.34 * s)), r, renderUp,
                0.10f * s, 0.08f * s, color(0xFFFFFF, fade * headReveal));
        diamond(b, m, muzzle.add(r.scale(0.34 * s)), r, renderUp,
                0.10f * s, 0.08f * s, color(0xFFFFFF, fade * headReveal));
        shapeReveal = fullReveal;
    }

    private void drawPommelSlash(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                 float s, float p, float fade, float age) {
        Vec3 center = o.add(f.scale(2.2 * s)).add(renderUp.scale(1.05 * s));
        crescent(b, m, center, f, r, (0.55f + 2.7f * ease(p)) * s,
                0.42f * s, -72, 72, color(palette.ember, fade), age);
        crescent(b, m, center.add(renderUp.scale(0.16 * s)), f, r,
                (0.45f + 2.4f * ease(p)) * s, 0.14f * s,
                -68, 68, color(palette.hot, fade), age + 2);
        burst(b, m, center, f, r, 0.35f * s, 1.35f * s * p, 7,
                color(palette.gold, fade * 0.76f));
    }

    private void ring(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 axisA, Vec3 axisB,
                      float radius, float width, int segments, int color, float age, int spacing) {
        sweptRing(b, m, center, axisA, axisB, radius, width, segments, color, age, spacing,
                0.0f, 360.0f, shapeReveal);
    }

    private void sweptRing(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 axisA, Vec3 axisB,
                           float radius, float width, int segments, int color, float age, int spacing,
                           float startDegrees, float sweepDegrees, float reveal) {
        int count = Math.min(segments, 28);
        float visible = clamp(reveal) * count;
        int visibleSegments = Math.min(count, (int) Math.ceil(visible));
        int phase = (int) Math.floor(age * 0.52f);
        float inner = Math.max(0, radius - width * 0.5f);
        float outer = radius + width * 0.5f;
        for (int i = 0; i < visibleSegments; i++) {
            float end = Math.min(1.0f, visible - i);
            double a0 = Math.toRadians(startDegrees + sweepDegrees * i / count);
            double a1 = Math.toRadians(startDegrees + sweepDegrees * (i + end) / count);
            int c = flameShade(color, (i + 0.5f) / count);
            if (Math.floorMod(i - phase, spacing) <= 1) c = VfxPixelRender.mixRgb(c, palette.hot, 0.58f);
            quad(b, m, ellipse(center, axisA, axisB, inner, a0), ellipse(center, axisA, axisB, inner, a1),
                    ellipse(center, axisA, axisB, outer, a1), ellipse(center, axisA, axisB, outer, a0), c);
        }
    }

    private void stripedRibbon(BufferBuilder b, Matrix4f m, int segments, DoubleFunction<Vec3> path,
                               Vec3 widthAxis, DoubleFunction<Float> halfWidth, int color,
                               float age, int spacing) {
        int count = Math.min(segments, 30);
        float visible = shapeReveal * count;
        int visibleSegments = Math.min(count, (int) Math.ceil(visible));
        int phase = (int) Math.floor(age * 0.58f);
        for (int i = 0; i < visibleSegments; i++) {
            double t0 = i / (double) count;
            double t1 = (i + Math.min(1.0f, visible - i)) / count;
            Vec3 p0 = path.apply(t0);
            Vec3 p1 = path.apply(t1);
            Vec3 w0 = widthAxis.scale(halfWidth.apply(t0));
            Vec3 w1 = widthAxis.scale(halfWidth.apply(t1));
            int c = flameShade(color, (i + 0.5f) / count);
            if (Math.floorMod(i - phase, spacing) == 0) c = VfxPixelRender.mixRgb(c, palette.hot, 0.62f);
            quad(b, m, p0.subtract(w0), p1.subtract(w1), p1.add(w1), p0.add(w0), c);
        }
    }

    private void crescent(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 f, Vec3 r,
                          float radius, float width, float startDegrees, float endDegrees,
                          int color, float age) {
        int count = 16;
        float visible = shapeReveal * count;
        int visibleSegments = Math.min(count, (int) Math.ceil(visible));
        int phase = (int) Math.floor(age * 0.48f);
        for (int i = 0; i < visibleSegments; i++) {
            float end = Math.min(1.0f, visible - i);
            double a0 = Math.toRadians(startDegrees + (endDegrees - startDegrees) * i / count);
            double a1 = Math.toRadians(startDegrees + (endDegrees - startDegrees) * (i + end) / count);
            int c = flameShade(color, (i + 0.5f) / count);
            if (Math.floorMod(i - phase, 7) == 0) c = VfxPixelRender.mixRgb(c, palette.hot, 0.66f);
            quad(b, m, ellipse(center, f, r, radius - width, a0), ellipse(center, f, r, radius - width, a1),
                    ellipse(center, f, r, radius, a1), ellipse(center, f, r, radius, a0), c);
        }
    }

    private static void diamond(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 horizontal,
                                Vec3 vertical, float halfWidth, float halfHeight, int color) {
        quad(b, m, center.subtract(horizontal.scale(halfWidth)), center.add(vertical.scale(halfHeight)),
                center.add(horizontal.scale(halfWidth)), center.subtract(vertical.scale(halfHeight)), color);
    }

    private void burst(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 axisA, Vec3 axisB,
                              float innerRadius, float length, int count, int color) {
        float contact = clamp((shapeReveal - 0.58f) / 0.42f);
        int visibleSpikes = Math.min(count, (int) Math.ceil(contact * count));
        for (int i = 0; i < visibleSpikes; i++) {
            double angle = Math.PI * 2.0 * i / count;
            Vec3 radial = axisA.scale(Math.cos(angle)).add(axisB.scale(Math.sin(angle)));
            Vec3 tangent = axisA.scale(-Math.sin(angle)).add(axisB.scale(Math.cos(angle)));
            float halfWidth = length * (i % 3 == 0 ? 0.095f : 0.065f);
            float spikeLength = length * contact * (i % 2 == 0 ? 1.0f : 0.72f);
            Vec3 root = center.add(radial.scale(innerRadius));
            Vec3 tip = center.add(radial.scale(innerRadius + spikeLength));
            quad(b, m, root.subtract(tangent.scale(halfWidth)), tip, tip,
                    root.add(tangent.scale(halfWidth)), color);
        }
    }

    private int flameShade(int color, float position) {
        if (position < 0.16f) return VfxPixelRender.mixRgb(color, palette.deep, 0.66f);
        if (position < 0.40f) return VfxPixelRender.mixRgb(color, palette.ember, 0.34f);
        if (position < 0.66f) return VfxPixelRender.mixRgb(color, palette.gold, 0.48f);
        if (position < 0.84f) return VfxPixelRender.mixRgb(color, palette.orange, 0.20f);
        return VfxPixelRender.mixRgb(color, palette.deep, 0.46f);
    }

    private static Vec3 ellipse(Vec3 center, Vec3 a, Vec3 b, float radius, double angle) {
        return center.add(a.scale(Math.cos(angle) * radius)).add(b.scale(Math.sin(angle) * radius));
    }

    private static void quad(BufferBuilder b, Matrix4f m, Vec3 a, Vec3 c, Vec3 d, Vec3 e, int color) {
        VfxPixelRender.quad(b, m, (float) a.x, (float) a.y, (float) a.z,
                (float) c.x, (float) c.y, (float) c.z, (float) d.x, (float) d.y, (float) d.z,
                (float) e.x, (float) e.y, (float) e.z, color);
    }

    private void drawMovementTrail(BufferBuilder b, Matrix4f m, VfxInstance instance, Camera camera,
                                   Vec3 right, float scale, float fade, float age) {
        var points = instance.originHistory();
        for (int i = 1; i < points.size(); i++) {
            Vec3 a = points.get(i - 1).subtract(camera.getPosition()).add(renderUp.scale(0.65 * scale));
            Vec3 c = points.get(i).subtract(camera.getPosition()).add(renderUp.scale(0.65 * scale));
            if (a.distanceToSqr(c) < 0.0025) continue;
            float life = i / (float) points.size();
            float width = (0.24f + 0.46f * life) * scale;
            int base = flameShade(color(i % 4 == 0 ? palette.gold : palette.ember, fade * life * 0.78f), life);
            quad(b, m, a.subtract(right.scale(width)), c.subtract(right.scale(width * 0.86)),
                    c.add(right.scale(width * 0.86)), a.add(right.scale(width)), base);
            if (i % 2 == 0) {
                float core = width * 0.34f;
                quad(b, m, a.subtract(right.scale(core)), c.subtract(right.scale(core)),
                        c.add(right.scale(core)), a.add(right.scale(core)),
                        color(palette.hot, fade * life * 0.62f));
            }
        }
    }

    private static Vec3 rightOf(Vec3 forward) {
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        return right.lengthSqr() > 1.0E-6 ? right.normalize() : new Vec3(1, 0, 0);
    }

    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
    private static float smooth(float value) { return value * value * (3.0f - 2.0f * value); }
    private static float ease(float value) { float inverse = 1 - value; return 1 - inverse * inverse * inverse; }
    private static int color(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private record Palette(int deep, int ember, int orange, int gold, int hot) {}
}
