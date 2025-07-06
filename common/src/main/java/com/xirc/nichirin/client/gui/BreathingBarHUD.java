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
public class BreathingBarHUD {

    // Position configuration (uses same positions as stamina bar)
    public enum BarPosition {
        ABOVE_HOTBAR_CENTER,
        ABOVE_HOTBAR_LEFT,  // Changed to LEFT for breathing bar
        TOP_RIGHT,
        TOP_LEFT
    }

    /**
     * -- SETTER --
     *  Sets the position of the breathing bar
     */
    // Configuration
    @Setter
    private static BarPosition position = BarPosition.ABOVE_HOTBAR_LEFT;  // Default to left side
    private static final int BREATHING_COLOR = 0xFF55AAFF; // Light blue
    private static final int BACKGROUND_COLOR = 0x80000000; // Semi-transparent black
    private static final int BORDER_COLOR = 0xFF000000; // Full black for outline

    // Bar dimensions (same as stamina)
    private static final int BAR_WIDTH = 40;
    private static final int BAR_HEIGHT = 6;
    private static final int BORDER_WIDTH = 1;

    // Breathing values
    private static float currentBreath = 100f;
    private static float maxBreath = 100f;
    private static float displayedBreath = 100f;
    private static float lastBreath = 100f;

    // Animation
    private static final float ANIMATION_SPEED = 0.15f;

    // Auto-hide functionality
    private static final long HIDE_DELAY_MS = 3000; // 3 seconds
    private static long lastChangeTime = System.currentTimeMillis();
    /**
     * -- GETTER --
     *  Gets the current fade alpha value (0-1)
     */
    @Getter
    private static float fadeAlpha = 1.0f;
    private static final float FADE_SPEED = 0.05f;

    /**
     * Updates the breathing values
     */
    public static void updateBreath(float current, float max) {
        if (max <= 0) max = 100f;
        current = Math.max(0, Math.min(current, max));

        // Check if values changed
        if (current != currentBreath || max != maxBreath) {
            lastChangeTime = System.currentTimeMillis();
            fadeAlpha = 1.0f; // Reset fade when values change
        }

        currentBreath = current;
        maxBreath = max;
    }

    /**
     * Calculates the bar position based on current setting
     */
    private static int[] calculatePosition(int screenWidth, int screenHeight) {
        int x, y;

        y = switch (position) {
            case ABOVE_HOTBAR_CENTER -> {
                x = (screenWidth - BAR_WIDTH) / 2;
                yield screenHeight - 62;
            }
            case ABOVE_HOTBAR_LEFT -> {
                x = screenWidth / 2 - 91 - 15 - BAR_WIDTH;  // Mirror of stamina bar position
                yield screenHeight - 62;
            }
            case TOP_RIGHT -> {
                x = screenWidth - BAR_WIDTH - 10;
                yield 40;  // Slightly below stamina if both at top
            }
            case TOP_LEFT -> {
                x = 10;
                yield 40;  // Slightly below stamina if both at top
            }
            default -> {
                x = (screenWidth - BAR_WIDTH) / 2;
                yield screenHeight - 62;
            }
        };

        return new int[]{x, y};
    }

    /**
     * Renders the breathing bar
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
        float difference = currentBreath - displayedBreath;
        if (Math.abs(difference) > 0.1f) {
            displayedBreath += difference * ANIMATION_SPEED;
        } else {
            displayedBreath = currentBreath;
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
        int breathingColor = alpha | (BREATHING_COLOR & 0x00FFFFFF);
        int highlightColor = alpha | 0x0088DDFF;  // Lighter blue highlight
        int textColor = alpha | 0x00FFFFFF;

        // Draw background with border
        graphics.fill(x - BORDER_WIDTH, y - BORDER_WIDTH,
                x + BAR_WIDTH + BORDER_WIDTH, y + BAR_HEIGHT + BORDER_WIDTH, borderColor);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, backgroundColor);

        // Calculate and draw breathing fill
        float fillPercentage = maxBreath > 0 ? displayedBreath / maxBreath : 0;
        fillPercentage = Mth.clamp(fillPercentage, 0f, 1f);
        int fillWidth = (int) (BAR_WIDTH * fillPercentage);

        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + BAR_HEIGHT, breathingColor);

            // Highlight
            graphics.fill(x, y, x + fillWidth, y + 1, highlightColor);
        }

        // Draw text label with fade
        Component text = Component.literal("Breath");
        int textWidth = minecraft.font.width(text);
        int textX = x + (BAR_WIDTH - textWidth) / 2;
        int textY = y - 12;
        graphics.drawString(minecraft.font, text, textX, textY, textColor, true);

        // Draw breathing numbers with fade
        String breathText = String.format("%.0f/%.0f", displayedBreath, maxBreath);
        int numberWidth = minecraft.font.width(breathText);
        int numberX = x + (BAR_WIDTH - numberWidth) / 2;
        int numberY = y + BAR_HEIGHT + 3;
        graphics.drawString(minecraft.font, breathText, numberX, numberY, textColor, true);
    }

    /**
     * Gets the current breathing percentage (0-1)
     */
    public static float getBreathingPercentage() {
        return maxBreath > 0 ? displayedBreath / maxBreath : 0f;
    }

    /**
     * Checks if breathing bar should be visible
     */
    public static boolean shouldRender() {
        return fadeAlpha > 0;
    }

    /**
     * Forces the breathing bar to show immediately
     */
    public static void forceShow() {
        lastChangeTime = System.currentTimeMillis();
        fadeAlpha = 1.0f;
    }
}