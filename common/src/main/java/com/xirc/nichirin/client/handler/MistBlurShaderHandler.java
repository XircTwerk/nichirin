package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.registry.NichirinEffectRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class MistBlurShaderHandler {

    public static void register() {
        MistBlurOverlay.register();
        ClientTickEvent.CLIENT_POST.register(MistBlurShaderHandler::tick);
    }

    private static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        boolean hasEffect = minecraft.player.hasEffect(NichirinEffectRegistry.blurry());
        float intensity = hasEffect ? 0.35F : 0.0F;
        MistBlurOverlay.setTargetIntensity(intensity);
        MistBlurOverlay.tick();
    }
}
