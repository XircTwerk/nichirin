package com.xirc.nichirin.client.afterimage;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
final class AfterimageBufferSource implements MultiBufferSource {
    private final MultiBufferSource delegate;
    private final ResourceLocation texture;
    private final float alpha;

    AfterimageBufferSource(MultiBufferSource delegate, ResourceLocation texture, float alpha) {
        this.delegate = delegate;
        this.texture = texture;
        this.alpha = alpha;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return new AlphaVertexConsumer(delegate.getBuffer(RenderType.entityTranslucent(texture)), alpha);
    }
}
