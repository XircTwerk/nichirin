package com.xirc.nichirin.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.client.renderer.effects.AttackHitboxRenderer;
import com.xirc.nichirin.client.shader.DeadCalmShaderEffect;
import com.xirc.nichirin.client.shader.NichirinShaderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugRenderer.class)
public class DebugRendererMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void nichirin$renderAttackHitboxes(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                               double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.getEntityRenderDispatcher().shouldRenderHitBoxes() &&
                AttackHitboxRenderer.getHitboxCount() > 0) {
            AttackHitboxRenderer.render(
                    poseStack,
                    new Vec3(cameraX, cameraY, cameraZ),
                    minecraft.levelRenderer,
                    bufferSource
            );
        }

        DeadCalmShaderEffect effect = NichirinShaderManager.getInstance()
                .getProcessor(DeadCalmShaderEffect.class);

        if (effect != null && effect.getBlockRenderer().isActive()) {
            System.out.println("DEBUG: Rendering blue blocks!");
            effect.getBlockRenderer().render(poseStack);
        }
    }
}