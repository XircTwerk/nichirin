package com.xirc.nichirin.mixin.client;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.Program;
import com.xirc.nichirin.client.shader.NichirinShaderInjection;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

@Mixin(Program.class)
public abstract class ProgramMixin {
    // require = 0: Iris rewrites vanilla shader loading — when it removes this call site the
    // redirect silently no-ops instead of crashing the game at mixin apply. The injection is
    // independently disabled for Sodium/Iris via NichirinShaderInjection.injectionEnabled().
    @Redirect(
            method = "compileShaderInternal",
            require = 0,
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/apache/commons/io/IOUtils;toString(Ljava/io/InputStream;Ljava/nio/charset/Charset;)Ljava/lang/String;",
                    remap = false))
    private static String nichirin$injectWisteriaLighting(
            InputStream inputStream,
            Charset charset,
            Program.Type type,
            String shaderName,
            InputStream shaderData,
            String sourceName,
            GlslPreprocessor processor) throws IOException {
        String source = IOUtils.toString(inputStream, charset);
        return NichirinShaderInjection.transform(type, shaderName, source);
    }
}
