package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.mixin_logic.NichirinBlurAccessor;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;

public final class MistBlurOverlay {
    private static float targetIntensity;
    private static float intensity;
    private static int pulseTicks;
    private MistBlurOverlay() {
    }

    public static void register() {
        ClientGuiEvent.RENDER_HUD.register((graphics, partialTicks) -> {
            renderMist(graphics, partialTicks);
        });
    }

    private static void renderMist(GuiGraphics graphics, DeltaTracker partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || intensity <= 0.003F) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        float alpha = Math.min(0.90F, intensity);
        ((NichirinBlurAccessor) minecraft.gameRenderer).nichirin$processBlurEffect(
                partialTicks.getGameTimeDeltaTicks(),
                5.5F + alpha * 10.0F);
        graphics.fill(0, 0, width, height, argb(alpha * 0.72F, 0xA9E4FF));
    }

    public static void tick() {
        float pulse = 0.0F;
        if (pulseTicks > 0) {
            pulseTicks--;
            pulse = Math.min(1.0F, pulseTicks / 12.0F);
        }

        float wanted = Math.max(targetIntensity, pulse);
        float factor = wanted > intensity ? 0.24F : 0.11F;
        intensity += (wanted - intensity) * factor;
        if (intensity < 0.002F && wanted <= 0.0F) {
            intensity = 0.0F;
        }
    }

    public static void setTargetIntensity(float value) {
        targetIntensity = Math.max(0.0F, Math.min(1.0F, value));
    }

    public static void trigger(float strength) {
        pulseTicks = 24;
        targetIntensity = Math.max(targetIntensity, Math.max(0.35F, Math.min(1.0F, strength)));
        intensity = Math.max(intensity, Math.max(0.45F, Math.min(1.0F, strength)));
    }

    public static boolean hasVisibleIntensity() {
        return intensity > 0.002F || targetIntensity > 0.0F || pulseTicks > 0;
    }

    private static int argb(float alpha, int rgb) {
        int a = Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
