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
public class StaminaBarHUD {

    // Position configuration
    public enum BarPosition {
        ABOVE_HOTBAR_CENTER,
        ABOVE_HOTBAR_RIGHT,
        TOP_RIGHT,
        TOP_LEFT
    }

    /**
     * -- SETTER --
     *  Sets the position of the stamina bar
     */
    // Configuration
    @Setter
    private static BarPosition position = BarPosition.ABOVE_HOTBAR_RIGHT;
    private static final int STAMINA_COLOR = 0xFFFFD700; // Gold
    private static final int BACKGROUND_COLOR = 0x80000000; // Semi-transparent black
    private static final int BORDER_COLOR = 0xFF000000; // Full black for outline

    // Bar dimensions
    private static final int BAR_WIDTH = 40;
    private static final int BAR_HEIGHT = 6;
    private static final int BORDER_WIDTH = 1;

    // Stamina values
    private static float currentStamina = 100f;
    private static float maxStamina = 100f;
    private static float displayedStamina = 100f;
    private static float lastStamina = 100f;

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
     * Updates the stamina values
     */
    public static void updateStamina(float current, float max) {
        if (max <= 0) max = 100f;
        current = Math.max(0, Math.min(current, max));

        // Check if values changed
        if (current != currentStamina || max != maxStamina) {
            lastChangeTime = System.currentTimeMillis();
            fadeAlpha = 1.0f; // Reset fade when values change
        }

        currentStamina = current;
        maxStamina = max;
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
            case ABOVE_HOTBAR_RIGHT -> {
                x = screenWidth / 2 + 91 + 15;
                yield screenHeight - 62;
            }
            case TOP_RIGHT -> {
                x = screenWidth - BAR_WIDTH - 10;
                yield 25;
            }
            case TOP_LEFT -> {
                x = 10;
                yield 25;
            }
            default -> {
                x = (screenWidth - BAR_WIDTH) / 2;
                yield screenHeight - 62;
            }
        };

        return new int[]{x, y};
    }

    /**
     * Renders the stamina bar
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
        float difference = currentStamina - displayedStamina;
        if (Math.abs(difference) > 0.1f) {
            displayedStamina += difference * ANIMATION_SPEED;
        } else {
            displayedStamina = currentStamina;
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
        int staminaColor = alpha | (STAMINA_COLOR & 0x00FFFFFF);
        int highlightColor = alpha | 0x00FFFF80;
        int textColor = alpha | 0x00FFFFFF;

        // Draw background with border
        graphics.fill(x - BORDER_WIDTH, y - BORDER_WIDTH,
                x + BAR_WIDTH + BORDER_WIDTH, y + BAR_HEIGHT + BORDER_WIDTH, borderColor);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, backgroundColor);

        // Calculate and draw stamina fill
        float fillPercentage = maxStamina > 0 ? displayedStamina / maxStamina : 0;
        fillPercentage = Mth.clamp(fillPercentage, 0f, 1f);
        int fillWidth = (int) (BAR_WIDTH * fillPercentage);

        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + BAR_HEIGHT, staminaColor);

            // Highlight
            graphics.fill(x, y, x + fillWidth, y + 1, highlightColor);
        }

        // Draw text label with fade
        Component text = Component.literal("Stamina");
        int textWidth = minecraft.font.width(text);
        int textX = x + (BAR_WIDTH - textWidth) / 2;
        int textY = y - 12;
        graphics.drawString(minecraft.font, text, textX, textY, textColor, true);

        // Draw stamina numbers with fade
        String staminaText = String.format("%.0f/%.0f", displayedStamina, maxStamina);
        int numberWidth = minecraft.font.width(staminaText);
        int numberX = x + (BAR_WIDTH - numberWidth) / 2;
        int numberY = y + BAR_HEIGHT + 3;
        graphics.drawString(minecraft.font, staminaText, numberX, numberY, textColor, true);
    }

    /**
     * Gets the current stamina percentage (0-1)
     */
    public static float getStaminaPercentage() {
        return maxStamina > 0 ? displayedStamina / maxStamina : 0f;
    }

    /**
     * Checks if stamina bar should be visible
     */
    public static boolean shouldRender() {
        return fadeAlpha > 0;
    }

    /**
     * Forces the stamina bar to show immediately
     */
    public static void forceShow() {
        lastChangeTime = System.currentTimeMillis();
        fadeAlpha = 1.0f;
    }

}