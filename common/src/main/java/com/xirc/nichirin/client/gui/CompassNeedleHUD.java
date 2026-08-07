package com.xirc.nichirin.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Owner-only countdown displayed while Compass Needle's fighting-spirit tracking is active. */
@Environment(EnvType.CLIENT)
public final class CompassNeedleHUD {
    private static final int BAR_WIDTH = 112;
    private static final int BAR_HEIGHT = 6;
    private static final float MAX_SECONDS = 60.0f;

    private static long startedAtMs;
    private static long endsAtMs;
    private static long durationMs;

    private CompassNeedleHUD() {}

    public static void activate(float durationSeconds) {
        float cappedSeconds = Mth.clamp(durationSeconds, 0.1f, MAX_SECONDS);
        startedAtMs = System.currentTimeMillis();
        durationMs = Math.max(1L, Math.round(cappedSeconds * 1000.0f));
        endsAtMs = startedAtMs + durationMs;
    }

    public static void clear() {
        startedAtMs = 0L;
        endsAtMs = 0L;
        durationMs = 0L;
    }

    public static boolean isActive() {
        return endsAtMs > System.currentTimeMillis();
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || endsAtMs <= 0L) return;

        long now = System.currentTimeMillis();
        long remainingMs = endsAtMs - now;
        if (remainingMs <= 0L) {
            clear();
            return;
        }

        float progress = Mth.clamp(remainingMs / (float) durationMs, 0.0f, 1.0f);
        float remainingSeconds = remainingMs / 1000.0f;
        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int textY = minecraft.getWindow().getGuiScaledHeight() - 69;
        int barX = centerX - BAR_WIDTH / 2;
        int barY = textY + 12;

        String label = String.format("Compass Needle: %.1fs", remainingSeconds);
        graphics.drawCenteredString(minecraft.font, label, centerX, textY, 0xFFD7F8FF);

        graphics.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, 0xE0000000);
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xB0202935);
        int fillWidth = Math.max(1, Math.round(BAR_WIDTH * progress));
        int fillColor = progress < 0.20f ? 0xFFE82E52 : 0xFF5CCFEA;
        graphics.fill(barX, barY, barX + fillWidth, barY + BAR_HEIGHT, fillColor);
        graphics.fill(barX, barY, barX + fillWidth, barY + 1, 0xFFD7F8FF);

        // Twelve divisions echo the numbered compass without making the HUD visually heavy.
        for (int i = 1; i < 12; i++) {
            int markerX = barX + Math.round(BAR_WIDTH * i / 12.0f);
            graphics.fill(markerX, barY + 1, markerX + 1, barY + BAR_HEIGHT - 1, 0x70000000);
        }
    }
}
