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
 * Composites the mod's code-driven VFX onto the main framebuffer in two sub-passes:
 *
 * <ul>
 *   <li><b>Effects</b> render to a low-resolution target and upscale with nearest filtering — the
 *       chunky "pixel-art" look. Combined with the world-locked vertex snap this stays stable and
 *       roughly world-space; the block size is set by {@link #EFFECT_PIXEL_SCALE}.</li>
 *   <li><b>Ribbon trails</b> (blade + arm) render to a full-resolution target so they stay smooth —
 *       pixelating them would make them rough and stair-stepped, which is the opposite of what a
 *       flowing trail wants.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public final class VfxPixelRenderPass {
    // Screen-space chunkiness of the effect layer only. Higher = blockier (and lower-res when far).
    private static final int EFFECT_PIXEL_SCALE = 6;
    private static TextureTarget effectTarget;
    private static TextureTarget trailTarget;

    private VfxPixelRenderPass() {}

    public static void render(PoseStack poseStack, Camera camera, float partialTick) {
        boolean hasEffects = VfxEngine.hasActiveEffects();
        boolean hasTrails = BladeTrailRenderer.hasTrails() || ArmTrailRenderer.hasTrails();
        if (!hasEffects && !hasTrails) return;

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        // Lock the pixel grid to the world so effects don't shimmer as the camera moves.
        var camPos = camera.getPosition();
        VfxPixelRender.setWorldOrigin(camPos.x, camPos.y, camPos.z);

        if (hasEffects) {
            int width = Math.max(1, (main.width + EFFECT_PIXEL_SCALE - 1) / EFFECT_PIXEL_SCALE);
            int height = Math.max(1, (main.height + EFFECT_PIXEL_SCALE - 1) / EFFECT_PIXEL_SCALE);
            effectTarget = ensureTarget(effectTarget, width, height);
            renderInto(effectTarget, main, () -> VfxEngine.render(poseStack, camera, partialTick));
            blit(effectTarget, main);
        }

        if (hasTrails) {
            trailTarget = ensureTarget(trailTarget, main.width, main.height);
            renderInto(trailTarget, main, () -> {
                BladeTrailRenderer.render(poseStack, camera);
                ArmTrailRenderer.render(poseStack, camera);
            });
            blit(trailTarget, main);
        }

        main.bindWrite(true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private static void renderInto(TextureTarget target, RenderTarget main, Runnable draw) {
        target.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        target.clear(Minecraft.ON_OSX);
        target.copyDepthFrom(main);
        target.bindWrite(true);
        draw.run();
    }

    private static void blit(TextureTarget target, RenderTarget main) {
        main.bindWrite(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        target.blitToScreen(main.viewWidth, main.viewHeight, false);
        RenderSystem.disableBlend();
    }

    private static TextureTarget ensureTarget(TextureTarget target, int width, int height) {
        if (target == null) {
            target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            target.setFilterMode(9728); // GL_NEAREST: preserve each texel when scaling.
        } else if (target.width != width || target.height != height) {
            target.resize(width, height, Minecraft.ON_OSX);
            target.setFilterMode(9728);
        }
        return target;
    }
}
