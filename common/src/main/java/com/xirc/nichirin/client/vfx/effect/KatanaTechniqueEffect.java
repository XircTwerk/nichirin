package com.xirc.nichirin.client.vfx.effect;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.client.vfx.VfxEffect;
import com.xirc.nichirin.client.vfx.VfxInstance;
import com.xirc.nichirin.client.vfx.VfxPixelRender;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Minimal neutral-katana accents made from connected pixel geometry, never particles. */
public final class KatanaTechniqueEffect implements VfxEffect {
    private static final int SHADOW = 0xA8656565;
    private static final int STEEL = 0xD8C8C8C8;
    private static final int EDGE = 0xEEFFFFFF;

    public enum Style {
        CHECK,
        PIERCING_FINISH
    }

    private final Style style;

    public KatanaTechniqueEffect(Style style) {
        this.style = style;
    }

    @Override
    public int lifetimeTicks() {
        return style == Style.PIERCING_FINISH ? 12 : 9;
    }

    @Override
    public void render(VfxInstance instance, PoseStack poseStack, Camera camera, float partialTick) {
        float age = instance.ageTicks() + partialTick;
        float fadeStart = style == Style.PIERCING_FINISH ? 5.0f : 4.0f;
        float fade = 1.0f - clamp((age - fadeStart) / (lifetimeTicks() - fadeStart));
        if (fade <= 0.0f) return;

        Vec3 origin = instance.origin(partialTick).subtract(camera.getPosition());
        Vec3 forward = instance.direction().normalize();
        Vec3 right = rightOf(forward);
        Vec3 up = right.cross(forward).normalize();
        float scale = instance.scale();
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = VfxPixelRender.beginQuads();

        if (style == Style.CHECK) {
            drawCheck(buffer, matrix, origin, forward, right, up,
                    scale, clamp(age / 2.0f), fade);
        } else {
            drawPiercingFinish(buffer, matrix, origin, forward, right, up,
                    scale, clamp(age / 3.0f), fade);
        }

        VfxPixelRender.finish(buffer);
    }

    private static void drawCheck(BufferBuilder buffer, Matrix4f matrix, Vec3 origin,
                                  Vec3 forward, Vec3 right, Vec3 up, float scale,
                                  float reveal, float fade) {
        if (reveal <= 0.0f) return;
        Vec3 center = origin.add(forward.scale(0.702f * scale));
        float reach = 0.306f * scale * reveal;
        float width = 0.066f * scale;
        drawRibbon(buffer, matrix, center.subtract(right.scale(reach)), center.add(right.scale(reach)),
                up, width, withAlpha(STEEL, fade * 0.82f));
        drawRibbon(buffer, matrix, center.subtract(up.scale(reach)), center.add(up.scale(reach)),
                right, width, withAlpha(EDGE, fade));
    }

    private static void drawPiercingFinish(BufferBuilder buffer, Matrix4f matrix, Vec3 tip,
                                           Vec3 forward, Vec3 right, Vec3 up, float scale,
                                           float reveal, float fade) {
        if (reveal <= 0.0f) return;

        float xReach = 0.34f * scale * reveal;
        float xWidth = 0.0625f * scale;
        Vec3 diagonalA = right.add(up).normalize();
        Vec3 diagonalB = right.subtract(up).normalize();
        drawRibbon(buffer, matrix, tip.subtract(diagonalA.scale(xReach)), tip.add(diagonalA.scale(xReach)),
                diagonalB, xWidth, withAlpha(EDGE, fade));
        drawRibbon(buffer, matrix, tip.subtract(diagonalB.scale(xReach)), tip.add(diagonalB.scale(xReach)),
                diagonalA, xWidth, withAlpha(STEEL, fade * 0.9f));

        Vec3 splitPoint = tip.add(forward.scale((0.48f + 0.28f * reveal) * scale));
        float spread = 0.42f * scale * reveal;
        float lift = 0.24f * scale * reveal;
        float windWidth = 0.043f * scale;
        drawRibbon(buffer, matrix, tip, splitPoint.add(right.scale(spread)).add(up.scale(lift)),
                up, windWidth, withAlpha(EDGE, fade * 0.72f));
        drawRibbon(buffer, matrix, tip, splitPoint.subtract(right.scale(spread)).add(up.scale(lift)),
                up, windWidth, withAlpha(STEEL, fade * 0.66f));
        drawRibbon(buffer, matrix, tip, splitPoint.add(right.scale(spread)).subtract(up.scale(lift)),
                up, windWidth, withAlpha(STEEL, fade * 0.58f));
        drawRibbon(buffer, matrix, tip, splitPoint.subtract(right.scale(spread)).subtract(up.scale(lift)),
                up, windWidth, withAlpha(SHADOW, fade * 0.52f));

        for (int layer = 0; layer < 2; layer++) {
            float distance = (0.22f + layer * 0.24f) * scale;
            float radius = (0.20f + layer * 0.11f) * scale * reveal;
            Vec3 center = tip.add(forward.scale(distance));
            drawDiamond(buffer, matrix, center, right, up, radius, windWidth,
                    withAlpha(layer == 0 ? EDGE : STEEL, fade * (0.54f - layer * 0.12f)));
        }
    }

    private static void drawDiamond(BufferBuilder buffer, Matrix4f matrix, Vec3 center,
                                    Vec3 right, Vec3 up, float radius, float width, int color) {
        Vec3 top = center.add(up.scale(radius));
        Vec3 rightPoint = center.add(right.scale(radius));
        Vec3 bottom = center.subtract(up.scale(radius));
        Vec3 leftPoint = center.subtract(right.scale(radius));
        drawRibbon(buffer, matrix, top, rightPoint, up.add(right), width, color);
        drawRibbon(buffer, matrix, rightPoint, bottom, up.subtract(right), width, color);
        drawRibbon(buffer, matrix, bottom, leftPoint, up.add(right), width, color);
        drawRibbon(buffer, matrix, leftPoint, top, up.subtract(right), width, color);
    }

    private static void drawRibbon(BufferBuilder buffer, Matrix4f matrix, Vec3 start, Vec3 end,
                                   Vec3 widthAxis, float width, int color) {
        Vec3 offset = widthAxis.normalize().scale(width);
        quad(buffer, matrix, start.subtract(offset), end.subtract(offset),
                end.add(offset), start.add(offset), color);
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

    private static int withAlpha(int color, float multiplier) {
        int sourceAlpha = (color >>> 24) & 0xFF;
        int alpha = Math.max(0, Math.min(255, Math.round(sourceAlpha * clamp(multiplier))));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
