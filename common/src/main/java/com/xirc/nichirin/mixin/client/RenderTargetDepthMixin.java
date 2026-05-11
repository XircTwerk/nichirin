package com.xirc.nichirin.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.xirc.nichirin.mixin_logic.NichirinDepthHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Exposes RenderTarget.depthBufferId through NichirinDepthHolder so shaders
 * can bind MC's existing depth texture as DiffuseDepthSampler without
 * reflection or access wideners.
 */
@Mixin(RenderTarget.class)
public class RenderTargetDepthMixin implements NichirinDepthHolder {

    @Shadow
    protected int depthBufferId;

    @Override
    public int nichirin$getDepthTexId() {
        return depthBufferId;
    }
}
