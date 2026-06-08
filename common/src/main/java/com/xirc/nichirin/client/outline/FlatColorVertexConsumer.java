package com.xirc.nichirin.client.outline;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Wraps a {@link VertexConsumer} and rewrites every colour call to a fixed RGBA. The model's
 * vertex texture and lighting are passed through unchanged — only colour is overridden.
 *
 * Used to convert a regular entity model render into a flat-coloured silhouette for the
 * outline effect.
 */
@Environment(EnvType.CLIENT)
public final class FlatColorVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float fr, fg, fb, fa;

    public FlatColorVertexConsumer(VertexConsumer delegate, float r, float g, float b, float a) {
        this.delegate = delegate;
        this.fr = r; this.fg = g; this.fb = b; this.fa = a;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        delegate.setColor(fr, fg, fb, fa);
        return this;
    }

    @Override
    public VertexConsumer setColor(float r, float g, float b, float a) {
        delegate.setColor(fr, fg, fb, fa);
        return this;
    }

    @Override
    public VertexConsumer setColor(int packed) {
        delegate.setColor(fr, fg, fb, fa);
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
