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
    public static final float GRID = 1.0f / 8.0f;

    private VfxPixelRender() {}

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
        buffer.addVertex(matrix, snap(ax), snap(ay), snap(az)).setColor(color);
        buffer.addVertex(matrix, snap(bx), snap(by), snap(bz)).setColor(color);
        buffer.addVertex(matrix, snap(cx), snap(cy), snap(cz)).setColor(color);
        buffer.addVertex(matrix, snap(dx), snap(dy), snap(dz)).setColor(color);
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
