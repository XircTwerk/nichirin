package com.xirc.nichirin.client.outline;

import com.mojang.blaze3d.shaders.Uniform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ShaderInstance;

/**
 * Holds the registered cel-outline ShaderInstance and the current OutlineWidth uniform value.
 *
 * The loader-specific module (NeoForge BreathOfNichirinNeoForgeClient) registers the shader
 * on RegisterShadersEvent and calls {@link #setShader} once loaded. The RenderType's shader
 * state shard reads it back via {@link #getShader} every frame, and uploads
 * {@link #getCurrentWidth()} into the OutlineWidth uniform before each draw.
 */
@Environment(EnvType.CLIENT)
public final class OutlineShaderHolder {
    private static ShaderInstance shader;
    private static float currentWidth = 0.03f;

    private OutlineShaderHolder() {}

    public static void setShader(ShaderInstance s) { shader = s; }
    public static ShaderInstance getShader() { return shader; }

    public static void setCurrentWidth(float w) { currentWidth = w; }
    public static float getCurrentWidth() { return currentWidth; }

    /** Pushes the current width into the shader's OutlineWidth uniform. Call before draw. */
    public static void uploadWidth() {
        if (shader == null) return;
        Uniform u = shader.getUniform("OutlineWidth");
        if (u != null) u.set(currentWidth);
    }
}
