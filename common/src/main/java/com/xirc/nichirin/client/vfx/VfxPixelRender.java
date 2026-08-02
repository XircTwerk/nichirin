package com.xirc.nichirin.client.vfx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public final class VfxPixelRender {
    public static final float GRID = 1.0f / 16.0f;
    private static float opacityMultiplier = 1.0f;
    private static boolean ownFirstPerson;

    private VfxPixelRender() {}

    public static void setRenderContext(float opacity, boolean isOwnFirstPerson) {
        opacityMultiplier = Math.max(0.0f, Math.min(1.0f, opacity));
        ownFirstPerson = isOwnFirstPerson;
    }

    public static void clearRenderContext() {
        opacityMultiplier = 1.0f;
        ownFirstPerson = false;
    }

    public static boolean isOwnFirstPerson() {
        return ownFirstPerson;
    }

    public static BufferBuilder beginQuads() {
        if (VfxShaderHolder.getShader() != null) {
            RenderSystem.setShader(VfxShaderHolder::getShader);
        } else {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        return Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    public static void finish(BufferBuilder buffer) {
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    public static float snap(double value) {
        return (float) (Math.round(value / GRID) * GRID);
    }

    public static void quad(BufferBuilder buffer, Matrix4f matrix,
                            float ax, float ay, float az, float bx, float by, float bz,
                            float cx, float cy, float cz, float dx, float dy, float dz, int color) {
        int adjusted = multiplyAlpha(color, opacityMultiplier);
        addQuad(buffer, matrix, ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, adjusted);

        // Near-edge-on flat faces disappear in first person even with culling disabled. Add a
        // camera-facing copy only in that case; ordinary third-person silhouettes stay untouched.
        org.joml.Vector3f a = new org.joml.Vector3f(ax, ay, az);
        org.joml.Vector3f b = new org.joml.Vector3f(bx, by, bz);
        org.joml.Vector3f c = new org.joml.Vector3f(cx, cy, cz);
        org.joml.Vector3f d = new org.joml.Vector3f(dx, dy, dz);
        org.joml.Vector3f normal = new org.joml.Vector3f(b).sub(a).cross(new org.joml.Vector3f(d).sub(a));
        org.joml.Vector3f center = new org.joml.Vector3f(a).add(b).add(c).add(d).mul(0.25f);
        if (normal.lengthSquared() > 1.0E-8f && center.lengthSquared() > 1.0E-8f) {
            float visibility = Math.abs(normal.normalize().dot(new org.joml.Vector3f(center).normalize()));
            if (visibility < 0.18f) {
                org.joml.Vector3f start = new org.joml.Vector3f(a).add(d).mul(0.5f);
                org.joml.Vector3f end = new org.joml.Vector3f(b).add(c).mul(0.5f);
                org.joml.Vector3f axis = new org.joml.Vector3f(end).sub(start);
                float startWidth = a.distance(d) * 0.5f;
                float endWidth = b.distance(c) * 0.5f;
                org.joml.Vector3f view = new org.joml.Vector3f(center).normalize();
                org.joml.Vector3f width = view.cross(new org.joml.Vector3f(axis));
                if (axis.lengthSquared() > 1.0E-8f && width.lengthSquared() > 1.0E-8f) {
                    width.normalize();
                    org.joml.Vector3f sa = new org.joml.Vector3f(start).sub(new org.joml.Vector3f(width).mul(startWidth));
                    org.joml.Vector3f sb = new org.joml.Vector3f(end).sub(new org.joml.Vector3f(width).mul(endWidth));
                    org.joml.Vector3f sc = new org.joml.Vector3f(end).add(new org.joml.Vector3f(width).mul(endWidth));
                    org.joml.Vector3f sd = new org.joml.Vector3f(start).add(new org.joml.Vector3f(width).mul(startWidth));
                    addQuad(buffer, matrix, sa.x, sa.y, sa.z, sb.x, sb.y, sb.z,
                            sc.x, sc.y, sc.z, sd.x, sd.y, sd.z, adjusted);
                }
            }
        }
    }

    private static void addQuad(BufferBuilder buffer, Matrix4f matrix,
                                float ax, float ay, float az, float bx, float by, float bz,
                                float cx, float cy, float cz, float dx, float dy, float dz, int color) {
        buffer.addVertex(matrix, snap(ax), snap(ay), snap(az)).setColor(color);
        buffer.addVertex(matrix, snap(bx), snap(by), snap(bz)).setColor(color);
        buffer.addVertex(matrix, snap(cx), snap(cy), snap(cz)).setColor(color);
        buffer.addVertex(matrix, snap(dx), snap(dy), snap(dz)).setColor(color);
    }

    private static int multiplyAlpha(int color, float multiplier) {
        int alpha = Math.max(0, Math.min(255, Math.round(((color >>> 24) & 0xFF) * multiplier)));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public static int mixRgb(int color, int targetRgb, float amount) {
        amount = Math.max(0.0f, Math.min(1.0f, amount));
        int alpha = (color >>> 24) & 0xFF;
        int sourceRed = (color >>> 16) & 0xFF;
        int sourceGreen = (color >>> 8) & 0xFF;
        int sourceBlue = color & 0xFF;
        int red = Math.round(sourceRed + (((targetRgb >>> 16) & 0xFF) - sourceRed) * amount);
        int green = Math.round(sourceGreen + (((targetRgb >>> 8) & 0xFF) - sourceGreen) * amount);
        int blue = Math.round(sourceBlue + ((targetRgb & 0xFF) - sourceBlue) * amount);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
