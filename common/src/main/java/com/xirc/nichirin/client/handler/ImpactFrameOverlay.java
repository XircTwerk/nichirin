package com.xirc.nichirin.client.handler;

import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;

public final class ImpactFrameOverlay {
    private static final int DURATION_TICKS = 4;
    private static int ticksRemaining;
    private static float magnitude = 1.0F;

    private ImpactFrameOverlay() {
    }

    public static void register() {
        ClientGuiEvent.RENDER_HUD.register((graphics, partialTicks) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || ticksRemaining <= 0) {
                return;
            }

            int width = minecraft.getWindow().getGuiScaledWidth();
            int height = minecraft.getWindow().getGuiScaledHeight();
            float age = DURATION_TICKS - ticksRemaining + partialTicks.getGameTimeDeltaPartialTick(true);
            float progress = Math.max(0.0F, Math.min(1.0F, age / DURATION_TICKS));
            float strength = Math.max(0.35F, Math.min(2.0F, magnitude));

            float flashAlpha = (1.0F - progress) * (1.0F - progress) * 0.14F * strength;
            graphics.fill(0, 0, width, height, argb(flashAlpha, 0xFFFFFF));
        });
    }

    public static void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }

    public static void trigger(float strength) {
        magnitude = Math.max(0.35F, Math.min(2.0F, strength));
        ticksRemaining = DURATION_TICKS;
        ImpactCameraShake.trigger(strength);
    }

    private static int argb(float alpha, int rgb) {
        int a = Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

}
