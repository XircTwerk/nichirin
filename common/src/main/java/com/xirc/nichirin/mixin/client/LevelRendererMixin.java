package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.client.renderer.effects.AttackHitboxRenderer;
import com.xirc.nichirin.client.shader.DeadCalmShaderEffect;
import com.xirc.nichirin.client.shader.NichirinShaderManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderLevel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDD)V",
                    shift = At.Shift.AFTER))
    private void renderAttackHitboxes(PoseStack poseStack, float partialTick, long finishNanoTime,
                                      boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
                                      LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {

        if (Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes() &&
                AttackHitboxRenderer.getHitboxCount() > 0) {

            AttackHitboxRenderer.render(
                    poseStack,
                    camera.getPosition(),
                    (LevelRenderer)(Object)this,
                    Minecraft.getInstance().renderBuffers().bufferSource()
            );
        }

        // RENDER BLUE BLOCKS HERE
        DeadCalmShaderEffect effect = NichirinShaderManager.getInstance()
                .getProcessor(DeadCalmShaderEffect.class);

        if (effect != null && effect.getBlockRenderer().isActive()) {
            System.out.println("DEBUG: Rendering blue blocks!");
            effect.getBlockRenderer().render(poseStack);
        }
    }

    /**
     * Hook into sky rendering to replace with Dead Calm skybox when active
     */
    @Inject(
            method = "renderSky",
            at = @At("HEAD"),
            cancellable = true
    )
    private void nichirin$renderDeadCalmSky(
            PoseStack poseStack,
            Matrix4f projectionMatrix,
            float partialTick,
            Camera camera,
            boolean isFoggy,
            Runnable setupFog,
            CallbackInfo ci
    ) {
        DeadCalmShaderEffect effect = NichirinShaderManager.getInstance()
                .getProcessor(DeadCalmShaderEffect.class);

        if (effect != null && effect.getSkyboxRenderer().isActive()) {
            System.out.println("DEBUG: Rendering Dead Calm skybox!");
            effect.getSkyboxRenderer().render(poseStack, projectionMatrix, partialTick);
            ci.cancel(); // Don't render normal sky
        }
    }

    /**
     * Hook after entities are rendered to copy depth buffer
     */
    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V",
                    ordinal = 0
            )
    )
    private void nichirin$copyDepthBuffer(
            PoseStack poseStack,
            float partialTick,
            long finishNanoTime,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        NichirinShaderManager.getInstance().copyDepthBuffer();
    }

    /**
     * Hook at the end of level rendering to process post-processing shaders
     */
    @Inject(
            method = "renderLevel",
            at = @At("TAIL")
    )
    private void nichirin$processShaders(
            PoseStack poseStack,
            float partialTick,
            long finishNanoTime,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        NichirinShaderManager.getInstance().processAll(poseStack);
    }
}