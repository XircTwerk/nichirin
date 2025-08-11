package com.xirc.nichirin.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Rhythm meter for Musical Score effect
 * Shows moving musical notes that players must hit when they reach the center
 */
@Environment(EnvType.CLIENT)
public class RhythmMeter {

    private static final int METER_WIDTH = 400;
    private static final int METER_HEIGHT = 80;
    private static final int HIT_ZONE_WIDTH = 60;
    private static final int HIT_ZONE_HEIGHT = 50;
    private static final int NOTE_SIZE = 12;

    // Colors
    private static final int BACKGROUND_COLOR = 0x80000000;
    private static final int HIT_ZONE_BORDER_COLOR = 0xFF8B4513; // Staff line brown
    private static final int HIT_ZONE_FILL_COLOR = 0x80F5DEB3; // Semi-transparent bone color
    private static final int NOTE_COLOR = 0xFF000000; // Black notes
    private static final int SUCCESS_COLOR = 0xFF00FF00;

    // Note timing
    private static final int NOTE_SPAWN_INTERVAL = 40; // How often notes spawn
    private static final int NOTE_TRAVEL_TIME = 60; // How long notes take to cross the meter
    private static final int MAX_NOTES = 3; // Maximum notes on screen at once

    // Success feedback tracking
    private static long lastSuccessTime = 0;
    private static final int SUCCESS_DISPLAY_DURATION = 20; // 20 ticks = 1 second

    // Score tracking
    private static int perfectHitScore = 0;
    private static long scoreResetTime = 0;
    private static final int SCORE_RESET_DURATION = 100; // 5 seconds to reset score

    // Debug logging throttling
    private static long lastDebugTime = 0;
    private static final int DEBUG_COOLDOWN = 20; // Only log debug info every 20 ticks (1 second)
    private static long lastClearTime = 0;
    private static final int CLEAR_LOG_COOLDOWN = 10; // Only log note clearing every 10 ticks

    // Note data structure
    private static class MusicalNote {
        float x; // Current X position
        float y; // Y position on staff
        long spawnTime; // When this note was created
        boolean active; // Whether this note is still moving

        MusicalNote(float startX, float staffY, long gameTime) {
            this.x = startX;
            this.y = staffY;
            this.spawnTime = gameTime;
            this.active = true;
        }
    }

    // Store active notes
    private static final java.util.List<MusicalNote> activeNotes = new java.util.ArrayList<>();

    /**
     * Get consistent positioning values used across all methods
     */
    private static class PositionInfo {
        final int screenWidth;
        final int screenHeight;
        final int centerX;
        final int centerY;
        final int meterStartX;
        final int meterEndX;
        final int hitZoneLeft;
        final int hitZoneRight;

        PositionInfo() {
            screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

            // Position to match Musical Score overlay exactly
            int staffSpacing = screenHeight / 8;
            centerX = screenWidth / 2;
            centerY = screenHeight / 4 + 2 * (staffSpacing / 5);

            // Meter bounds
            meterStartX = centerX - (METER_WIDTH / 2);
            meterEndX = centerX + (METER_WIDTH / 2);

            // Hit zone bounds (centered on screen)
            hitZoneLeft = centerX - (HIT_ZONE_WIDTH / 2);
            hitZoneRight = centerX + (HIT_ZONE_WIDTH / 2);
        }
    }

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

        PositionInfo pos = new PositionInfo();

        // Debug positioning (throttled)
        long currentTime = minecraft.level.getGameTime();
        if (currentTime - lastDebugTime >= DEBUG_COOLDOWN) {
            System.out.println("DEBUG RHYTHM: Screen=" + pos.screenWidth + "x" + pos.screenHeight +
                    ", Center=" + pos.centerX + "," + pos.centerY +
                    ", HitZone=[" + pos.hitZoneLeft + "-" + pos.hitZoneRight + "]" +
                    ", ActiveNotes=" + activeNotes.size());
            lastDebugTime = currentTime;
        }

        // Render hit zone and meter
        renderHitZone(graphics, pos, partialTicks);
        renderRhythmMeter(graphics, pos, partialTicks);
    }

    /**
     * Render the hit zone - pulsing with the beat
     */
    private static void renderHitZone(GuiGraphics graphics, PositionInfo pos, float partialTicks) {
        long gameTime = Minecraft.getInstance().level.getGameTime();
        boolean showingSuccess = (gameTime - lastSuccessTime) < SUCCESS_DISPLAY_DURATION;

        // Calculate hit zone position
        int hitZoneX = pos.hitZoneLeft;
        int hitZoneY = pos.centerY - (HIT_ZONE_HEIGHT / 2);

        // Create pulsing effect
        float beatCycle = (gameTime % 40.0f) / 40.0f;
        float pulse = 0.9f + 0.1f * (float)Math.sin(beatCycle * Math.PI * 2);

        // Calculate pulsed size
        int pulsedWidth = (int)(HIT_ZONE_WIDTH * pulse);
        int pulsedHeight = (int)(HIT_ZONE_HEIGHT * pulse);
        int pulsedX = hitZoneX + (HIT_ZONE_WIDTH - pulsedWidth) / 2;
        int pulsedY = hitZoneY + (HIT_ZONE_HEIGHT - pulsedHeight) / 2;

        // Draw background
        int backgroundColor = showingSuccess ? 0xCCFFFF00 : HIT_ZONE_FILL_COLOR;
        graphics.fill(pulsedX, pulsedY, pulsedX + pulsedWidth, pulsedY + pulsedHeight, backgroundColor);

        // Draw borders
        int borderColor = showingSuccess ? SUCCESS_COLOR : HIT_ZONE_BORDER_COLOR;
        int thickness = Math.max(1, (int)(3 * pulse));

        // Draw border
        graphics.renderOutline(pulsedX - thickness, pulsedY - thickness,
                pulsedWidth + (thickness * 2), pulsedHeight + (thickness * 2), borderColor);

        // Draw text
        String label = "♪ HIT ♪";
        int labelWidth = Minecraft.getInstance().font.width(label);
        int labelX = pulsedX + (pulsedWidth - labelWidth) / 2;
        int labelY = pulsedY + (pulsedHeight / 2) - 4;

        graphics.drawString(Minecraft.getInstance().font, label, labelX, labelY, NOTE_COLOR, true);

        // Render score counter above the hit zone
        renderScoreCounter(graphics, hitZoneX, hitZoneY - 30, gameTime);
    }

    /**
     * Render the perfect hit score counter
     */
    private static void renderScoreCounter(GuiGraphics graphics, int x, int y, long gameTime) {
        // Reset score if no perfect hits for a while
        if (gameTime - scoreResetTime > SCORE_RESET_DURATION && perfectHitScore > 0) {
            perfectHitScore = 0;
        }

        if (perfectHitScore > 0) {
            String scoreText = "Perfect Hits: " + perfectHitScore;
            int textWidth = Minecraft.getInstance().font.width(scoreText);
            int textX = x + (HIT_ZONE_WIDTH - textWidth) / 2;

            // Pulsing effect for score display
            float scorePulse = 0.8f + 0.2f * (float)Math.sin(gameTime * 0.1f);
            int scoreColor = (int)(255 * scorePulse) << 24 | 0xFFD700; // Pulsing gold

            // Background for score
            graphics.fill(textX - 5, y - 5, textX + textWidth + 5, y + 15, 0x80000000);
            graphics.drawString(Minecraft.getInstance().font, scoreText, textX, y, scoreColor, true);
        }
    }

    /**
     * Render the actual rhythm meter with notes
     */
    private static void renderRhythmMeter(GuiGraphics graphics, PositionInfo pos, float partialTicks) {
        long gameTime = Minecraft.getInstance().level.getGameTime();

        // Update and manage notes
        updateNotes(gameTime, pos);

        // Draw active notes
        for (MusicalNote note : activeNotes) {
            if (note.active) {
                drawMusicalNote(graphics, (int)note.x, (int)note.y);
            }
        }

        // Show timing status
        TimingQuality timing = getCurrentTiming();
        if (timing != TimingQuality.NONE) {
            String timingText = timing.getDisplayName();
            if (!timingText.isEmpty()) {
                int timingColor = timing.getColor();
                int timingTextX = pos.centerX - (Minecraft.getInstance().font.width(timingText) / 2);
                graphics.drawString(Minecraft.getInstance().font, timingText,
                        timingTextX, pos.centerY + 40, timingColor, true);

                if (timing != TimingQuality.OFF_BEAT && timing != TimingQuality.NONE) {
                    String multiplierText = String.format("%.1fx Damage", timing.getDamageMultiplier());
                    int multiplierX = pos.centerX - (Minecraft.getInstance().font.width(multiplierText) / 2);
                    graphics.drawString(Minecraft.getInstance().font, multiplierText,
                            multiplierX, pos.centerY + 52, timingColor, true);
                }
            }
        }
    }

    /**
     * Update note positions and spawn new notes
     */
    private static void updateNotes(long gameTime, PositionInfo pos) {
        // Remove notes that have finished
        activeNotes.removeIf(note -> !note.active || note.x > pos.meterEndX + NOTE_SIZE);

        // Spawn new notes periodically
        if (gameTime % NOTE_SPAWN_INTERVAL == 0 && activeNotes.size() < MAX_NOTES) {
            spawnNewNote(gameTime, pos);
        }

        // Update existing note positions
        for (MusicalNote note : activeNotes) {
            if (note.active) {
                // Calculate note progress (0.0 to 1.0)
                float progress = (gameTime - note.spawnTime) / (float)NOTE_TRAVEL_TIME;

                if (progress >= 1.0f) {
                    note.active = false; // Note has reached the end
                } else {
                    // Move note from left to right across the FULL meter
                    note.x = pos.meterStartX - NOTE_SIZE + (progress * (METER_WIDTH + NOTE_SIZE * 2));
                }
            }
        }
    }

    /**
     * Spawn a new musical note
     */
    private static void spawnNewNote(long gameTime, PositionInfo pos) {
        // Position note randomly around the center area
        float noteY = pos.centerY + (Minecraft.getInstance().level.getRandom().nextFloat() - 0.5f) * 30;

        // Start note off the left side of the meter
        float startX = pos.meterStartX - NOTE_SIZE;

        activeNotes.add(new MusicalNote(startX, noteY, gameTime));

        // Debug note spawning (throttled)
        if (gameTime - lastDebugTime >= DEBUG_COOLDOWN) {
            System.out.println("DEBUG: Spawned note at X=" + startX + ", Y=" + noteY);
        }
    }

    /**
     * Draw a single musical note
     */
    private static void drawMusicalNote(GuiGraphics graphics, int x, int y) {
        // Draw note head (filled oval)
        graphics.fill(x, y + 3, x + 8, y + 9, NOTE_COLOR);

        // Draw note stem (vertical line)
        graphics.fill(x + 7, y - 3, x + 9, y + 6, NOTE_COLOR);

        // Draw note flag (curved lines at top)
        graphics.fill(x + 9, y - 3, x + 12, y - 1, NOTE_COLOR);
        graphics.fill(x + 9, y - 1, x + 11, y + 1, NOTE_COLOR);
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

        PositionInfo pos = new PositionInfo();

        // Debug timing check (throttled)
        long currentTime = Minecraft.getInstance().level.getGameTime();
        boolean shouldDebug = currentTime - lastDebugTime >= DEBUG_COOLDOWN;

        TimingQuality bestTiming = TimingQuality.OFF_BEAT;
        float bestOverlap = 0f;

        for (MusicalNote note : activeNotes) {
            if (note.active) {
                int noteLeft = (int)note.x;
                int noteRight = (int)note.x + NOTE_SIZE;

                // Check if note overlaps with hit zone
                if (noteRight > pos.hitZoneLeft && noteLeft < pos.hitZoneRight) {
                    // Calculate overlap
                    int overlapLeft = Math.max(noteLeft, pos.hitZoneLeft);
                    int overlapRight = Math.min(noteRight, pos.hitZoneRight);
                    int overlapWidth = overlapRight - overlapLeft;
                    float overlapPercent = (float)overlapWidth / NOTE_SIZE;

                    if (overlapPercent > bestOverlap) {
                        bestOverlap = overlapPercent;

                        if (overlapPercent >= 0.7f) { // 70% or more of note in hit zone
                            bestTiming = TimingQuality.PERFECT;
                        } else if (overlapPercent >= 0.3f) { // 30% or more of note in hit zone
                            bestTiming = TimingQuality.GOOD;
                        }
                    }

                    if (shouldDebug) {
                        System.out.println("DEBUG TIMING: Note at X=" + note.x + " has " +
                                String.format("%.1f", overlapPercent * 100) + "% overlap -> " +
                                (overlapPercent >= 0.7f ? "PERFECT" :
                                        overlapPercent >= 0.3f ? "GOOD" : "MISS"));
                    }
                }
            }
        }

        if (shouldDebug && activeNotes.size() > 0) {
            System.out.println("DEBUG TIMING: Best timing = " + bestTiming +
                    " (overlap: " + String.format("%.1f", bestOverlap * 100) + "%)");
        }

        return bestTiming;
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

        // Increment perfect hit score
        perfectHitScore++;
        scoreResetTime = lastSuccessTime;

        System.out.println("DEBUG: Perfect hit triggered! Score: " + perfectHitScore);

        // Remove the note that was hit
        removeHitNote();
    }

    /**
     * Remove the note that was successfully hit
     */
    private static void removeHitNote() {
        PositionInfo pos = new PositionInfo();

        // Find and remove the note with the most overlap in the hit zone
        MusicalNote bestNote = null;
        float bestOverlap = 0;

        for (MusicalNote note : activeNotes) {
            if (note.active) {
                int noteLeft = (int)note.x;
                int noteRight = (int)note.x + NOTE_SIZE;

                // Check if note overlaps with hit zone
                if (noteRight > pos.hitZoneLeft && noteLeft < pos.hitZoneRight) {
                    int overlapLeft = Math.max(noteLeft, pos.hitZoneLeft);
                    int overlapRight = Math.min(noteRight, pos.hitZoneRight);
                    int overlapWidth = overlapRight - overlapLeft;
                    float overlapPercent = (float)overlapWidth / NOTE_SIZE;

                    if (overlapPercent > bestOverlap) {
                        bestOverlap = overlapPercent;
                        bestNote = note;
                    }
                }
            }
        }

        if (bestNote != null) {
            bestNote.active = false;
            System.out.println("DEBUG: Removed hit note with " + (bestOverlap * 100) + "% overlap");
        }
    }

    /**
     * Clear only the notes that were being targeted by an attack
     * This is called after any attack attempt (perfect, good, or miss)
     * Call this from your existing attack code!
     */
    public static void clearTargetedNotes() {
        if (Minecraft.getInstance().player == null) return;

        // Only clear if Musical Score is active
        if (!Minecraft.getInstance().player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.MUSICAL_SCORE.get())) {
            return;
        }

        System.out.println("DEBUG CLEAR: clearTargetedNotes() called with " + activeNotes.size() + " active notes");

        PositionInfo pos = new PositionInfo();
        long currentTime = Minecraft.getInstance().level.getGameTime();

        // Count notes before clearing
        int notesBeforeClearing = (int) activeNotes.stream().filter(note -> note.active).count();
        int notesInHitZone = 0;

        // First, count how many notes are actually in the hit zone
        for (MusicalNote note : activeNotes) {
            if (note.active) {
                int noteLeft = (int)note.x;
                int noteRight = (int)note.x + NOTE_SIZE;

                if (noteRight > pos.hitZoneLeft && noteLeft < pos.hitZoneRight) {
                    notesInHitZone++;
                    System.out.println("DEBUG CLEAR: Note at X=" + note.x + " is in hit zone [" +
                            pos.hitZoneLeft + "-" + pos.hitZoneRight + "]");
                }
            }
        }

        System.out.println("DEBUG CLEAR: Found " + notesInHitZone + " notes in hit zone out of " + notesBeforeClearing + " total");

        // Only remove notes that are actually overlapping the hit zone
        activeNotes.removeIf(note -> {
            if (!note.active) {
                System.out.println("DEBUG CLEAR: Removing inactive note");
                return true; // Remove inactive notes anyway
            }

            int noteLeft = (int)note.x;
            int noteRight = (int)note.x + NOTE_SIZE;

            // Only remove if note significantly overlaps with hit zone (at least 30%)
            if (noteRight > pos.hitZoneLeft && noteLeft < pos.hitZoneRight) {
                int overlapLeft = Math.max(noteLeft, pos.hitZoneLeft);
                int overlapRight = Math.min(noteRight, pos.hitZoneRight);
                int overlapWidth = overlapRight - overlapLeft;
                float overlapPercent = (float)overlapWidth / NOTE_SIZE;

                boolean shouldRemove = overlapPercent >= 0.3f;
                System.out.println("DEBUG CLEAR: Note at X=" + note.x + " has " +
                        String.format("%.1f", overlapPercent * 100) + "% overlap -> " +
                        (shouldRemove ? "REMOVING" : "KEEPING"));

                return shouldRemove; // Only clear notes with 30%+ overlap
            }

            System.out.println("DEBUG CLEAR: Note at X=" + note.x + " is outside hit zone -> KEEPING");
            return false; // Don't remove notes outside hit zone
        });

        // Count notes after clearing
        int notesAfterClearing = (int) activeNotes.stream().filter(note -> note.active).count();
        int notesCleared = notesBeforeClearing - notesAfterClearing;

        System.out.println("DEBUG CLEAR: FINAL RESULT - Cleared " + notesCleared + " notes. Remaining: " + notesAfterClearing);
    }

    /**
     * Clear all active notes (when effect ends)
     */
    public static void clearNotes() {
        int clearedCount = activeNotes.size();
        activeNotes.clear();
        resetScore();

        if (clearedCount > 0) {
            System.out.println("DEBUG: Cleared all " + clearedCount + " notes (Musical Score ended)");
        }
    }

    /**
     * Reset the perfect hit score
     */
    public static void resetScore() {
        if (perfectHitScore > 0) {
            System.out.println("DEBUG: Reset perfect hit score from " + perfectHitScore + " to 0");
        }
        perfectHitScore = 0;
        scoreResetTime = 0;
    }

    /**
     * Get current perfect hit score
     */
    public static int getPerfectHitScore() {
        return perfectHitScore;
    }

    /**
     * Timing quality enum
     */
    public enum TimingQuality {
        PERFECT("PERFECT!", 0xFF00FF00, 2.0f, true),
        GOOD("Good", 0xFFFFFF00, 1.5f, false),
        OFF_BEAT("Miss", 0xFFFF0000, 1.0f, false),
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