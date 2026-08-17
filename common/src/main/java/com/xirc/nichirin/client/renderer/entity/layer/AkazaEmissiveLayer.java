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
 * Emissive glow layer for Akaza's blue lines and eyes. Rendered through the vanilla additive emissive
 * pass ({@link RenderType#eyes}) at full-bright, exactly like jcraft:eoe's stand glow layers (e.g.
 * Magician's Red / The Sun) — the masks add their colour over the model so the lines actually glow
 * instead of sitting flat. Overdrive swaps to the generated red hue-shift.
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
        // Additive emissive pass — the "jcraft glow": the mask's colour is added over the model, so the
        // lines/eyes read as light rather than a flat fullbright cutout (which had no glow at all).
        RenderType renderType = RenderType.eyes(texture);

        RenderType prevRenderType = context.renderType();
        var prevVertexConsumer = context.vertexConsumer();
        int prevPackedLight = context.packedLight();

        context.setRenderType(renderType);
        context.setPackedLight(LightTexture.FULL_BRIGHT);
        context.setVertexConsumer(context.multiBufferSource().getBuffer(renderType));

        context.rendererPipeline().reRender(context);

        context.setRenderType(prevRenderType);
        context.setVertexConsumer(prevVertexConsumer);
        context.setPackedLight(prevPackedLight);
    }
}
