package com.xirc.nichirin.client.afterimage;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class AlphaVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float alpha;

    AlphaVertexConsumer(VertexConsumer delegate, float alpha) {
        this.delegate = delegate;
        this.alpha = alpha;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        delegate.setColor(r, g, b, Math.round(alpha * 255.0F));
        return this;
    }

    @Override
    public VertexConsumer setColor(float r, float g, float b, float a) {
        delegate.setColor(r, g, b, alpha);
        return this;
    }

    @Override
    public VertexConsumer setColor(int packed) {
        int r = packed >> 16 & 0xFF;
        int g = packed >> 8 & 0xFF;
        int b = packed & 0xFF;
        delegate.setColor(r, g, b, Math.round(alpha * 255.0F));
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        delegate.setNormal(x, y, z);
        return this;
    }
}
