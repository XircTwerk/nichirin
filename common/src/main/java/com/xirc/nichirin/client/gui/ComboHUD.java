package com.xirc.nichirin.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Demon Slayer style combo counter HUD with simple, clean effects
 * Features large white numbers with black outline and golden timer bar underneath
 */
public class ComboHUD {

    private static int currentCombo = 0;
    private static float totalDamage = 0.0f;
    private static long stunEndTime = 0; // When the stun effect should end
    private static long comboStartTime = 0;

    // Animation states
    private static float numberScale = 1.0f;
    private static float barFlashIntensity = 0.0f;

    // Visual constants
    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 6;

    // Colors - Demon Slayer style
    private static final int COMBO_TEXT_COLOR = 0xFFFFFFFF; // Pure white
    private static final int COMBO_OUTLINE_COLOR = 0xFF000000; // Black outline
    private static final int BAR_BACKGROUND = 0x80000000; // Semi-transparent black
    private static final int BAR_OUTLINE = 0xFF333333; // Dark outline
    private static final int BAR_ACTIVE_COLOR = 0xFFFFD700; // Golden yellow
    private static final int BAR_FLASH_COLOR = 0xFFFFFFFF; // White flash
    private static final int DAMAGE_TEXT_COLOR = 0xFFFF6B35; // Orange for damage

    /**
     * Update combo display - called when a hit lands
     * Handles both extending existing combos and starting fresh ones
     */
    public static void updateCombo(int comboCount, int stunDurationTicks) {
        long currentTime = System.currentTimeMillis();

        // Check if we have an active combo that can be extended
        boolean hasActiveCombo = currentCombo > 0 && currentTime < stunEndTime && stunEndTime > 0;

        if (hasActiveCombo) {
            // Extend existing combo - increment by 1
            currentCombo++;
            System.out.println("Combo extended! Now at: " + currentCombo + " (server sent: " + comboCount + ")");
        } else {
            // Start new combo - use server's count or default to 1
            currentCombo = Math.max(1, comboCount);
            totalDamage = 0.0f; // Reset damage on new combo start
            System.out.println("New combo started! Count: " + currentCombo + " (server sent: " + comboCount + ")");
        }

        // Always refill timer bar to full on any hit
        long durationMs = stunDurationTicks * 50; // Convert ticks to ms
        comboStartTime = currentTime;
        stunEndTime = currentTime + durationMs;

        // Trigger visual effects
        numberScale = 1.3f;
        barFlashIntensity = 1.0f;

        System.out.println("Timer refilled for " + durationMs + "ms - bar should be 100% full");
    }

    /**
     * Add damage to the combo - called for every hit including light katana attacks
     */
    public static void addDamage(float damage) {
        if (damage > 0) {
            totalDamage += damage;
            System.out.println("Damage added: " + damage + ", Total: " + totalDamage);
        }
    }

    /**
     * Alternative method if you need to update combo with a specific count
     */
    public static void setCombo(int comboCount, int stunDurationTicks) {
        long currentTime = System.currentTimeMillis();

        if (comboCount > 0) {
            currentCombo = comboCount;

            // Refill timer bar to full
            long durationMs = stunDurationTicks * 50;
            comboStartTime = currentTime;
            stunEndTime = currentTime + durationMs;

            // Trigger visual effects
            numberScale = 1.3f;
            barFlashIntensity = 1.0f;

            System.out.println("Combo set to: " + comboCount + ", Timer refilled");
        } else {
            resetCombo();
        }
    }

    /**
     * Main render method
     */
    public static void render(GuiGraphics guiGraphics) {
        // Only render if we have an active combo (combo > 0)
        if (currentCombo <= 0) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        updateAnimations();

        // Check if combo timer has expired (bar ran out)
        long currentTime = System.currentTimeMillis();
        if (currentTime >= stunEndTime && stunEndTime > 0) {
            // Timer ran out - combo ends completely
            System.out.println("Combo timer expired - combo ended");
            resetCombo();
            return; // Stop rendering when combo ends
        }

        // Position on screen - center right area
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int comboX = screenWidth - 100;
        int comboY = (screenHeight / 2) - 20;

        renderComboDisplay(guiGraphics, comboX, comboY, mc.font, currentTime);
    }

    /**
     * Update animation states
     */
    private static void updateAnimations() {
        // Scale animation for numbers
        if (numberScale > 1.0f) {
            numberScale = Math.max(1.0f, numberScale - 0.05f);
        }

        // Bar flash animation
        if (barFlashIntensity > 0.0f) {
            barFlashIntensity = Math.max(0.0f, barFlashIntensity - 0.1f);
        }
    }

    /**
     * Render the combo display - Demon Slayer style
     */
    private static void renderComboDisplay(GuiGraphics guiGraphics, int centerX, int centerY, Font font, long currentTime) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Calculate bar fill percentage (starts at 100% when hit lands, drains to 0%)
        long timeRemaining = Math.max(0, stunEndTime - currentTime);
        long originalDuration = stunEndTime - comboStartTime; // Duration set when timer was reset

        float fillPercentage;
        if (originalDuration > 0 && stunEndTime > 0) {
            fillPercentage = (float)timeRemaining / originalDuration;
        } else {
            fillPercentage = 0f;
        }
        fillPercentage = Mth.clamp(fillPercentage, 0f, 1f);

        System.out.println("Bar fill: " + (fillPercentage * 100) + "% (remaining: " + timeRemaining + "ms, total: " + originalDuration + "ms)");

        // Draw combo number with scaling
        renderComboNumber(guiGraphics, font, centerX, centerY);

        // Draw combo bar underneath the number
        renderComboBar(guiGraphics, centerX, centerY + 20, fillPercentage);

        // Always draw damage if any (lower threshold for light attacks)
        if (totalDamage > 0.001f) { // Very low threshold to catch even light katana attacks
            renderDamage(guiGraphics, font, centerX, centerY + 35);
        }

        RenderSystem.disableBlend();
    }

    /**
     * Render large combo number with simple black outline
     */
    private static void renderComboNumber(GuiGraphics guiGraphics, Font font, int centerX, int centerY) {
        String comboText = String.valueOf(currentCombo);

        guiGraphics.pose().pushPose();

        // Apply scaling animation
        float finalScale = numberScale * 2.5f; // Large scale
        guiGraphics.pose().scale(finalScale, finalScale, 1.0f);

        // Calculate scaled position
        int textWidth = font.width(comboText);
        int scaledX = (int)((centerX - textWidth / 2) / finalScale);
        int scaledY = (int)((centerY - 5) / finalScale);

        // Draw simple black outline (8 directions, single layer only)
        guiGraphics.drawString(font, comboText, scaledX - 1, scaledY - 1, COMBO_OUTLINE_COLOR);
        guiGraphics.drawString(font, comboText, scaledX, scaledY - 1, COMBO_OUTLINE_COLOR);
        guiGraphics.drawString(font, comboText, scaledX + 1, scaledY - 1, COMBO_OUTLINE_COLOR);
        guiGraphics.drawString(font, comboText, scaledX - 1, scaledY, COMBO_OUTLINE_COLOR);
        guiGraphics.drawString(font, comboText, scaledX + 1, scaledY, COMBO_OUTLINE_COLOR);
        guiGraphics.drawString(font, comboText, scaledX - 1, scaledY + 1, COMBO_OUTLINE_COLOR);
        guiGraphics.drawString(font, comboText, scaledX, scaledY + 1, COMBO_OUTLINE_COLOR);
        guiGraphics.drawString(font, comboText, scaledX + 1, scaledY + 1, COMBO_OUTLINE_COLOR);

        // Main white text
        guiGraphics.drawString(font, comboText, scaledX, scaledY, COMBO_TEXT_COLOR);

        guiGraphics.pose().popPose();
    }

    /**
     * Render horizontal combo bar with outline - combo extension timer
     * Shows time remaining to land next hit and extend combo
     */
    private static void renderComboBar(GuiGraphics guiGraphics, int centerX, int barY, float fillPercentage) {
        int barLeft = centerX - BAR_WIDTH / 2;
        int barTop = barY;
        int barRight = barLeft + BAR_WIDTH;
        int barBottom = barTop + BAR_HEIGHT;

        // Draw outline first
        guiGraphics.fill(barLeft - 1, barTop - 1, barRight + 1, barBottom + 1, BAR_OUTLINE);

        // Background bar
        guiGraphics.fill(barLeft, barTop, barRight, barBottom, BAR_BACKGROUND);

        if (fillPercentage > 0) {
            // Calculate fill width (drains from right to left as time runs out)
            int fillWidth = (int)(BAR_WIDTH * fillPercentage);

            // Active bar color - always golden yellow when active
            int barColor = BAR_ACTIVE_COLOR;

            // Add flash effect when combo extends (bar refills)
            if (barFlashIntensity > 0) {
                int flashAmount = (int)(255 * barFlashIntensity);
                barColor = blendColors(barColor, BAR_FLASH_COLOR, flashAmount);
            }

            // Draw filled portion (full width when recently hit, shrinks over time)
            guiGraphics.fill(barLeft, barTop, barLeft + fillWidth, barBottom, barColor);

            // Add subtle glow on active edge
            if (fillWidth > 2) {
                // Right edge glow (the draining edge)
                int edgeX = barLeft + fillWidth;
                guiGraphics.fill(edgeX, barTop - 1, edgeX + 1, barBottom + 1, 0x80FFD700);
            }
        }
    }

    /**
     * Render damage counter
     */
    private static void renderDamage(GuiGraphics guiGraphics, Font font, int centerX, int damageY) {
        String damageText = String.format("%.1f", totalDamage);
        int damageWidth = font.width(damageText);
        int damageX = centerX - damageWidth / 2;

        // Simple black outline for damage text
        guiGraphics.drawString(font, damageText, damageX - 1, damageY - 1, COMBO_OUTLINE_COLOR);
        guiGraphics.drawString(font, damageText, damageX, damageY - 1, COMBO_OUTLINE_COLOR);
        guiGraphics.drawString(font, damageText, damageX + 1, damageY - 1, COMBO_OUTLINE_COLOR);
        guiGraphics.drawString(font, damageText, damageX - 1, damageY, COMBO_OUTLINE_COLOR);
        guiGraphics.drawString(font, damageText, damageX + 1, damageY, COMBO_OUTLINE_COLOR);
        guiGraphics.drawString(font, damageText, damageX - 1, damageY + 1, COMBO_OUTLINE_COLOR);
        guiGraphics.drawString(font, damageText, damageX, damageY + 1, COMBO_OUTLINE_COLOR);
        guiGraphics.drawString(font, damageText, damageX + 1, damageY + 1, COMBO_OUTLINE_COLOR);

        // Main damage text
        guiGraphics.drawString(font, damageText, damageX, damageY, DAMAGE_TEXT_COLOR);
    }

    // Utility method for color blending
    private static int blendColors(int color1, int color2, int blend) {
        blend = Math.min(255, Math.max(0, blend));

        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;

        int r = r1 + (r2 - r1) * blend / 255;
        int g = g1 + (g2 - g1) * blend / 255;
        int b = b1 + (b2 - b1) * blend / 255;
        int a = a1 + (a2 - a1) * blend / 255;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // Reset and utility methods
    private static void resetCombo() {
        currentCombo = 0;
        totalDamage = 0.0f;
        stunEndTime = 0;
        comboStartTime = 0;
        numberScale = 1.0f;
        barFlashIntensity = 0.0f;
    }

    public static boolean isComboActive() {
        return currentCombo > 0 && System.currentTimeMillis() < stunEndTime;
    }

    public static int getCurrentCombo() {
        return currentCombo;
    }

    public static float getTotalDamage() {
        return totalDamage;
    }

    public static void reset() {
        resetCombo();
    }
}