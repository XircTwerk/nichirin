package com.xirc.nichirin.client.vfx.effect;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.client.vfx.VfxEffect;
import com.xirc.nichirin.client.vfx.VfxInstance;
import com.xirc.nichirin.client.vfx.VfxPixelRender;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Pixel-built Destructive Death compass: twelve-point snow crystal, numerals, and target needle. */
public final class CompassNeedleEffect implements VfxEffect {
    public enum Style { COMPASS, COLLAPSE, ARROW }

    private static final int RED = 0xE82E52;
    private static final int DEEP_RED = 0x87152F;
    private static final int BLUE = 0x83E8FF;
    private static final int PALE = 0xD7F8FF;
    private static final int[][] DIGITS = {
            {0, 1, 2, 3, 4, 5}, {1, 2}, {0, 1, 6, 4, 3}, {0, 1, 6, 2, 3},
            {5, 6, 1, 2}, {0, 5, 6, 2, 3}, {0, 5, 6, 4, 3, 2}, {0, 1, 2},
            {0, 1, 2, 3, 4, 5, 6}, {0, 1, 2, 3, 5, 6}
    };
    private static final float[][] SEGMENTS = {
            {-0.28f, 0.50f, 0.28f, 0.50f}, {0.28f, 0.50f, 0.28f, 0.0f},
            {0.28f, 0.0f, 0.28f, -0.50f}, {-0.28f, -0.50f, 0.28f, -0.50f},
            {-0.28f, 0.0f, -0.28f, -0.50f}, {-0.28f, 0.50f, -0.28f, 0.0f},
            {-0.28f, 0.0f, 0.28f, 0.0f}
    };

    private final Style style;

    public CompassNeedleEffect(Style style) {
        this.style = style;
    }

    @Override
    public int lifetimeTicks() {
        return switch (style) {
            case COMPASS -> 220;
            case COLLAPSE -> 10;
            case ARROW -> 8;
        };
    }

    @Override
    public void render(VfxInstance instance, PoseStack poseStack, Camera camera, float partialTick) {
        float age = instance.ageTicks() + partialTick;
        float fadeIn = style == Style.COLLAPSE ? 1.0f : smooth(clamp(age / 7.0f));
        float fadeOut = 1.0f - smooth(clamp((age - instance.lifetimeTicks() + 10.0f) / 10.0f));
        float alpha = fadeIn * fadeOut;
        if (alpha <= 0.01f) return;

        Vec3 origin = instance.origin(partialTick).subtract(camera.getPosition());
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = VfxPixelRender.beginQuads();
        if (style == Style.COMPASS) {
            drawCompass(buffer, matrix, origin, instance.scale(), age, alpha);
        } else if (style == Style.COLLAPSE) {
            float collapse = smooth(clamp(age / 9.0f));
            drawCompass(buffer, matrix, origin, instance.scale() * (1.0f - collapse),
                    30.0f, alpha * (1.0f - collapse * 0.72f));
        } else {
            drawArrow(buffer, matrix, origin, instance.direction(), instance.scale(), age, alpha);
        }
        VfxPixelRender.finish(buffer);
    }

    private void drawCompass(BufferBuilder b, Matrix4f m, Vec3 o, float size, float age, float alpha) {
        float reveal = smooth(clamp(age / 14.0f));
        float pulse = 1.0f + (float) Math.sin(age * 0.23f) * 0.025f;
        int blue = color(BLUE, alpha * 0.88f);
        int pale = color(PALE, alpha * 0.96f);
        int red = color(mix(DEEP_RED, RED, clamp(age / 10.0f)), alpha * Math.max(0.0f, 1.0f - age / 18.0f));

        // Dense angular sampling plus a 1/36-block vertex grid keeps the large compass crisp.
        ring(b, m, o, 1.28f * size * pulse * reveal, 108, 0.15f, red);
        ring(b, m, o, 2.60f * size * pulse * reveal, 144, 0.14f, blue);
        ring(b, m, o, 3.62f * size * reveal, 162, 0.12f, color(BLUE, alpha * 0.55f));

        // Twelve compass hours and the six long snowflake arms seen in the anime technique.
        for (int i = 0; i < 12; i++) {
            double angle = -Math.PI / 2.0 + i * Math.PI / 6.0;
            Vec3 radial = horizontal(angle);
            Vec3 tangent = horizontal(angle + Math.PI / 2.0);
            float stagger = smooth(clamp((age - i * 0.35f) / 9.0f));
            float outer = (i % 2 == 0 ? 3.22f : 2.93f) * size * reveal;
            line(b, m, o.add(radial.scale(0.42 * size)), o.add(radial.scale(outer)),
                    i % 2 == 0 ? 0.145f : 0.105f, color(i % 2 == 0 ? PALE : BLUE, alpha * stagger));

            if (i % 2 == 0) {
                for (int branch = 1; branch <= 3; branch++) {
                    Vec3 stem = o.add(radial.scale((1.05f + branch * 0.56f) * size * reveal));
                    float branchLength = (0.48f - branch * 0.055f) * size * reveal;
                    line(b, m, stem, stem.add(radial.scale(branchLength)).add(tangent.scale(branchLength)),
                            0.090f, color(BLUE, alpha * stagger));
                    line(b, m, stem, stem.add(radial.scale(branchLength)).subtract(tangent.scale(branchLength)),
                            0.090f, color(BLUE, alpha * stagger));
                }
            }

            Vec3 numeralCenter = o.add(radial.scale(4.05f * size * reveal));
            drawNumber(b, m, numeralCenter, tangent, radial, i + 1, 0.42f * size,
                    color(PALE, alpha * stagger));
        }

        // A sparse moving scan glint gives the active compass a living, crystalline response.
        double scanAngle = -Math.PI / 2.0 + age * 0.075;
        Vec3 scan = horizontal(scanAngle);
        line(b, m, o.add(scan.scale(2.66 * size)), o.add(scan.scale(3.48 * size)), 0.21f,
                color(RED, alpha * 0.75f));
    }

    private void drawArrow(BufferBuilder b, Matrix4f m, Vec3 o, Vec3 direction,
                           float distance, float age, float alpha) {
        Vec3 forward = new Vec3(direction.x, 0.0, direction.z);
        if (forward.lengthSqr() < 1.0E-5) return;
        forward = forward.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
        float length = Math.max(0.8f, Math.min(16.0f, distance) - 0.8f);
        float pulse = 0.82f + (float) Math.sin(age * 1.4f) * 0.18f;
        Vec3 start = o.add(forward.scale(1.35));
        Vec3 tip = o.add(forward.scale(length));
        line(b, m, start, tip, 0.095f, color(BLUE, alpha * pulse));
        line(b, m, tip, tip.subtract(forward.scale(0.55)).add(side.scale(0.34)), 0.12f,
                color(PALE, alpha));
        line(b, m, tip, tip.subtract(forward.scale(0.55)).subtract(side.scale(0.34)), 0.12f,
                color(PALE, alpha));
        line(b, m, start, start.add(forward.scale(0.42)), 0.20f, color(RED, alpha * pulse));
    }

    private void drawNumber(BufferBuilder b, Matrix4f m, Vec3 center, Vec3 xAxis, Vec3 yAxis,
                            int number, float scale, int color) {
        String value = Integer.toString(number);
        float totalWidth = value.length() * 0.68f * scale;
        for (int digitIndex = 0; digitIndex < value.length(); digitIndex++) {
            int digit = value.charAt(digitIndex) - '0';
            float offset = digitIndex * 0.68f * scale - totalWidth * 0.5f + 0.34f * scale;
            Vec3 digitCenter = center.add(xAxis.scale(offset));
            for (int segment : DIGITS[digit]) {
                float[] s = SEGMENTS[segment];
                Vec3 from = digitCenter.add(xAxis.scale(s[0] * scale)).add(yAxis.scale(s[1] * scale));
                Vec3 to = digitCenter.add(xAxis.scale(s[2] * scale)).add(yAxis.scale(s[3] * scale));
                line(b, m, from, to, 0.065f, color);
            }
        }
    }

    private static void ring(BufferBuilder b, Matrix4f m, Vec3 center, float radius,
                             int segments, float width, int color) {
        if (radius < 0.05f) return;
        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2.0 * i / segments;
            double next = Math.PI * 2.0 * (i + 1) / segments;
            line(b, m, center.add(horizontal(a).scale(radius)),
                    center.add(horizontal(next).scale(radius)), width, color);
        }
    }

    private static void line(BufferBuilder b, Matrix4f m, Vec3 from, Vec3 to, float width, int color) {
        // Every solid pixel stroke gets a wider low-alpha energy sheath. Because both layers use
        // the same snapped endpoints, the aura hugs the compass instead of becoming soft clutter.
        emitLine(b, m, from.add(0.0, -0.006, 0.0), to.add(0.0, -0.006, 0.0),
                width * 3.4f, multiplyAlpha(color, 0.17f));
        emitLine(b, m, from, to, width * 1.12f, color);
    }

    private static void emitLine(BufferBuilder b, Matrix4f m, Vec3 from, Vec3 to,
                                 float width, int color) {
        Vec3 delta = to.subtract(from);
        Vec3 side = new Vec3(-delta.z, 0.0, delta.x);
        if (side.lengthSqr() < 1.0E-8) return;
        side = side.normalize().scale(width * 0.5);
        Vec3 a = from.add(side), d = from.subtract(side);
        Vec3 bPos = to.add(side), c = to.subtract(side);
        addVertex(b, m, a, color);
        addVertex(b, m, bPos, color);
        addVertex(b, m, c, color);
        addVertex(b, m, d, color);
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 point, int color) {
        float grid = 1.0f / 36.0f;
        buffer.addVertex(matrix,
                        Math.round((float) point.x / grid) * grid,
                        Math.round((float) point.y / grid) * grid,
                        Math.round((float) point.z / grid) * grid)
                .setColor(color);
    }

    private static int multiplyAlpha(int color, float multiplier) {
        int alpha = Math.max(0, Math.min(255,
                Math.round(((color >>> 24) & 255) * multiplier)));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static Vec3 horizontal(double angle) {
        return new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
    }

    private static int color(int rgb, float alpha) {
        return (Math.max(0, Math.min(255, Math.round(alpha * 255.0f))) << 24) | rgb;
    }

    private static int mix(int from, int to, float amount) {
        int r = Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * amount);
        int g = Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * amount);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * amount);
        return (r << 16) | (g << 8) | b;
    }

    private static float clamp(float value) { return Math.max(0.0f, Math.min(1.0f, value)); }
    private static float smooth(float value) { return value * value * (3.0f - 2.0f * value); }
}
