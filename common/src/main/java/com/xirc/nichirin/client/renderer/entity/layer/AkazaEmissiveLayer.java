package com.xirc.nichirin.client.renderer.entity.layer;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.entity.npc.AkazaEntity;
import mod.azure.azurelib.common.model.AzBone;
import mod.azure.azurelib.common.render.AzRendererPipelineContext;
import mod.azure.azurelib.common.render.layer.AzRenderLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Emissive glow layer for Akaza's blue lines and eyes. Re-renders the model with each emissive
 * texture at full brightness ({@link RenderType#eyes}); when Akaza is in Overdrive it swaps to the
 * code-generated red hue-shift ({@link AkazaEmissiveTextures}).
 */
@Environment(EnvType.CLIENT)
public class AkazaEmissiveLayer implements AzRenderLayer<UUID, AkazaEntity> {

    private static final ResourceLocation LINES = ResourceLocation.fromNamespaceAndPath(
            BreathOfNichirin.MOD_ID, "textures/entity/npc/akaza/akaza_lines.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(
            BreathOfNichirin.MOD_ID, "textures/entity/npc/akaza/akaza_eyes.png");

    @Override
    public void preRender(AzRendererPipelineContext<UUID, AkazaEntity> context) {}

    @Override
    public void renderForBone(AzRendererPipelineContext<UUID, AkazaEntity> context, AzBone bone) {}

    @Override
    public void render(AzRendererPipelineContext<UUID, AkazaEntity> context) {
        AkazaEntity akaza = context.animatable();
        boolean overdrive = akaza != null && akaza.isOverdrive();

        renderEmissive(context, AkazaEmissiveTextures.processed(LINES, overdrive));
        renderEmissive(context, AkazaEmissiveTextures.processed(EYES, overdrive));
    }

    private void renderEmissive(AzRendererPipelineContext<UUID, AkazaEntity> context, ResourceLocation texture) {
        RenderType renderType = RenderType.eyes(texture);

        RenderType prevRenderType = context.renderType();
        var prevVertexConsumer = context.vertexConsumer();
        int prevPackedLight = context.packedLight();

        context.setRenderType(renderType);
        context.setPackedLight(LightTexture.FULL_SKY);
        context.setVertexConsumer(context.multiBufferSource().getBuffer(renderType));

        context.rendererPipeline().reRender(context);

        context.setRenderType(prevRenderType);
        context.setVertexConsumer(prevVertexConsumer);
        context.setPackedLight(prevPackedLight);
    }
}
