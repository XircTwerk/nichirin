package com.xirc.nichirin.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Rhythm meter for Musical Score effect
 * Shows a timing indicator and beat zones for perfect attack timing
 */
@Environment(EnvType.CLIENT)
public class RhythmMeter {

    private static final int METER_WIDTH = 300;
    private static final int METER_HEIGHT = 20;
    private static final int PERFECT_ZONE_WIDTH = 30; // Width of the perfect timing zone
    private static final int GOOD_ZONE_WIDTH = 60; // Width of the good timing zone

    // Colors
    private static final int BACKGROUND_COLOR = 0x80000000;
    private static final int METER_COLOR = 0xFF444444;
    private static final int PERFECT_ZONE_COLOR = 0xFF00FF00; // Bright green
    private static final int GOOD_ZONE_COLOR = 0xFFFFFF00; // Yellow
    private static final int INDICATOR_COLOR = 0xFFFFFFFF; // White
    private static final int BORDER_COLOR = 0xFFFFFFFF;
    private static final int SUCCESS_COLOR = 0xFF00FF00; // Green for success

    // Success feedback tracking
    private static long lastSuccessTime = 0;
    private static final int SUCCESS_DISPLAY_DURATION = 20; // 20 ticks = 1 second

    /**
     * Render the rhythm meter if Musical Score is active
     */
    public static void render(GuiGraphics graphics, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        // Only show if player has Musical Score effect
        if (!minecraft.player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.MUSICAL_SCORE.get())) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        // Position meter at bottom center of screen
        int meterX = (screenWidth - METER_WIDTH) / 2;
        int meterY = screenHeight - 80; // 80 pixels from bottom

        renderRhythmMeter(graphics, meterX, meterY, partialTicks);
    }

    /**
     * Render the actual rhythm meter
     */
    private static void renderRhythmMeter(GuiGraphics graphics, int x, int y, float partialTicks) {
        long gameTime = Minecraft.getInstance().level.getGameTime();
        float time = gameTime + partialTicks;

        // Calculate beat timing (same as note sounds - every 10 ticks = 0.5 seconds)
        float beatCycle = (time % 10.0f) / 10.0f; // 0.0 to 1.0 cycle

        // Calculate indicator position (moves left to right)
        int indicatorX = x + (int)(METER_WIDTH * beatCycle);

        // Calculate perfect zone position (centered in meter)
        int perfectZoneCenter = x + METER_WIDTH / 2;
        int perfectZoneStart = perfectZoneCenter - PERFECT_ZONE_WIDTH / 2;
        int perfectZoneEnd = perfectZoneCenter + PERFECT_ZONE_WIDTH / 2;

        int goodZoneStart = perfectZoneCenter - GOOD_ZONE_WIDTH / 2;
        int goodZoneEnd = perfectZoneCenter + GOOD_ZONE_WIDTH / 2;

        // Check if showing success feedback
        boolean showingSuccess = (gameTime - lastSuccessTime) < SUCCESS_DISPLAY_DURATION;

        // Background
        graphics.fill(x, y, x + METER_WIDTH, y + METER_HEIGHT, BACKGROUND_COLOR);

        // Meter track
        graphics.fill(x + 2, y + 8, x + METER_WIDTH - 2, y + 12, METER_COLOR);

        // Good timing zone (yellow or green if success)
        int goodColor = showingSuccess ? SUCCESS_COLOR : GOOD_ZONE_COLOR;
        graphics.fill(goodZoneStart, y + 6, goodZoneEnd, y + 14, goodColor);

        // Perfect timing zone (green or brighter green if success)
        int perfectColor = showingSuccess ? 0xFF66FF66 : PERFECT_ZONE_COLOR; // Brighter green for success
        graphics.fill(perfectZoneStart, y + 7, perfectZoneEnd, y + 13, perfectColor);

        // Beat markers - make them more visible
        for (int i = 0; i <= 4; i++) { // 5 markers (including start and end)
            int markerX = x + (METER_WIDTH * i / 4);
            int markerColor = showingSuccess ? SUCCESS_COLOR : 0xFFCCCCCC;

            // Make beat markers taller and more prominent
            graphics.fill(markerX - 1, y + 2, markerX + 1, y + 18, markerColor);

            // Add beat numbers
            String beatNum = String.valueOf(i + 1);
            int textX = markerX - Minecraft.getInstance().font.width(beatNum) / 2;
            graphics.drawString(Minecraft.getInstance().font, beatNum,
                    textX, y - 8, markerColor, false);
        }

        // Moving indicator (white line or green if success)
        int indicatorColor = showingSuccess ? SUCCESS_COLOR : INDICATOR_COLOR;

        // Make indicator more prominent with a wider line and glow effect
        graphics.fill(indicatorX - 2, y + 1, indicatorX + 2, y + 19, indicatorColor);

        // Add glow effect around indicator
        graphics.fill(indicatorX - 3, y + 3, indicatorX + 3, y + 17, (indicatorColor & 0xFFFFFF) | 0x40000000);

        // Border (green if success)
        int borderColor = showingSuccess ? SUCCESS_COLOR : BORDER_COLOR;
        graphics.renderOutline(x, y, METER_WIDTH, METER_HEIGHT, borderColor);

        // Zone labels
        String perfectLabel = "PERFECT";
        String goodLabel = "GOOD";
        int perfectLabelX = perfectZoneCenter - Minecraft.getInstance().font.width(perfectLabel) / 2;
        int goodLabelX = goodZoneStart + (GOOD_ZONE_WIDTH - Minecraft.getInstance().font.width(goodLabel)) / 2;

        graphics.drawString(Minecraft.getInstance().font, perfectLabel,
                perfectLabelX, y + 1, 0xFF000000, false); // Black text on green zone

        // Only show "GOOD" label if it won't overlap with "PERFECT"
        if (goodZoneStart + Minecraft.getInstance().font.width(goodLabel) < perfectZoneStart - 5) {
            graphics.drawString(Minecraft.getInstance().font, goodLabel,
                    goodLabelX, y + 1, 0xFF000000, false);
        }

        // Text instructions
        String instructionText = showingSuccess ? "PERFECT RHYTHM!" : "Attack on the beat for bonus damage!";
        int textColor = showingSuccess ? SUCCESS_COLOR : 0xFFFFFFFF;
        int textWidth = Minecraft.getInstance().font.width(instructionText);
        int textX = x + (METER_WIDTH - textWidth) / 2;
        graphics.drawString(Minecraft.getInstance().font, instructionText,
                textX, y - 12, textColor, true);

        // Show current timing quality
        TimingQuality timing = getCurrentTiming();
        if (timing != TimingQuality.NONE) {
            String timingText = timing.getDisplayName();
            int timingColor = showingSuccess ? SUCCESS_COLOR : timing.getColor();
            int timingTextX = x + (METER_WIDTH - Minecraft.getInstance().font.width(timingText)) / 2;
            graphics.drawString(Minecraft.getInstance().font, timingText,
                    timingTextX, y + METER_HEIGHT + 4, timingColor, true);

            // Show damage multiplier
            if (timing != TimingQuality.OFF_BEAT && timing != TimingQuality.NONE) {
                String multiplierText = String.format("%.1fx Damage", timing.getDamageMultiplier() * 3.0f);
                int multiplierX = x + (METER_WIDTH - Minecraft.getInstance().font.width(multiplierText)) / 2;
                graphics.drawString(Minecraft.getInstance().font, multiplierText,
                        multiplierX, y + METER_HEIGHT + 16, timingColor, true);
            }
        }

        // Success pulse effect
        if (showingSuccess) {
            long timeSinceSuccess = gameTime - lastSuccessTime;
            float pulseAlpha = 1.0f - (timeSinceSuccess / (float)SUCCESS_DISPLAY_DURATION);
            int pulseColor = (int)(pulseAlpha * 128) << 24 | 0x00FF00;

            // Create pulsing border effect
            graphics.fill(x - 2, y - 2, x + METER_WIDTH + 2, y, pulseColor);
            graphics.fill(x - 2, y + METER_HEIGHT, x + METER_WIDTH + 2, y + METER_HEIGHT + 2, pulseColor);
            graphics.fill(x - 2, y, x, y + METER_HEIGHT, pulseColor);
            graphics.fill(x + METER_WIDTH, y, x + METER_WIDTH + 2, y + METER_HEIGHT, pulseColor);
        }
    }

    /**
     * Check if the current timing is perfect for an attack
     */
    public static TimingQuality getCurrentTiming() {
        if (Minecraft.getInstance().player == null) return TimingQuality.NONE;

        // Only check timing if Musical Score is active
        if (!Minecraft.getInstance().player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.MUSICAL_SCORE.get())) {
            return TimingQuality.NONE;
        }

        long gameTime = Minecraft.getInstance().level.getGameTime();
        float beatCycle = (gameTime % 10.0f) / 10.0f; // 0.0 to 1.0 cycle

        // Calculate distance from center (0.5 = perfect beat)
        float distanceFromCenter = Math.abs(beatCycle - 0.5f);

        // Perfect timing (within 15% of center)
        if (distanceFromCenter <= 0.15f) {
            return TimingQuality.PERFECT;
        }
        // Good timing (within 30% of center)
        else if (distanceFromCenter <= 0.30f) {
            return TimingQuality.GOOD;
        }
        // Off beat
        else {
            return TimingQuality.OFF_BEAT;
        }
    }

    /**
     * Get damage multiplier based on current timing
     */
    public static float getDamageMultiplier() {
        TimingQuality timing = getCurrentTiming();
        return timing.getDamageMultiplier();
    }

    /**
     * Check if current timing allows no cooldown
     */
    public static boolean allowsNoCooldown() {
        TimingQuality timing = getCurrentTiming();
        return timing.allowsNoCooldown();
    }

    /**
     * Trigger success feedback when player hits perfect timing
     */
    public static void triggerSuccess() {
        lastSuccessTime = Minecraft.getInstance().level != null ?
                Minecraft.getInstance().level.getGameTime() : System.currentTimeMillis() / 50;
    }

    /**
     * Timing quality enum
     */
    public enum TimingQuality {
        PERFECT("PERFECT!", 0xFF00FF00, 2.0f, true),
        GOOD("Good", 0xFFFFFF00, 1.5f, false),
        OFF_BEAT("Off Beat", 0xFFFF0000, 1.0f, false),
        NONE("", 0xFFFFFFFF, 1.0f, false);

        private final String displayName;
        private final int color;
        private final float damageMultiplier;
        private final boolean noCooldown;

        TimingQuality(String displayName, int color, float damageMultiplier, boolean noCooldown) {
            this.displayName = displayName;
            this.color = color;
            this.damageMultiplier = damageMultiplier;
            this.noCooldown = noCooldown;
        }

        public String getDisplayName() { return displayName; }
        public int getColor() { return color; }
        public float getDamageMultiplier() { return damageMultiplier; }
        public boolean allowsNoCooldown() { return noCooldown; }
    }
}