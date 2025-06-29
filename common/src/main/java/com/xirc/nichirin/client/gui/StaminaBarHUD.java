package com.xirc.nichirin.client.gui;

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

    // Configuration
    private static BarPosition position = BarPosition.ABOVE_HOTBAR_RIGHT;
    private static final int STAMINA_COLOR = 0xFFFFD700; // Gold
    private static final int BACKGROUND_COLOR = 0x80000000; // Semi-transparent black
    private static final int BORDER_COLOR = 0xFFFFFFF; // Dark gray

    // Bar dimensions
    private static final int BAR_WIDTH = 40;
    private static final int BAR_HEIGHT = 6;
    private static final int BORDER_WIDTH = 1;

    // Stamina values
    private static float currentStamina = 100f;
    private static float maxStamina = 100f;
    private static float displayedStamina = 100f;

    // Animation
    private static final float ANIMATION_SPEED = 0.15f;

    /**
     * Sets the position of the stamina bar
     */
    public static void setPosition(BarPosition newPosition) {
        position = newPosition;
    }

    /**
     * Updates the stamina values
     */
    public static void updateStamina(float current, float max) {
        if (max <= 0) max = 100f;
        current = Math.max(0, Math.min(current, max));

        currentStamina = current;
        maxStamina = max;

    }

    /**
     * Calculates the bar position based on current setting
     */
    private static int[] calculatePosition(int screenWidth, int screenHeight) {
        int x, y;

        switch (position) {
            case ABOVE_HOTBAR_CENTER:
                x = (screenWidth - BAR_WIDTH) / 2;
                y = screenHeight - 62;
                break;
            case ABOVE_HOTBAR_RIGHT:
                x = screenWidth / 2 + 91 + 15;
                y = screenHeight - 62;
                break;
            case TOP_RIGHT:
                x = screenWidth - BAR_WIDTH - 10;
                y = 25;
                break;
            case TOP_LEFT:
                x = 10;
                y = 25;
                break;
            default:
                x = (screenWidth - BAR_WIDTH) / 2;
                y = screenHeight - 62;
                break;
        }

        return new int[]{x, y};
    }

    /**
     * Renders the stamina bar
     */
    public static void render(GuiGraphics graphics, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;

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

        // Draw background with border
        graphics.fill(x - BORDER_WIDTH, y - BORDER_WIDTH,
                x + BAR_WIDTH + BORDER_WIDTH, y + BAR_HEIGHT + BORDER_WIDTH, BORDER_COLOR);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BACKGROUND_COLOR);

        // Calculate and draw stamina fill
        float fillPercentage = maxStamina > 0 ? displayedStamina / maxStamina : 0;
        fillPercentage = Mth.clamp(fillPercentage, 0f, 1f);
        int fillWidth = (int) (BAR_WIDTH * fillPercentage);

        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + BAR_HEIGHT, STAMINA_COLOR);

            // Highlight
            graphics.fill(x, y, x + fillWidth, y + 1, 0xFFFFFF80);
        }

        // Draw text label
        Component text = Component.literal("Stamina");
        int textWidth = minecraft.font.width(text);
        int textX = x + (BAR_WIDTH - textWidth) / 2;
        int textY = y - 12;
        graphics.drawString(minecraft.font, text, textX, textY, 0xFFFFFFFF, true);

        // Draw stamina numbers
        String staminaText = String.format("%.0f/%.0f", displayedStamina, maxStamina);
        int numberWidth = minecraft.font.width(staminaText);
        int numberX = x + (BAR_WIDTH - numberWidth) / 2;
        int numberY = y + BAR_HEIGHT + 3;
        graphics.drawString(minecraft.font, staminaText, numberX, numberY, 0xFFFFFFFF, true);
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
        return true;
    }
}