package com.xirc.nichirin.client.gui;

import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public class StanceBarHUD {

    // Configuration - simplified since we only need one position
    private static final int STANCE_COLOR = 0xFFFF8C00; // Orange color (DarkOrange)
    private static final int BACKGROUND_COLOR = 0x80000000; // Semi-transparent black
    private static final int BORDER_COLOR = 0xFF000000; // Full black for outline

    // Bar dimensions (same size as stamina bar)
    private static final int BAR_WIDTH = 40; // Same as stamina
    private static final int BAR_HEIGHT = 6; // Same as stamina
    private static final int BORDER_WIDTH = 1;

    // Stance values
    private static float currentStance = 80f; // Default max is 80
    private static float maxStance = 80f;
    private static float displayedStance = 80f;
    private static float lastStance = 80f;

    // Animation (slightly slower than stamina)
    private static final float ANIMATION_SPEED = 0.12f;

    // Auto-hide functionality
    private static final long HIDE_DELAY_MS = 3000;
    private static long lastChangeTime = System.currentTimeMillis();
    @Getter
    private static float fadeAlpha = 1.0f;
    private static final float FADE_SPEED = 0.05f;

    /**
     * Updates the stance values
     */
    public static void updateStance(float current, float max) {
        if (max <= 0) max = 80f;
        current = Math.max(0, Math.min(current, max));

        // Check if values changed
        if (current != currentStance || max != maxStance) {
            lastChangeTime = System.currentTimeMillis();
            fadeAlpha = 1.0f; // Reset fade when values change
        }

        currentStance = current;
        maxStance = max;
    }

    /**
     * Calculates the bar position based on current setting
     */
    private static int[] calculatePosition(int screenWidth, int screenHeight) {
        // Just take the stamina bar position and slide it right
        int x = screenWidth / 2 + 91 + 15 + 60;
        int y = screenHeight - 62; // Same as stamina bar

        return new int[]{x, y};
    }

    /**
     * Renders the stance bar
     */
    public static void render(GuiGraphics graphics, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;

        // Update fade animation
        long currentTime = System.currentTimeMillis();
        long timeSinceChange = currentTime - lastChangeTime;

        if (timeSinceChange > HIDE_DELAY_MS) {
            // Start fading out
            fadeAlpha = Math.max(0, fadeAlpha - FADE_SPEED);
        } else {
            // Ensure full visibility during delay period
            fadeAlpha = 1.0f;
        }

        // Don't render if completely faded out
        if (fadeAlpha <= 0) return;

        // Update animation
        float difference = currentStance - displayedStance;
        if (Math.abs(difference) > 0.1f) {
            displayedStance += difference * ANIMATION_SPEED;
        } else {
            displayedStance = currentStance;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int[] pos = calculatePosition(screenWidth, screenHeight);
        int x = pos[0];
        int y = pos[1];

        // Apply alpha to colors
        int alpha = (int)(fadeAlpha * 255) << 24;
        int borderColor = (alpha) | (BORDER_COLOR & 0x00FFFFFF);
        int backgroundColor = (int)(fadeAlpha * 0x80) << 24; // Semi-transparent background
        int stanceColor = alpha | (STANCE_COLOR & 0x00FFFFFF);
        int highlightColor = alpha | 0x00FFAA55; // Orange highlight
        int textColor = alpha | 0x00FFFFFF;

        // Draw background with border
        graphics.fill(x - BORDER_WIDTH, y - BORDER_WIDTH,
                x + BAR_WIDTH + BORDER_WIDTH, y + BAR_HEIGHT + BORDER_WIDTH, borderColor);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, backgroundColor);

        // Calculate and draw stance fill
        float fillPercentage = maxStance > 0 ? displayedStance / maxStance : 0;
        fillPercentage = Mth.clamp(fillPercentage, 0f, 1f);
        int fillWidth = (int) (BAR_WIDTH * fillPercentage);

        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + BAR_HEIGHT, stanceColor);

            // Highlight
            graphics.fill(x, y, x + fillWidth, y + 1, highlightColor);
        }

        // Draw text label with fade
        Component text = Component.literal("Stance");
        int textWidth = minecraft.font.width(text);
        int textX = x + (BAR_WIDTH - textWidth) / 2;
        int textY = y - 12;
        graphics.drawString(minecraft.font, text, textX, textY, textColor, true);

        // Draw stance numbers with fade
        String stanceText = String.format("%.0f/%.0f", displayedStance, maxStance);
        int numberWidth = minecraft.font.width(stanceText);
        int numberX = x + (BAR_WIDTH - numberWidth) / 2;
        int numberY = y + BAR_HEIGHT + 3;
        graphics.drawString(minecraft.font, stanceText, numberX, numberY, textColor, true);
    }

    /**
     * Gets the current stance percentage (0-1)
     */
    public static float getStancePercentage() {
        return maxStance > 0 ? displayedStance / maxStance : 0f;
    }

    /**
     * Checks if stance bar should be visible
     */
    public static boolean shouldRender() {
        return fadeAlpha > 0;
    }

    /**
     * Forces the stance bar to show immediately
     */
    public static void forceShow() {
        lastChangeTime = System.currentTimeMillis();
        fadeAlpha = 1.0f;
    }
}