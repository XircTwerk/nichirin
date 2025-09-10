package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.client.renderer.effects.AttackHitboxRenderer;
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
    }
}