package com.xirc.nichirin.client.vfx.effect;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.client.vfx.VfxEffect;
import com.xirc.nichirin.client.vfx.VfxInstance;
import com.xirc.nichirin.client.vfx.VfxPixelRender;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class WaterSurfaceSlashEffect implements VfxEffect {
    private static final int LIFETIME = 26;
    private static final int SEGMENTS = 12;
    private static final int DEEP_WATER = 0xC0004E86;
    private static final int WATER = 0xD81B93F0;
    private static final int LIGHT_WATER = 0xE867EDFF;
    private static final int FOAM = 0xF2EEFBFD;
    private final boolean reverse;

    public WaterSurfaceSlashEffect() {
        this(false);
    }

    public WaterSurfaceSlashEffect(boolean reverse) {
        this.reverse = reverse;
    }

    @Override
    public int lifetimeTicks() {
        return LIFETIME;
    }

    @Override
    public void render(VfxInstance instance, PoseStack poseStack, Camera camera, float partialTick) {
        float age = instance.ageTicks() + partialTick;
        float progress = clamp(age / 9.0f);
        float fade = 1.0f - clamp((age - 15.0f) / 11.0f);
        if (progress <= 0.0f || fade <= 0.0f) return;

        Vec3 origin = instance.origin(partialTick).subtract(camera.getPosition());
        Vec3 forward = instance.direction().normalize();
        Vec3 right = rightOf(forward);
        Vec3 up = right.cross(forward).normalize();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = VfxPixelRender.beginQuads();

        float scale = instance.scale();
        float radius = (0.85f + 3.55f * easeOut(progress)) * scale;
        float alpha = fade * fade;
        float reveal = smooth(progress);
        drawMovementHistory(buffer, matrix, instance, camera, right, up, scale, alpha);
        drawTrailingWake(buffer, matrix, origin, forward, right, up, radius, scale, progress, alpha, age,
                clamp((reveal - 0.16f) / 0.84f), reverse);
        drawSurfaceArc(buffer, matrix, origin, forward, right, up, radius, scale, alpha, age, reveal, reverse);
        drawWaveCrest(buffer, matrix, origin, forward, right, up, radius, scale, progress, alpha, age,
                clamp((reveal - 0.08f) / 0.92f), reverse);
        VfxPixelRender.finish(buffer);
    }

    private static void drawSurfaceArc(BufferBuilder buffer, Matrix4f matrix, Vec3 origin,
                                       Vec3 forward, Vec3 right, Vec3 up, float radius, float scale,
                                       float alpha, float age, float reveal, boolean reverse) {
        int stripePhase = (int) Math.floor(age * 0.22f);
        float visible = reveal * SEGMENTS;
        int visibleSegments = Math.min(SEGMENTS, (int) Math.ceil(visible));
        for (int i = 0; i < visibleSegments; i++) {
            float end = Math.min(1.0f, visible - i);
            float shadePosition = (i + 0.5f) / SEGMENTS;
            double a0 = slashAngle(82.0, 164.0, i, SEGMENTS, reverse);
            double a1 = slashAngle(82.0, 164.0, i + end, SEGMENTS, reverse);
            Vec3 inner0 = arcPoint(origin, forward, right, up, radius - 0.48f * scale, a0, 0.10f * scale);
            Vec3 inner1 = arcPoint(origin, forward, right, up, radius - 0.48f * scale, a1, 0.10f * scale);
            Vec3 bottom0 = arcPoint(origin, forward, right, up, radius - 0.68f * scale, a0, -0.10f * scale);
            Vec3 bottom1 = arcPoint(origin, forward, right, up, radius - 0.68f * scale, a1, -0.10f * scale);
            Vec3 middle0 = arcPoint(origin, forward, right, up, radius - 0.20f * scale, a0, 0.16f * scale);
            Vec3 middle1 = arcPoint(origin, forward, right, up, radius - 0.20f * scale, a1, 0.16f * scale);
            Vec3 outer0 = arcPoint(origin, forward, right, up, radius + 0.10f * scale, a0, 0.23f * scale);
            Vec3 outer1 = arcPoint(origin, forward, right, up, radius + 0.10f * scale, a1, 0.23f * scale);
            quad(buffer, matrix, bottom0, bottom1, inner1, inner0,
                    waterShade(withAlpha(DEEP_WATER, alpha * 0.72f), shadePosition));
            quad(buffer, matrix, inner0, inner1, middle1, middle0,
                    waterShade(withAlpha(DEEP_WATER, alpha * 0.88f), shadePosition));
            quad(buffer, matrix, middle0, middle1, outer1, outer0,
                    flowingShade(withAlpha(WATER, alpha), shadePosition, i, stripePhase));
        }
    }

    private static void drawWaveCrest(BufferBuilder buffer, Matrix4f matrix, Vec3 origin,
                                      Vec3 forward, Vec3 right, Vec3 up, float radius, float scale,
                                      float progress, float alpha, float age, float reveal, boolean reverse) {
        int stripePhase = (int) Math.floor(age * 0.22f);
        float visible = reveal * SEGMENTS;
        int visibleSegments = Math.min(SEGMENTS, (int) Math.ceil(visible));
        for (int i = 0; i < visibleSegments; i++) {
            float end = Math.min(1.0f, visible - i);
            float shadePosition = (i + 0.5f) / SEGMENTS;
            double a0 = slashAngle(78.0, 156.0, i, SEGMENTS, reverse);
            double a1 = slashAngle(78.0, 156.0, i + end, SEGMENTS, reverse);
            double crest0 = Math.pow(Math.max(0.0, Math.cos(a0)), 1.45);
            double crest1 = Math.pow(Math.max(0.0, Math.cos(a1)), 1.45);
            float base0 = (float) (0.22 + crest0 * 0.56 * progress) * scale;
            float base1 = (float) (0.22 + crest1 * 0.56 * progress) * scale;
            float top0 = base0 + (float) (0.22 + crest0 * 0.58) * scale;
            float top1 = base1 + (float) (0.22 + crest1 * 0.58) * scale;
            Vec3 low0 = arcPoint(origin, forward, right, up, radius - 0.04f * scale, a0, base0);
            Vec3 low1 = arcPoint(origin, forward, right, up, radius - 0.04f * scale, a1, base1);
            Vec3 high0 = arcPoint(origin, forward, right, up, radius + 0.02f * scale, a0, top0);
            Vec3 high1 = arcPoint(origin, forward, right, up, radius + 0.02f * scale, a1, top1);
            quad(buffer, matrix, low0, low1, high1, high0,
                    flowingShade(withAlpha(LIGHT_WATER, alpha), shadePosition, i, stripePhase));

            Vec3 foam0 = high0.add(up.scale(foamHeight(i) * scale));
            Vec3 foam1 = high1.add(up.scale(foamHeight(i + 1) * scale));
            int foamColor = waterShade(withAlpha(FOAM, alpha * 0.94f), shadePosition);
            if (Math.floorMod(i - stripePhase, 6) == 0) {
                foamColor = VfxPixelRender.mixRgb(foamColor, 0xFFFFFF, 0.62f);
            }
            quad(buffer, matrix, high0, high1, foam1, foam0,
                    foamColor);
        }
    }

    private static void drawTrailingWake(BufferBuilder buffer, Matrix4f matrix, Vec3 origin,
                                         Vec3 forward, Vec3 right, Vec3 up, float radius, float scale,
                                         float progress, float alpha, float age, float reveal, boolean reverse) {
        float wakeRadius = Math.max(0.32f * scale, radius - 0.82f * scale);
        int phase = (int) Math.floor(age * 0.18f);
        int wakeSegments = 10;
        float visible = reveal * wakeSegments;
        int visibleSegments = Math.min(wakeSegments, (int) Math.ceil(visible));
        for (int i = 0; i < visibleSegments; i++) {
            float end = Math.min(1.0f, visible - i);
            double a0 = slashAngle(70.0, 140.0, i, wakeSegments, reverse);
            double a1 = slashAngle(70.0, 140.0, i + end, wakeSegments, reverse);
            float lift0 = (0.03f + (float) Math.cos(a0) * 0.12f * progress) * scale;
            float lift1 = (0.03f + (float) Math.cos(a1) * 0.12f * progress) * scale;
            Vec3 inner0 = arcPoint(origin, forward, right, up, wakeRadius - 0.12f * scale, a0, lift0);
            Vec3 inner1 = arcPoint(origin, forward, right, up, wakeRadius - 0.12f * scale, a1, lift1);
            Vec3 outer0 = arcPoint(origin, forward, right, up, wakeRadius + 0.08f * scale, a0, lift0 + 0.04f * scale);
            Vec3 outer1 = arcPoint(origin, forward, right, up, wakeRadius + 0.08f * scale, a1, lift1 + 0.04f * scale);
            int color = flowingShade(withAlpha(LIGHT_WATER, alpha * 0.46f),
                    (i + 0.5f) / wakeSegments, i, phase);
            quad(buffer, matrix, inner0, inner1, outer1, outer0, color);
        }
    }

    private static float foamHeight(int point) {
        return switch (Math.floorMod(point, 4)) {
            case 0 -> 0.28f;
            case 1 -> 0.14f;
            case 2 -> 0.22f;
            default -> 0.11f;
        };
    }

    private static Vec3 arcPoint(Vec3 origin, Vec3 forward, Vec3 right, Vec3 up,
                                 float radius, double angle, float y) {
        return origin.add(forward.scale(Math.cos(angle) * radius))
                .add(right.scale(Math.sin(angle) * radius))
                .add(up.scale(y));
    }

    private static double slashAngle(double halfArc, double span, double segment, int count,
                                     boolean reverse) {
        double degrees = -halfArc + span * segment / count;
        return Math.toRadians(reverse ? -degrees : degrees);
    }

    private static void drawMovementHistory(BufferBuilder buffer, Matrix4f matrix, VfxInstance instance,
                                            Camera camera, Vec3 right, Vec3 up, float scale, float alpha) {
        var points = instance.originHistory();
        Vec3 currentOrigin = instance.origin();
        for (int i = 1; i < points.size(); i++) {
            if (points.get(i).subtract(currentOrigin).dot(instance.direction()) > 0.12) continue;
            Vec3 a = points.get(i - 1).subtract(camera.getPosition()).add(up.scale(0.55 * scale));
            Vec3 b = points.get(i).subtract(camera.getPosition()).add(up.scale(0.55 * scale));
            if (a.distanceToSqr(b) < 0.0025) continue;
            float width = (0.16f + 0.22f * i / points.size()) * scale;
            int color = withAlpha(i % 3 == 0 ? FOAM : WATER, alpha * i / points.size() * 0.72f);
            quad(buffer, matrix, a.subtract(right.scale(width)), b.subtract(right.scale(width)),
                    b.add(right.scale(width)), a.add(right.scale(width)), color);
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

    private static Vec3 rightOf(Vec3 forward) {
        Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
        return right.lengthSqr() > 1.0E-6 ? right.normalize() : new Vec3(1.0, 0.0, 0.0);
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(255.0f * alpha)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float easeOut(float value) {
        float inverse = 1.0f - value;
        return 1.0f - inverse * inverse * inverse;
    }

    private static float smooth(float value) {
        return value * value * (3.0f - 2.0f * value);
    }

    private static int waterShade(int color, float position) {
        if (position < 0.17f) return VfxPixelRender.mixRgb(color, 0x061A46, 0.58f);
        if (position < 0.42f) return VfxPixelRender.mixRgb(color, 0x004E86, 0.38f);
        if (position < 0.67f) return VfxPixelRender.mixRgb(color, 0xA4F1FB, 0.46f);
        if (position < 0.84f) return VfxPixelRender.mixRgb(color, 0x34D1FD, 0.18f);
        return VfxPixelRender.mixRgb(color, 0x0B3D73, 0.42f);
    }

    private static int flowingShade(int color, float position, int segment, int phase) {
        int shaded = waterShade(color, position);
        if (Math.floorMod(segment - phase, 6) == 0) {
            return VfxPixelRender.mixRgb(shaded, 0xA4F1FB, 0.58f);
        }
        return shaded;
    }
}
