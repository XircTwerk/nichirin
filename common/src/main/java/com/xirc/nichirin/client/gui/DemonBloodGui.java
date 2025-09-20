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

    /**
     * Updates blood points from server sync
     */
    public static void updateBloodPoints(int bloodPoints, boolean isDemon) {
        if (isDemon) {
            DemonComponent.setClientBloodPoints(bloodPoints);
        }
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

        // Get blood points from component
        int bloodPoints = DemonComponent.getClientBloodPoints();

        // Calculate position (same as hunger bar position)
        int left = screenWidth / 2 + 91;
        int top = screenHeight - 39;

        // Render blood segments
        for (int i = 0; i < BLOOD_SEGMENTS; i++) {
            int x = left - i * 8 - 9;
            int y = top;

            // Determine which texture to use based on blood level
            ResourceLocation texture;

            if (i < bloodPoints) {
                // Full blood segment
                texture = BLOOD_FULL;
            } else {
                // Empty blood segment
                texture = BLOOD_EMPTY;
            }

            // Render the blood segment
            graphics.blit(texture, x, y, 0, 0,
                    BLOOD_BAR_WIDTH, BLOOD_BAR_HEIGHT,
                    BLOOD_BAR_WIDTH, BLOOD_BAR_HEIGHT);
        }
    }

    /**
     * Checks if we should hide the vanilla hunger bar for demons
     */
    public static boolean shouldHideHungerBar(Player player) {
        return player != null && MovesetHelper.hasDemonMoveset(player);
    }
}