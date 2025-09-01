package com.xirc.nichirin.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Simplified Demon Slayer style combo counter HUD
 * Server is the authority on combo logic, client just displays with smooth visuals
 */
public class ComboHUD {

    private static int currentCombo = 0;
    private static int lastValidCombo = 0;
    private static float totalDamage = 0.0f;
    private static long stunEndTime = 0;
    private static long comboStartTime = 0;

    // Animation states
    private static float numberScale = 1.0f;
    private static float barFlashIntensity = 0.0f;
    private static float fadeOutAlpha = 1.0f;
    private static boolean shouldFadeOut = false;

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
     * Update combo display - called when server sends new combo data
     * Simple logic: trust the server completely
     */
    public static void updateCombo(int comboCount, int stunDurationTicks) {
        long currentTime = System.currentTimeMillis();

        // If server sends combo count 1, it's always a fresh start
        if (comboCount == 1) {
            System.out.println("New combo! Server sent: " + comboCount);
            currentCombo = comboCount;
            lastValidCombo = currentCombo;
            shouldFadeOut = false;
            fadeOutAlpha = 1.0f;
            totalDamage = 0.0f; // Reset damage on fresh start
        } else if (comboCount > currentCombo) {
            // Server says combo is progressing - keep accumulating damage
            System.out.println("Combo progressing! " + currentCombo + " -> " + comboCount);
            currentCombo = comboCount;
            lastValidCombo = currentCombo;
            shouldFadeOut = false;
            fadeOutAlpha = 1.0f;
            // Don't reset totalDamage - let it accumulate
        } else {
            // Same count or other cases - just update
            System.out.println("Combo update! Count: " + comboCount);
            currentCombo = comboCount;
            lastValidCombo = currentCombo;
            shouldFadeOut = false;
            fadeOutAlpha = 1.0f;
            // Don't reset totalDamage
        }

        // Always refill timer bar to full on any hit
        long durationMs = stunDurationTicks * 50; // Convert ticks to ms
        comboStartTime = currentTime;
        stunEndTime = currentTime + durationMs;

        // Trigger visual effects
        numberScale = 1.3f;
        barFlashIntensity = 1.0f;
    }

    /**
     * Add damage to the combo - called for every hit
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
            lastValidCombo = currentCombo;
            shouldFadeOut = false;
            fadeOutAlpha = 1.0f;

            // Refill timer bar to full
            long durationMs = stunDurationTicks * 50;
            comboStartTime = currentTime;
            stunEndTime = currentTime + durationMs;

            // Trigger visual effects
            numberScale = 1.3f;
            barFlashIntensity = 1.0f;
        } else {
            resetCombo();
        }
    }

    /**
     * Main render method
     */
    public static void render(GuiGraphics guiGraphics) {
        // Skip rendering if no combo and not fading out
        if (currentCombo <= 0 && !shouldFadeOut) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        updateAnimations();

        // Calculate timer bar fill percentage
        long currentTime = System.currentTimeMillis();
        long timeRemaining = Math.max(0, stunEndTime - currentTime);
        long originalDuration = stunEndTime - comboStartTime;

        float fillPercentage = 0f;
        if (originalDuration > 0 && stunEndTime > 0) {
            fillPercentage = (float)timeRemaining / originalDuration;
        }
        fillPercentage = Mth.clamp(fillPercentage, 0f, 1f);

        // If bar is completely empty and we have a combo, start fade-out
        if (fillPercentage <= 0f && stunEndTime > 0 && currentCombo > 0 && !shouldFadeOut) {
            shouldFadeOut = true;
        }

        // If we're fading out and fade is complete, stop rendering entirely
        if (shouldFadeOut && fadeOutAlpha <= 0f) {
            resetCombo();
            return;
        }

        // Position on screen - center right area
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int comboX = screenWidth - 100;
        int comboY = (screenHeight / 2) - 20;

        renderComboDisplay(guiGraphics, comboX, comboY, mc.font, fillPercentage);
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

        // Fade-out animation
        if (shouldFadeOut) {
            fadeOutAlpha = Math.max(0.0f, fadeOutAlpha - 0.08f);
        }
    }

    /**
     * Render the combo display
     */
    private static void renderComboDisplay(GuiGraphics guiGraphics, int centerX, int centerY, Font font, float fillPercentage) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Draw combo number
        if (currentCombo > 0 || shouldFadeOut) {
            int displayCombo = shouldFadeOut ? lastValidCombo : currentCombo;
            renderComboNumber(guiGraphics, font, centerX, centerY, displayCombo);
        }

        // Draw combo bar underneath the number
        if (!shouldFadeOut && fillPercentage > 0) {
            renderComboBar(guiGraphics, centerX, centerY + 20, fillPercentage);
        }

        // Draw damage if any
        if (totalDamage > 0.001f) {
            renderDamage(guiGraphics, font, centerX, centerY + 35);
        }

        RenderSystem.disableBlend();
    }

    /**
     * Render large combo number with scaling and fade support
     */
    private static void renderComboNumber(GuiGraphics guiGraphics, Font font, int centerX, int centerY, int comboToShow) {
        String comboText = String.valueOf(comboToShow);

        guiGraphics.pose().pushPose();

        // Apply scaling animation
        float finalScale = numberScale * 2.5f;
        guiGraphics.pose().scale(finalScale, finalScale, 1.0f);

        // Calculate scaled position
        int textWidth = font.width(comboText);
        int scaledX = (int)((centerX - textWidth / 2) / finalScale);
        int scaledY = (int)((centerY - 5) / finalScale);

        // Apply fade-out alpha to colors
        int fadeOutlineColor = applyAlpha(COMBO_OUTLINE_COLOR, fadeOutAlpha);
        int fadeTextColor = applyAlpha(COMBO_TEXT_COLOR, fadeOutAlpha);

        // Draw black outline
        guiGraphics.drawString(font, comboText, scaledX - 1, scaledY - 1, fadeOutlineColor);
        guiGraphics.drawString(font, comboText, scaledX, scaledY - 1, fadeOutlineColor);
        guiGraphics.drawString(font, comboText, scaledX + 1, scaledY - 1, fadeOutlineColor);
        guiGraphics.drawString(font, comboText, scaledX - 1, scaledY, fadeOutlineColor);
        guiGraphics.drawString(font, comboText, scaledX + 1, scaledY, fadeOutlineColor);
        guiGraphics.drawString(font, comboText, scaledX - 1, scaledY + 1, fadeOutlineColor);
        guiGraphics.drawString(font, comboText, scaledX, scaledY + 1, fadeOutlineColor);
        guiGraphics.drawString(font, comboText, scaledX + 1, scaledY + 1, fadeOutlineColor);

        // Main white text with fade
        guiGraphics.drawString(font, comboText, scaledX, scaledY, fadeTextColor);

        guiGraphics.pose().popPose();
    }

    /**
     * Render horizontal combo bar
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
            // Calculate fill width
            int fillWidth = (int)(BAR_WIDTH * fillPercentage);

            // Active bar color
            int barColor = BAR_ACTIVE_COLOR;

            // Add flash effect when combo extends
            if (barFlashIntensity > 0) {
                int flashAmount = (int)(255 * barFlashIntensity);
                barColor = blendColors(barColor, BAR_FLASH_COLOR, flashAmount);
            }

            // Draw filled portion
            guiGraphics.fill(barLeft, barTop, barLeft + fillWidth, barBottom, barColor);

            // Add edge glow
            if (fillWidth > 2) {
                int edgeX = barLeft + fillWidth;
                guiGraphics.fill(edgeX, barTop - 1, edgeX + 1, barBottom + 1, 0x80FFD700);
            }
        }
    }

    /**
     * Render damage counter with fade support
     */
    private static void renderDamage(GuiGraphics guiGraphics, Font font, int centerX, int damageY) {
        String damageText = String.format("%.1f", totalDamage);
        int damageWidth = font.width(damageText);
        int damageX = centerX - damageWidth / 2;

        // Apply fade-out alpha to damage colors
        int fadeOutlineColor = applyAlpha(COMBO_OUTLINE_COLOR, fadeOutAlpha);
        int fadeDamageColor = applyAlpha(DAMAGE_TEXT_COLOR, fadeOutAlpha);

        // Black outline for damage text
        guiGraphics.drawString(font, damageText, damageX - 1, damageY - 1, fadeOutlineColor);
        guiGraphics.drawString(font, damageText, damageX, damageY - 1, fadeOutlineColor);
        guiGraphics.drawString(font, damageText, damageX + 1, damageY - 1, fadeOutlineColor);
        guiGraphics.drawString(font, damageText, damageX - 1, damageY, fadeOutlineColor);
        guiGraphics.drawString(font, damageText, damageX + 1, damageY, fadeOutlineColor);
        guiGraphics.drawString(font, damageText, damageX - 1, damageY + 1, fadeOutlineColor);
        guiGraphics.drawString(font, damageText, damageX, damageY + 1, fadeOutlineColor);
        guiGraphics.drawString(font, damageText, damageX + 1, damageY + 1, fadeOutlineColor);

        // Main damage text with fade
        guiGraphics.drawString(font, damageText, damageX, damageY, fadeDamageColor);
    }

    // Utility methods
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

    private static int applyAlpha(int color, float alpha) {
        alpha = Mth.clamp(alpha, 0.0f, 1.0f);

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int originalAlpha = (color >> 24) & 0xFF;

        int newAlpha = (int)(originalAlpha * alpha);

        return (newAlpha << 24) | (r << 16) | (g << 8) | b;
    }

    // Reset and utility methods
    private static void resetCombo() {
        currentCombo = 0;
        lastValidCombo = 0;
        totalDamage = 0.0f;
        stunEndTime = 0;
        comboStartTime = 0;
        numberScale = 1.0f;
        barFlashIntensity = 0.0f;
        shouldFadeOut = false;
        fadeOutAlpha = 1.0f;
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