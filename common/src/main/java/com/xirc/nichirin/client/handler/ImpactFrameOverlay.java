package com.xirc.nichirin.client.handler;

import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;

public final class ImpactFrameOverlay {
    private static final int DURATION_TICKS = 4;
    private static int ticksRemaining;
    /** Final flash strength used by the render — already clamped by the trigger that set it. */
    private static float flashStrength = 1.0F;

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

            float flashAlpha = (1.0F - progress) * (1.0F - progress) * 0.14F * flashStrength;
            graphics.fill(0, 0, width, height, argb(flashAlpha, 0xFFFFFF));
        });
    }

    public static void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }

    /** Legacy entry point: flash floors at 0.35 and the camera shake floors at 1.0. */
    public static void trigger(float strength) {
        flashStrength = Math.max(0.35F, Math.min(2.0F, strength));
        ticksRemaining = DURATION_TICKS;
        ImpactCameraShake.trigger(strength);
    }

    /**
     * Decoupled entry point: lets a caller (e.g. the gun) request a much fainter flash and a gentler
     * shake than the legacy floors allow, without affecting other attacks that use {@link #trigger(float)}.
     */
    public static void trigger(float flash, float shake) {
        flashStrength = Math.max(0.0F, Math.min(2.0F, flash));
        ticksRemaining = DURATION_TICKS;
        ImpactCameraShake.trigger(shake, 0.0F);
    }

    private static int argb(float alpha, int rgb) {
        int a = Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

}
