package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.client.light.WisteriaLightData;
import com.xirc.nichirin.client.shader.NichirinShaderInjection;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderInstance.class)
public abstract class ShaderInstanceMixin {
    @Inject(method = "apply", at = @At("TAIL"))
    private void nichirin$uploadWisteriaLightUniforms(CallbackInfo ci) {
        ShaderInstance shader = (ShaderInstance) (Object) this;
        if (NichirinShaderInjection.isWisteriaChunkShader(shader.getName())) {
            WisteriaLightData.uploadToShader(shader.getId());
        }
    }
}
