package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.client.shader.MistBlurShaderEffect;
import com.xirc.nichirin.client.shader.NichirinShaderManager;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

/**
 * Activates the MistBlurShaderEffect when the local player has the Blurry mob effect.
 * Smoothly fades in/out based on remaining duration.
 */
@Environment(EnvType.CLIENT)
public class MistBlurShaderHandler {

    private static MistBlurShaderEffect shader;

    public static void register() {
        NichirinShaderManager manager = NichirinShaderManager.getInstance();

        shader = manager.getProcessor(MistBlurShaderEffect.class);
        if (shader == null) {
            shader = new MistBlurShaderEffect();
            manager.register(shader);
        }

        ClientTickEvent.CLIENT_POST.register(MistBlurShaderHandler::tick);
    }

    private static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) return;

        boolean hasEffect = minecraft.player.hasEffect(NichirinEffectRegistry.blurry());

        if (hasEffect) {
            if (!shader.isActive()) shader.setActive(true);
            // Full intensity while the effect is active
            shader.setTargetIntensity(1.0f);
        } else {
            // Fade out — shader deactivates itself once smoothed reaches 0
            shader.setTargetIntensity(0f);
            if (shader.getSmoothedIntensity() < 0.002f && shader.isActive()) {
                shader.setActive(false);
            }
        }
    }
}