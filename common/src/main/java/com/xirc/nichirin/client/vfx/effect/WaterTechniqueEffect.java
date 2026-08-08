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

public final class WaterTechniqueEffect implements VfxEffect {
    public enum Style {
        WHEEL(30), RIPPLE_THRUST(22), FLOWING_DANCE(34), STRIKING_TIDE(28), WATERFALL(36),
        SPLASHING_FLOW(24), WHIRLPOOL(40), BLESSED_RAIN(28), BLESSED_RAIN_LEAP(22),
        CONSTANT_FLUX(38), DEAD_CALM(50);

        private final int lifetime;
        Style(int lifetime) { this.lifetime = lifetime; }
    }

    private final Style style;
    private final Palette palette;
    private Vec3 renderUp = new Vec3(0, 1, 0);
    private float shapeReveal;

    public WaterTechniqueEffect(Style style) {
        this.style = style;
        this.palette = switch (style) {
            case WHEEL -> new Palette(0x004E86, 0x1B93F0, 0x3BCBF0, 0xEEFBFD);
            case RIPPLE_THRUST -> new Palette(0x18A5CC, 0x34D1FD, 0x71EEFF, 0xA4F1FB);
            case FLOWING_DANCE -> new Palette(0x1B93F0, 0x34D1FD, 0x56DAFF, 0xF7FFFF);
            case STRIKING_TIDE -> new Palette(0x1B93F0, 0x34D1FD, 0x67EDFF, 0xF7FFFF);
            case WATERFALL -> new Palette(0x34D1FD, 0x56DAFF, 0x67EDFF, 0xEEFBFD);
            case SPLASHING_FLOW -> new Palette(0x127590, 0x36ABCC, 0x68EDFF, 0xF7FFFF);
            case WHIRLPOOL -> new Palette(0x1A8BAB, 0x22ABD2, 0x34D1FD, 0x71EEFF);
            case BLESSED_RAIN -> new Palette(0x34E5FD, 0x71EEFF, 0xA4F1FB, 0xD3FFF6);
            case BLESSED_RAIN_LEAP -> new Palette(0x127590, 0x34D1FD, 0xA4F1FB, 0xF2FDFF);
            case CONSTANT_FLUX -> new Palette(0x0B1883, 0x1179EA, 0x34E5FD, 0xF7FFFF);
            case DEAD_CALM -> new Palette(0x127590, 0x18A5CC, 0x34D1FD, 0xF2FDFF);
        };
    }

    @Override
    public int lifetimeTicks() {
        return style.lifetime;
    }

    @Override
    public void render(VfxInstance instance, PoseStack poseStack, Camera camera, float partialTick) {
        float age = instance.ageTicks() + partialTick;
        float progress = style == Style.WHEEL
                ? 1.0f
                : clamp(age / Math.min(10.0f, style.lifetime * 0.35f));
        shapeReveal = style == Style.WHEEL ? 1.0f : smooth(progress);
        float fade = 1.0f - clamp((age - style.lifetime * 0.58f) / (style.lifetime * 0.42f));
        if (fade <= 0.0f) return;
        Vec3 origin = instance.origin(partialTick).subtract(camera.getPosition());
        Vec3 forward = instance.direction().normalize();
        Vec3 right = right(forward);
        renderUp = right.cross(forward).normalize();
        float scale = instance.scale();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = VfxPixelRender.beginQuads();
        drawMovementTrail(buffer, matrix, instance, camera, right, scale, fade);
        switch (style) {
            case WHEEL -> drawWheel(buffer, matrix, origin, forward, right.scale(-1), scale, progress, fade);
            case RIPPLE_THRUST -> drawRippleThrust(buffer, matrix, origin, forward, right, scale, progress, fade);
            case FLOWING_DANCE -> drawFlowingDance(buffer, matrix, origin, forward, right, scale, progress, fade, age);
            case STRIKING_TIDE -> drawStrikingTide(buffer, matrix, origin, forward, right, scale, progress, fade);
            case WATERFALL -> drawWaterfall(buffer, matrix, origin, forward, right, scale, progress, fade, age);
            case SPLASHING_FLOW -> drawSplashingFlow(buffer, matrix, origin, forward, right, scale, progress, fade, age);
            case WHIRLPOOL -> drawWhirlpool(buffer, matrix, origin, forward, right, scale, progress, fade, age);
            case BLESSED_RAIN -> drawBlessedRain(buffer, matrix, origin, forward, right, scale, progress, fade);
            case BLESSED_RAIN_LEAP -> drawBlessedRainLeap(buffer, matrix, origin, forward, right,
                    scale, progress, fade, age);
            case CONSTANT_FLUX -> drawConstantFlux(buffer, matrix, origin, forward, right, scale, progress, fade, age);
            case DEAD_CALM -> drawDeadCalm(buffer, matrix, origin, forward, right, scale, progress, fade, age);
        }
        VfxPixelRender.finish(buffer);
    }

    private void drawWheel(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                  float s, float p, float fade) {
        Vec3 center = o.add(renderUp.scale(1.25 * s)).add(f.scale(0.5 * s));
        float radius = (0.6f + 1.8f * ease(p)) * s;
        ring(b, m, center, r, renderUp, radius, 0.42f * s, 40, color(palette.water, fade));
        ring(b, m, center, r, renderUp, radius * 0.72f, 0.18f * s, 32, color(palette.deep, fade));
        ring(b, m, center, r, renderUp, radius * 1.08f, 0.10f * s, 44, color(palette.foam, fade));
        burst(b, m, center, r, renderUp, radius * 0.88f, 0.62f * s, 14,
                color(palette.light, fade * 0.74f));
    }

    private void drawRippleThrust(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                         float s, float p, float fade) {
        float length = (1.0f + 6.0f * ease(p)) * s;
        ribbon(b, m, 24, t -> o.add(f.scale(t * length)).add(renderUp.scale(0.85 * s)), r,
                t -> (float) ((1.0 - t) * 0.55 * s + 0.05), color(palette.light, fade));
        for (int i = 0; i < 3; i++) {
            float radius = (0.45f + i * 0.65f + p * 0.5f) * s;
            ring(b, m, o.add(f.scale(i * 0.55 * s)).add(renderUp.scale(0.85 * s)), r,
                    renderUp, radius, 0.08f * s, 28, color(i == 2 ? palette.foam : palette.water, fade));
        }
        Vec3 tip = o.add(f.scale(length)).add(renderUp.scale(0.85 * s));
        burst(b, m, tip, r, renderUp, 0.18f * s, 0.72f * s, 8,
                color(palette.foam, fade * 0.86f));
    }

    private void drawFlowingDance(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                         float s, float p, float fade, float age) {
        float length = (1.0f + 7.0f * ease(p)) * s;
        stripedRibbon(b, m, 42, t -> o.add(f.scale(t * length)).add(r.scale(Math.sin(t * Math.PI * 3.0) * 1.25 * s))
                        .add(renderUp.scale((0.25 + Math.sin(t * Math.PI * 2.0) * 0.3) * s)),
                r, t -> (float) ((0.18 + Math.sin(t * Math.PI) * 0.42) * s),
                color(palette.water, fade), palette.light, age, 5);
        ribbon(b, m, 42, t -> o.add(f.scale(t * length)).add(r.scale(Math.sin(t * Math.PI * 3.0) * 1.25 * s))
                        .add(renderUp.scale((0.34 + Math.sin(t * Math.PI * 2.0) * 0.3) * s)),
                r, t -> 0.09f * s, color(palette.foam, fade));
    }

    private void drawStrikingTide(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                         float s, float p, float fade) {
        float fullReveal = shapeReveal;
        for (int layer = 0; layer < 4; layer++) {
            shapeReveal = clamp((fullReveal - layer * 0.11f) / (1.0f - layer * 0.11f));
            if (shapeReveal <= 0.0f) continue;
            float radius = (0.8f + layer * 0.7f + p * 1.8f) * s;
            // Sweep the tide FORWARD instead of pooling as a floor circle at the caster: the leading
            // crest rides out ahead with progress and the trailing layers form its wake behind it.
            double advance = (0.9 + p * 3.0 - layer * 0.55) * s;
            Vec3 center = o.add(f.scale(advance)).add(renderUp.scale((0.08 + layer * 0.12) * s));
            ring(b, m, center, f, r, radius,
                    (0.28f - layer * 0.035f) * s, 40,
                    color(layer == 0 ? palette.deep : layer == 3 ? palette.foam : palette.water,
                            fade * (1.0f - layer * 0.1f)));
        }
        shapeReveal = clamp((fullReveal - 0.58f) / 0.42f);
        for (int strike = -1; strike <= 1; strike++) {
            Vec3 impact = o.add(f.scale((1.3 + strike * 0.38) * s)).add(r.scale(strike * 0.42 * s));
            burst(b, m, impact, f, r, 0.16f * s, (1.05f + Math.abs(strike) * 0.18f) * s * p,
                    6, color(strike == 0 ? palette.foam : palette.light, fade * 0.68f));
        }
        shapeReveal = fullReveal;
    }

    private void drawWaterfall(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                      float s, float p, float fade, float age) {
        Vec3 center = o.add(f.scale(3.0 * s));
        int strips = 20;
        int rows = 36;
        int stripePhase = (int) Math.floor(age * 0.82f);
        float height = 6.5f * s;
        float visibleRows = shapeReveal * rows;
        int rowCount = Math.min(rows, (int) Math.ceil(visibleRows));
        for (int i = 0; i < strips; i++) {
            double x0 = (-2.5 + 5.0 * i / strips) * s;
            double x1 = (-2.5 + 5.0 * (i + 1) / strips) * s;
            double wave0 = Math.sin(i * 1.7) * 0.18 * s;
            double wave1 = Math.sin((i + 1) * 1.7) * 0.18 * s;
            int distanceFromCenter = Math.abs(i * 2 + 1 - strips);
            int stripColor = distanceFromCenter >= strips - 2 ? palette.deep
                    : distanceFromCenter <= 3 ? palette.light : palette.water;
            for (int step = 0; step < rowCount; step++) {
                int row = rows - 1 - step;
                float fraction = Math.min(1.0f, visibleRows - step);
                double y1 = height * (row + 1) / rows;
                double y0 = y1 - height * fraction / rows;
                Vec3 a = center.add(r.scale(x0)).add(f.scale(wave0)).add(renderUp.scale(y0));
                Vec3 d = center.add(r.scale(x1)).add(f.scale(wave1)).add(renderUp.scale(y0));
                Vec3 topA = center.add(r.scale(x0)).add(f.scale(wave0)).add(renderUp.scale(y1));
                Vec3 topD = center.add(r.scale(x1)).add(f.scale(wave1)).add(renderUp.scale(y1));
                int stripe = Math.floorMod(row - i * 2 + stripePhase, 12);
                int cellColor = color(stripColor, fade * 0.86f);
                if (stripe == 0) {
                    cellColor = VfxPixelRender.mixRgb(
                            color(stripColor, fade * 0.94f), palette.foam, 0.62f);
                } else if (stripe == 1) {
                    cellColor = VfxPixelRender.mixRgb(
                            color(stripColor, fade * 0.90f), palette.light, 0.38f);
                } else if (stripe == 7) {
                    cellColor = VfxPixelRender.mixRgb(cellColor, palette.deep, 0.22f);
                }
                quad(b, m, a, d, topD, topA, cellColor);
            }
        }
        float impact = clamp((shapeReveal - 0.70f) / 0.30f);
        ring(b, m, center.add(renderUp.scale(0.05)), f, r, 3.2f * s * impact, 0.35f * s,
                40, color(palette.foam, fade * impact));
        burst(b, m, center.add(renderUp.scale(0.08 * s)), f, r,
                0.65f * s, 2.1f * s * impact, 12, color(palette.light, fade * impact * 0.68f));
    }

    private void drawSplashingFlow(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                          float s, float p, float fade, float age) {
        float length = (1.0f + 8.0f * ease(p)) * s;
        stripedRibbon(b, m, 32, t -> {
            double zigzag = Math.sin(t * Math.PI * 5.0) * 1.05 * s;
            return o.add(f.scale(t * length)).add(r.scale(zigzag)).add(renderUp.scale(0.15 * s));
        }, r, t -> (float) ((0.24 + 0.25 * Math.sin(t * Math.PI)) * s),
                color(palette.water, fade), palette.light, age, 5);
        ribbon(b, m, 32, t -> o.add(f.scale(t * length)).add(r.scale(Math.sin(t * Math.PI * 5.0) * 1.05 * s))
                        .add(renderUp.scale(0.22 * s)), r, t -> 0.07f * s, color(palette.foam, fade));
    }

    private void drawWhirlpool(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                      float s, float p, float fade, float age) {
        int segments = 72;
        float fullReveal = shapeReveal;
        for (int layer = 0; layer < 3; layer++) {
            final int currentLayer = layer;
            shapeReveal = clamp((fullReveal - layer * 0.10f) / (1.0f - layer * 0.10f));
            if (shapeReveal <= 0.0f) continue;
            stripedRibbon(b, m, segments, t -> {
                double angle = t * Math.PI * 5.0 + age * 0.16 + currentLayer * 2.1;
                double radius = (2.8 - t * 1.6 + currentLayer * 0.22) * s * p;
                return o.add(f.scale(Math.cos(angle) * radius)).add(r.scale(Math.sin(angle) * radius))
                        .add(renderUp.scale(t * 4.2 * s));
            }, r, t -> (0.12f + currentLayer * 0.06f) * s,
                    color(currentLayer == 0 ? palette.deep : currentLayer == 2 ? palette.foam : palette.water, fade),
                    currentLayer == 0 ? palette.water : palette.light, age + currentLayer * 2.0f, 6);
        }
        shapeReveal = fullReveal;
    }

    private void drawBlessedRain(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                        float s, float p, float fade) {
        for (int i = -4; i <= 4; i++) {
            double offset = i * 0.45 * s;
            Vec3 top = o.add(r.scale(offset)).subtract(f.scale(1.0 * s)).add(renderUp.scale(5.5 * s));
            Vec3 bottom = o.add(r.scale(offset * 0.5)).add(f.scale((2.5 + Math.abs(i) * 0.12) * s));
            Vec3 visibleBottom = top.add(bottom.subtract(top).scale(ease(p)));
            Vec3 side = r.scale((i == 0 ? 0.15 : 0.09) * s);
            quad(b, m, top.subtract(side), top.add(side), visibleBottom.add(side), visibleBottom.subtract(side),
                    waterShade(color(i == 0 ? palette.foam : palette.light,
                            fade * (i == 0 ? 1.0f : 0.72f)), (i + 4.5f) / 9.0f));
        }
        ring(b, m, o.add(f.scale(2.5 * s)), f, r, 2.4f * s * p, 0.22f * s, 36, color(palette.water, fade));
    }

    private void drawBlessedRainLeap(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                     float s, float p, float fade, float age) {
        Vec3 center = o.add(renderUp.scale(0.12 * s));
        ring(b, m, center, f, r, (0.55f + 1.45f * ease(p)) * s, 0.16f * s, 24,
                color(palette.light, fade * 0.82f));

        float fullReveal = shapeReveal;
        for (int stream = 0; stream < 4; stream++) {
            final int current = stream;
            shapeReveal = clamp((fullReveal - current * 0.07f) / (1.0f - current * 0.07f));
            if (shapeReveal <= 0.0f) continue;
            stripedRibbon(b, m, 28, t -> {
                double angle = current * Math.PI * 0.5 + t * Math.PI * 0.72 + age * 0.035;
                double radius = (1.35 - t * 1.02) * s;
                return center.add(f.scale(Math.cos(angle) * radius))
                        .add(r.scale(Math.sin(angle) * radius))
                        .add(renderUp.scale((0.10 + t * 4.8) * s));
            }, r, t -> (float) ((0.055 + Math.sin(t * Math.PI) * 0.07) * s),
                    color(current == 0 ? palette.foam : palette.light, fade * (0.90f - current * 0.08f)),
                    palette.foam, age + current, 7);
        }
        shapeReveal = fullReveal;
    }

    private void drawConstantFlux(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                         float s, float p, float fade, float age) {
        float length = (1.5f + 9.0f * ease(p)) * s;
        DoubleFunction<Vec3> spine = t -> o.add(f.scale(t * length))
                .add(r.scale(Math.sin(t * Math.PI * 4.0 + age * 0.12) * (0.5 + t) * s))
                .add(renderUp.scale((0.55 + Math.sin(t * Math.PI * 3.0) * 0.65 + t * 1.7) * s));
        stripedRibbon(b, m, 52, spine, r, t -> (float) ((0.18 + t * 0.48) * s),
                color(palette.water, fade), palette.light, age, 5);
        ribbon(b, m, 52, t -> spine.apply(t).add(renderUp.scale(0.12 * s)), r,
                t -> 0.08f * s, color(palette.foam, fade));
        if (shapeReveal < 0.58f) return;
        float headReveal = clamp((shapeReveal - 0.58f) / 0.42f);
        Vec3 head = spine.apply(shapeReveal);
        Vec3 side = r.scale(0.42 * s);
        Vec3 up = renderUp.scale(0.42 * s);
        quad(b, m, head.subtract(side), head.add(up), head.add(side), head.subtract(up),
                VfxPixelRender.mixRgb(color(palette.light, fade * headReveal), 0xD3FFF6, 0.42f));
        burst(b, m, head, r, renderUp, 0.46f * s, 1.65f * s, 12,
                color(palette.light, fade * headReveal * 0.76f));
        Vec3 eyes = head.add(f.scale(0.16 * s)).add(renderUp.scale(0.08 * s));
        Vec3 eyeSize = r.scale(0.075 * s);
        quad(b, m, eyes.subtract(r.scale(0.28 * s)).subtract(eyeSize),
                eyes.subtract(r.scale(0.28 * s)).add(renderUp.scale(0.10 * s)),
                eyes.subtract(r.scale(0.28 * s)).add(eyeSize), eyes.subtract(r.scale(0.28 * s)),
                color(0xD3FFF6, fade * headReveal));
        quad(b, m, eyes.add(r.scale(0.28 * s)).subtract(eyeSize),
                eyes.add(r.scale(0.28 * s)).add(renderUp.scale(0.10 * s)),
                eyes.add(r.scale(0.28 * s)).add(eyeSize), eyes.add(r.scale(0.28 * s)),
                color(0xD3FFF6, fade * headReveal));
    }

    private void drawDeadCalm(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 f, Vec3 r,
                                     float s, float p, float fade, float age) {
        Vec3 center = o.add(renderUp.scale(0.125 * s));
        float fullReveal = shapeReveal;
        for (int i = 1; i <= 8; i++) {
            shapeReveal = clamp((fullReveal - (i - 1) * 0.065f) / (1.0f - (i - 1) * 0.065f));
            if (shapeReveal <= 0.0f) continue;
            float radius = (i * 1.28f + (age * 0.055f % 1.0f)) * s * p;
            ring(b, m, center.add(renderUp.scale(0.04 + i * 0.009)), f, r, radius, 0.11f * s, 64,
                    color(i % 3 == 0 ? palette.foam : palette.light, fade * (0.96f - i * 0.045f)));
        }
        shapeReveal = clamp((fullReveal - 0.38f) / 0.62f);
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0 + age * 0.008;
            Vec3 dir = f.scale(Math.cos(angle)).add(r.scale(Math.sin(angle)));
            Vec3 start = center.add(dir.scale(1.0 * s));
            Vec3 end = center.add(dir.scale(8.0 * s * p));
            Vec3 side = dir.cross(renderUp).normalize().scale(0.10 * s);
            quad(b, m, start.subtract(side), start.add(side), end.add(side), end.subtract(side),
                    waterShade(color(palette.foam, fade * 0.72f), (i + 0.5f) / 8.0f));
        }
        shapeReveal = fullReveal;
    }

    private void ring(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 axisA, Vec3 axisB,
                      float radius, float width, int segments, int color) {
        int count = Math.min(segments, 16);
        float visible = shapeReveal * count;
        int visibleSegments = Math.min(count, (int) Math.ceil(visible));
        float innerRadius = Math.max(0.0f, radius - width * 0.5f);
        float outerRadius = radius + width * 0.5f;
        for (int i = 0; i < visibleSegments; i++) {
            float end = Math.min(1.0f, visible - i);
            double a0 = Math.PI * 2.0 * i / count;
            double a1 = Math.PI * 2.0 * (i + end) / count;
            Vec3 inner0 = ellipse(center, axisA, axisB, innerRadius, a0);
            Vec3 inner1 = ellipse(center, axisA, axisB, innerRadius, a1);
            Vec3 outer1 = ellipse(center, axisA, axisB, outerRadius, a1);
            Vec3 outer0 = ellipse(center, axisA, axisB, outerRadius, a0);
            quad(b, m, inner0, inner1, outer1, outer0,
                    waterShade(color, (i + 0.5f) / count));
        }
    }

    private static Vec3 ellipse(Vec3 center, Vec3 a, Vec3 b, float radius, double angle) {
        return center.add(a.scale(Math.cos(angle) * radius)).add(b.scale(Math.sin(angle) * radius));
    }

    private void ribbon(BufferBuilder b, Matrix4f m, int segments, DoubleFunction<Vec3> path,
                        Vec3 widthAxis, DoubleFunction<Float> halfWidth, int color) {
        int count = Math.min(segments, 16);
        float visible = shapeReveal * count;
        int visibleSegments = Math.min(count, (int) Math.ceil(visible));
        for (int i = 0; i < visibleSegments; i++) {
            double t0 = i / (double) count;
            double t1 = (i + Math.min(1.0f, visible - i)) / count;
            Vec3 p0 = path.apply(t0);
            Vec3 p1 = path.apply(t1);
            Vec3 w0 = widthAxis.scale(halfWidth.apply(t0));
            Vec3 w1 = widthAxis.scale(halfWidth.apply(t1));
            quad(b, m, p0.subtract(w0), p1.subtract(w1), p1.add(w1), p0.add(w0),
                    waterShade(color, (i + 0.5f) / count));
        }
    }

    private void stripedRibbon(BufferBuilder b, Matrix4f m, int segments, DoubleFunction<Vec3> path,
                               Vec3 widthAxis, DoubleFunction<Float> halfWidth, int color,
                               int stripeRgb, float age, int spacing) {
        int count = Math.min(segments, 24);
        float visible = shapeReveal * count;
        int visibleSegments = Math.min(count, (int) Math.ceil(visible));
        int phase = (int) Math.floor(age * 0.48f);
        for (int i = 0; i < visibleSegments; i++) {
            double t0 = i / (double) count;
            double t1 = (i + Math.min(1.0f, visible - i)) / count;
            Vec3 p0 = path.apply(t0);
            Vec3 p1 = path.apply(t1);
            Vec3 w0 = widthAxis.scale(halfWidth.apply(t0));
            Vec3 w1 = widthAxis.scale(halfWidth.apply(t1));
            int segmentColor = waterShade(color, (i + 0.5f) / count);
            if (Math.floorMod(i - phase, spacing) == 0) {
                segmentColor = VfxPixelRender.mixRgb(segmentColor, stripeRgb, 0.58f);
            }
            quad(b, m, p0.subtract(w0), p1.subtract(w1), p1.add(w1), p0.add(w0), segmentColor);
        }
    }

    private void drawMovementTrail(BufferBuilder b, Matrix4f m, VfxInstance instance, Camera camera,
                                   Vec3 right, float scale, float fade) {
        var points = instance.originHistory();
        Vec3 currentOrigin = instance.origin();
        for (int i = 1; i < points.size(); i++) {
            if (points.get(i).subtract(currentOrigin).dot(instance.direction()) > 0.12) continue;
            Vec3 a = points.get(i - 1).subtract(camera.getPosition()).add(renderUp.scale(0.45 * scale));
            Vec3 c = points.get(i).subtract(camera.getPosition()).add(renderUp.scale(0.45 * scale));
            if (a.distanceToSqr(c) < 0.0025) continue;
            float life = i / (float) points.size();
            float width = (0.17f + life * 0.35f) * scale;
            int body = waterShade(color(palette.water, fade * life * 0.70f), life);
            quad(b, m, a.subtract(right.scale(width)), c.subtract(right.scale(width * 0.82)),
                    c.add(right.scale(width * 0.82)), a.add(right.scale(width)), body);
            if (i % 3 == 0) {
                float foamWidth = width * 0.22f;
                quad(b, m, a.subtract(right.scale(foamWidth)), c.subtract(right.scale(foamWidth)),
                        c.add(right.scale(foamWidth)), a.add(right.scale(foamWidth)),
                        color(palette.foam, fade * life * 0.62f));
            }
            if (style == Style.SPLASHING_FLOW && i % 3 == 0) {
                ring(b, m, c.subtract(renderUp.scale(0.35 * scale)), instance.direction(), right,
                        0.42f * scale, 0.09f * scale, 12, color(palette.foam, fade * life * 0.56f));
            }
        }
    }

    private void burst(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 axisA, Vec3 axisB,
                              float innerRadius, float length, int count, int color) {
        float contact = clamp((shapeReveal - 0.58f) / 0.42f);
        int visibleSpikes = Math.min(count, (int) Math.ceil(contact * count));
        for (int i = 0; i < visibleSpikes; i++) {
            double angle = Math.PI * 2.0 * i / count;
            Vec3 radial = axisA.scale(Math.cos(angle)).add(axisB.scale(Math.sin(angle)));
            Vec3 tangent = axisA.scale(-Math.sin(angle)).add(axisB.scale(Math.cos(angle)));
            float width = length * (i % 2 == 0 ? 0.065f : 0.045f);
            float reach = length * contact * (i % 3 == 0 ? 1.0f : 0.72f);
            Vec3 root = center.add(radial.scale(innerRadius));
            Vec3 tip = center.add(radial.scale(innerRadius + reach));
            quad(b, m, root.subtract(tangent.scale(width)), tip, tip,
                    root.add(tangent.scale(width)), color);
        }
    }

    private static void quad(BufferBuilder b, Matrix4f m, Vec3 a, Vec3 c, Vec3 d, Vec3 e, int color) {
        VfxPixelRender.quad(b, m, (float) a.x, (float) a.y, (float) a.z,
                (float) c.x, (float) c.y, (float) c.z, (float) d.x, (float) d.y, (float) d.z,
                (float) e.x, (float) e.y, (float) e.z, color);
    }

    private static Vec3 right(Vec3 forward) {
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        return right.lengthSqr() > 1.0E-6 ? right.normalize() : new Vec3(1, 0, 0);
    }
    private static float clamp(float v) { return Math.max(0, Math.min(1, v)); }
    private static float smooth(float v) { return v * v * (3.0f - 2.0f * v); }
    private static float ease(float v) { float i = 1 - v; return 1 - i * i * i; }
    private static int waterShade(int color, float position) {
        if (position < 0.16f) return VfxPixelRender.mixRgb(color, 0x061A46, 0.58f);
        if (position < 0.40f) return VfxPixelRender.mixRgb(color, 0x004E86, 0.38f);
        if (position < 0.66f) return VfxPixelRender.mixRgb(color, 0xA4F1FB, 0.46f);
        if (position < 0.84f) return VfxPixelRender.mixRgb(color, 0x34D1FD, 0.18f);
        return VfxPixelRender.mixRgb(color, 0x0B3D73, 0.42f);
    }
    private static int color(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private record Palette(int deep, int water, int light, int foam) {}
}
