package com.xirc.nichirin.client.vfx;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

/**
 * Renders the complete VFX layer at a low resolution, then scales it back to the main
 * framebuffer with nearest-neighbour filtering.  Unlike a color pattern applied to the
 * polygons, this pixelizes the actual silhouette, animation and every internal water layer.
 */
@Environment(EnvType.CLIENT)
public final class VfxPixelRenderPass {
    private static final int PIXEL_SCALE = 8;
    private static TextureTarget target;

    private VfxPixelRenderPass() {}

    public static void render(PoseStack poseStack, Camera camera, float partialTick) {
        if (!VfxEngine.hasActiveEffects() && !BladeTrailRenderer.hasTrails()) return;

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        int width = Math.max(1, (main.width + PIXEL_SCALE - 1) / PIXEL_SCALE);
        int height = Math.max(1, (main.height + PIXEL_SCALE - 1) / PIXEL_SCALE);
        ensureTarget(width, height);

        target.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        target.clear(Minecraft.ON_OSX);
        target.copyDepthFrom(main);
        target.bindWrite(true);

        VfxEngine.render(poseStack, camera, partialTick);
        BladeTrailRenderer.render(poseStack, camera);

        main.bindWrite(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        target.blitToScreen(main.viewWidth, main.viewHeight, false);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private static void ensureTarget(int width, int height) {
        if (target == null) {
            target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            target.setFilterMode(9728); // GL_NEAREST: preserve each low-resolution texel.
        } else if (target.width != width || target.height != height) {
            target.resize(width, height, Minecraft.ON_OSX);
            target.setFilterMode(9728);
        }
    }
}
