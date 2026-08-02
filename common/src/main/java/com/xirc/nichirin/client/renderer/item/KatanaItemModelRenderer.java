package com.xirc.nichirin.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.client.animation.NichirinAnimations;
import com.xirc.nichirin.client.vfx.BladeTrailRenderer;
import mod.azure.azurelib.common.model.AzBone;
import mod.azure.azurelib.common.render.AzLayerRenderer;
import mod.azure.azurelib.common.render.AzRendererPipelineContext;
import mod.azure.azurelib.common.render.item.AzItemModelRenderer;
import mod.azure.azurelib.common.render.item.AzItemRendererPipeline;
import mod.azure.azurelib.common.render.item.AzItemRendererPipelineContext;
import mod.azure.azurelib.common.util.client.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.UUID;

/** Captures the transformed Blade bone before AzureLib renders its cubes. */
final class KatanaItemModelRenderer extends AzItemModelRenderer {
    KatanaItemModelRenderer(AzItemRendererPipeline pipeline, AzLayerRenderer<UUID, ItemStack> layers) {
        super(pipeline, layers);
    }

    @Override
    public void renderRecursively(AzRendererPipelineContext<UUID, ItemStack> context,
                                  AzBone bone, boolean isReRender) {
        AzItemRendererPipelineContext itemContext = (AzItemRendererPipelineContext) context;
        AbstractClientPlayer player = context.currentEntity() instanceof AbstractClientPlayer current
                ? current : HeldItemRenderContext.current();
        if (player == null && (itemContext.getTransformType() == net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || itemContext.getTransformType() == net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)) {
            player = Minecraft.getInstance().player;
        }
        if (!isReRender && "Blade".equalsIgnoreCase(bone.getName()) && player != null
                && NichirinAnimations.isKatanaAttackPlaying(player)) {
            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            RenderUtils.prepMatrixForBone(poseStack, bone);
            Matrix4f transform = poseStack.last().pose();
            Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Vec3 base = transform(transform, 0.0f, 11.0f / 16.0f, 2.0f / 16.0f).add(camera);
            Vec3 tip = transform(transform, 0.0f, 31.35f / 16.0f, 1.55f / 16.0f).add(camera);
            poseStack.popPose();
            BladeTrailRenderer.capture(player.getUUID(), itemContext.getTransformType(), base, tip);
        }
        super.renderRecursively(context, bone, isReRender);
    }

    private static Vec3 transform(Matrix4f matrix, float x, float y, float z) {
        Vector4f point = matrix.transform(new Vector4f(x, y, z, 1.0f));
        return new Vec3(point.x, point.y, point.z);
    }
}
