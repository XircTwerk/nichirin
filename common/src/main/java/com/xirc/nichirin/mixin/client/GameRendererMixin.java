package com.xirc.nichirin.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.client.handler.ImpactCameraShake;
import com.xirc.nichirin.client.handler.SlammedWobble;
import com.xirc.nichirin.mixin_logic.NichirinBlurAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin implements NichirinBlurAccessor {
    @Shadow
    private PostChain blurEffect;

    @Inject(method = "bobHurt", at = @At("TAIL"))
    private void nichirin$impactCameraShakeAfterHurtBob(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        ImpactCameraShake.apply(poseStack, partialTick);
        SlammedWobble.apply(poseStack, partialTick);
    }

    @Inject(method = "bobView", at = @At("TAIL"))
    private void nichirin$impactCameraShakeAfterViewBob(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        ImpactCameraShake.apply(poseStack, partialTick);
    }

    @Override
    public void nichirin$processBlurEffect(float partialTick, float radius) {
        if (blurEffect == null || radius < 1.0F) {
            return;
        }

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        blurEffect.setUniform("Radius", radius);
        blurEffect.process(partialTick);
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        RenderSystem.enableDepthTest();
    }
}
