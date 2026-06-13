package com.xirc.nichirin.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.xirc.nichirin.client.afterimage.AfterimageRenderer;
import com.xirc.nichirin.client.aura.AuraPixelize2DRenderer;
import com.xirc.nichirin.client.handler.BloodMoonClientState;
import com.xirc.nichirin.client.light.WisteriaLightData;
import com.xirc.nichirin.client.outline.OutlineRenderer;
import com.xirc.nichirin.client.renderer.effects.CloneRingRenderer;
import com.xirc.nichirin.client.renderer.effects.MistCloneRenderer;
import com.xirc.nichirin.client.shader.DeadCalmShaderEffect;
import com.xirc.nichirin.client.shader.NichirinShaderManager;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    private static final double NICHIRIN_CROUCH_EYE_HEIGHT_DELTA = 0.35D;

    /**
     * Tints the moon disc deep red during a blood moon.
     *
     * In vanilla renderSky the second setShaderTexture call (ordinal = 1) binds the moon
     * phase atlas.  We intercept it, perform the bind as normal, then immediately set the
     * shader colour to blood-red so the moon quad renders with that tint.
     */
    @Redirect(
            method = "renderSky",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V",
                    ordinal = 1)
    )
    private void nichirin$bloodMoonMoonTexture(int texUnit, ResourceLocation texture) {
        RenderSystem.setShaderTexture(texUnit, texture);
        if (BloodMoonClientState.isActive()) {
            // Deep blood-red tint applied right before the moon quad is drawn
            RenderSystem.setShaderColor(1.0F, 0.08F, 0.08F, 1.0F);
        }
    }

    /**
     * Reset shader colour after the sky pass so nothing rendered afterward is tinted.
     */
    @Inject(method = "renderSky", at = @At("TAIL"))
    private void nichirin$afterSkyRender(
            Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float partialTick,
            Camera camera, boolean isFoggy, Runnable setupFog, CallbackInfo ci) {
        if (BloodMoonClientState.isActive()) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    /**
     * Hook into sky rendering to replace with Dead Calm skybox when active.
     */
    @Inject(
            method = "renderSky",
            at = @At("HEAD"),
            cancellable = true
    )
    private void nichirin$renderDeadCalmSky(
            Matrix4f modelViewMatrix,
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
            PoseStack poseStack = new PoseStack();
            poseStack.mulPose(modelViewMatrix);
            effect.getSkyboxRenderer().render(poseStack, projectionMatrix, partialTick);
            ci.cancel(); // Don't render normal sky
        }
    }

    /**
     * Hook at the end of level rendering to process post-processing shaders.
     * At this point all geometry has rendered: sky pixels have depth 1.0,
     * world pixels have depth < 1.0.  NichirinShaderManager will snapshot
     * the depth before running any shader passes.
     */
    @Inject(
            method = "renderLevel",
            at = @At("TAIL")
    )
    private void nichirin$processShaders(
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        NichirinShaderManager.getInstance().setFrameContext(camera, frustumMatrix);
        NichirinShaderManager.getInstance().processAll(new PoseStack());

        // Aura system: render 2D pixelated disc at any entity with auras.
        try {
            float partial = deltaTracker.getGameTimeDeltaPartialTick(true);
            PoseStack auraStack = new PoseStack();
            auraStack.mulPose(frustumMatrix);
            AuraPixelize2DRenderer.renderAll(auraStack, camera, partial);
            AfterimageRenderer.render(auraStack, camera, partial);
            MistCloneRenderer.render(auraStack, camera, partial);
            CloneRingRenderer.render(auraStack, camera, partial);

            // Outline system, two paths:
            //  - seeThroughWalls=true  → MC's built-in outline framebuffer + edge-detection
            //    post-shader via EntityOutlineMixin (screen-space, ignores depth by design).
            //  - seeThroughWalls=false → OutlineRenderer's depth-tested cel-shader geometry
            //    pass, so the outline is occluded per-pixel where walls cover the entity.
            OutlineRenderer.renderAll(auraStack, camera, partial);
        } catch (Exception ignored) {
            // Never let aura/outline rendering crash the level render.
        }
    }

    @Inject(
            method = "renderEntity",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    )
    private void nichirin$adaptFirstPersonAnimationToCrouch(
            Entity entity,
            double cameraX,
            double cameraY,
            double cameraZ,
            float tickDelta,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            CallbackInfo ci
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (FirstPersonMode.isFirstPersonPass()
                && entity == minecraft.cameraEntity
                && entity.isCrouching()
                && !(entity instanceof LivingEntity living
                && living.hasEffect(NichirinEffectRegistry.blocking()))) {
            poseStack.translate(0.0D, -NICHIRIN_CROUCH_EYE_HEIGHT_DELTA, 0.0D);
        }
    }

    @ModifyArgs(
            method = "renderEntity",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    )
    private void nichirin$applyWisteriaLightToEntities(Args args) {
        Entity entity = args.get(0);
        int packedLight = args.get(8);
        double sampleY = entity.getY() + entity.getBbHeight() * 0.5D;
        args.set(8, WisteriaLightData.applyEntityLight(new Vec3(entity.getX(), sampleY, entity.getZ()), packedLight));
    }
}
