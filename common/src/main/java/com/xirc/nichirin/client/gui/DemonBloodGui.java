package com.xirc.nichirin.client.gui;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.system.DemonComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Renders the blood bar for demons, replacing the hunger bar
 */
public class DemonBloodGui {

    private static final ResourceLocation BLOOD_FULL = new ResourceLocation("nichirin", "textures/gui/blood_full.png");
    private static final ResourceLocation BLOOD_HALF = new ResourceLocation("nichirin", "textures/gui/blood_half.png");
    private static final ResourceLocation BLOOD_EMPTY = new ResourceLocation("nichirin", "textures/gui/blood_empty.png");

    // Blood bar dimensions (same as hunger bar)
    private static final int BLOOD_BAR_WIDTH = 9;
    private static final int BLOOD_BAR_HEIGHT = 9;
    private static final int BLOOD_SEGMENTS = 10;

    // Client-side blood tracking
    private static int clientFullBloodPoints = 10;
    private static int clientHalfBloodPoints = 0;

    // Track previous values to only log changes (for debugging)
    private static int lastFullBlood = -1;
    private static int lastHalfBlood = -1;

    /**
     * Updates blood points from server sync
     */
    public static void updateBloodPoints(int bloodPoints, boolean isDemon) {
        if (isDemon) {
            clientFullBloodPoints = Math.max(0, Math.min(bloodPoints, 10));
            DemonComponent.setClientBloodPoints(bloodPoints);
        }
    }

    /**
     * Updates half-blood points from server sync
     */
    public static void updateHalfBloodPoints(int halfBloodPoints) {
        clientHalfBloodPoints = Math.max(0, Math.min(halfBloodPoints, 1)); // Only 0 or 1 allowed
    }

    /**
     * Called when player dies/respawns to reset blood to full
     */
    public static void onPlayerRespawn() {
        clientFullBloodPoints = 10;
        clientHalfBloodPoints = 0;
        lastFullBlood = -1; // Force debug log update
        lastHalfBlood = -1;
    }

    /**
     * Renders the blood bar overlay on the HUD
     */
    public static void renderBloodBar(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || !MovesetHelper.hasDemonMoveset(player)) {
            return; // Not a demon, don't render
        }

        // Don't render in creative or spectator mode
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        // Get current blood state
        int fullBloodPoints = clientFullBloodPoints;
        int halfBloodPoints = clientHalfBloodPoints;

        // Clamp half blood points to valid range (0 or 1)
        halfBloodPoints = Math.max(0, Math.min(halfBloodPoints, 1));

        // Debug logging only when values change
        if (fullBloodPoints != lastFullBlood || halfBloodPoints != lastHalfBlood) {
            lastFullBlood = fullBloodPoints;
            lastHalfBlood = halfBloodPoints;
        }

        // Calculate position (same as hunger bar position)
        int left = screenWidth / 2 + 91;
        int top = screenHeight - 39;

        // Render blood segments (right to left, like hunger bar)
        for (int i = 0; i < BLOOD_SEGMENTS; i++) {
            int x = left - i * 8 - 9;
            int y = top;

            // Calculate what texture to use for this segment
            ResourceLocation texture = getTextureForSegment(i, fullBloodPoints, halfBloodPoints);

            // Render the blood segment
            graphics.blit(texture, x, y, 0, 0,
                    BLOOD_BAR_WIDTH, BLOOD_BAR_HEIGHT,
                    BLOOD_BAR_WIDTH, BLOOD_BAR_HEIGHT);
        }
    }

    /**
     * Determines the correct texture for a blood segment
     * Fixed logic: Half-blood represents DAMAGE TAKEN, not extra blood
     * So "Full: 9, Half: 1" means 8.5 blood remaining (9 - 0.5)
     */
    private static ResourceLocation getTextureForSegment(int segmentIndex, int fullBlood, int halfBlood) {
        // Calculate actual blood remaining (half-blood represents damage taken)
        double actualBlood = fullBlood - (halfBlood * 0.5);

        // Handle edge case: if actualBlood is 0 or negative, everything should be empty
        if (actualBlood <= 0) {
            return BLOOD_EMPTY;
        }

        // Segments render right to left: index 0 = rightmost = blood point 1
        // index 9 = leftmost = blood point 10
        double segmentBloodValue = segmentIndex + 1;

        if (actualBlood >= segmentBloodValue) {
            // This segment should be completely full
            return BLOOD_FULL;
        } else if (actualBlood >= segmentBloodValue - 0.5) {
            // This segment should be half full
            return BLOOD_HALF;
        } else {
            // This segment should be empty
            return BLOOD_EMPTY;
        }
    }

    /**
     * Checks if we should hide the vanilla hunger bar for demons
     */
    public static boolean shouldHideHungerBar(Player player) {
        if (player == null || !MovesetHelper.hasDemonMoveset(player)) {
            return false;
        }

        // Don't hide hunger bar in creative or spectator mode
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }

        return true;
    }

    /**
     * Get current full blood points (for other systems)
     */
    public static int getClientFullBloodPoints() {
        return clientFullBloodPoints;
    }

    /**
     * Get current half blood points (for other systems)
     */
    public static int getClientHalfBloodPoints() {
        return clientHalfBloodPoints;
    }
}