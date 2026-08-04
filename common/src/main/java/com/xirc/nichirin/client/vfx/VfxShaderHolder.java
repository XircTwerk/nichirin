package com.xirc.nichirin.client.vfx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ShaderInstance;

@Environment(EnvType.CLIENT)
public final class VfxShaderHolder {
    private static ShaderInstance shader;

    private VfxShaderHolder() {}

    public static void setShader(ShaderInstance value) { shader = value; }
    public static ShaderInstance getShader() { return shader; }

}
