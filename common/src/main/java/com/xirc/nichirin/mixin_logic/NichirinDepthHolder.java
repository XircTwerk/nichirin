package com.xirc.nichirin.mixin_logic;

/** Injected onto RenderTarget to expose the depth buffer texture ID. */
public interface NichirinDepthHolder {
    int nichirin$getDepthTexId();
}
