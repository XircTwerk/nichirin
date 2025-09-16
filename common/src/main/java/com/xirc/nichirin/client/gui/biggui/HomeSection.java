package com.xirc.nichirin.client.gui.biggui;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.data.MovesetProgression;
import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.common.util.PlayerStats;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Home section of THE BIG GUI - shows player stats and 3D model
 */
public class HomeSection {

    private static final int TOP_MARGIN = 20; // Moved up from 40

    public void render(GuiGraphics graphics, int mouseX, int mouseY, Player player,
                       int contentWidth, int contentHeight, Font font) {
        int contentX = 20;
        int contentY = TOP_MARGIN + 10;
        int centerX = (contentWidth - 20) / 2;

        // === 3D Player Model ===
        int modelX = centerX;
        int modelY = contentY + 110; // Moved up 30 pixels from 140 to 110
        int modelSize = 60;

        // Draw background for model area
        graphics.fill(modelX - modelSize - 5, modelY - modelSize - 5,
                modelX + modelSize + 5, modelY + modelSize + 5, 0xFF2A2A2A);
        graphics.fill(modelX - modelSize - 3, modelY - modelSize - 3,
                modelX + modelSize + 3, modelY + modelSize + 3, 0xFF1A1A1A);

        // Render 3D player model (mouse-following like inventory)
        PlayerModelRenderer.renderPlayerFollowingMouse(graphics, modelX, modelY, modelSize,
                mouseX, mouseY, player);

        // === Player Stats Section ===
        int statsY = contentY + 120 + modelSize + 20; // Fixed position independent of modelY
        int statLineHeight = 16;
        MovesetProgression progression = ProgressionHelper.getProgression(player);

        // Title
        Component statsTitle = Component.translatable("gui.nichirin.home.player_stats").withStyle(style -> style.withBold(true));
        graphics.drawString(font, statsTitle,
                centerX - font.width(statsTitle) / 2, statsY, 0xFFFFFF);
        statsY += 25;

        // Player name
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.name"),
                player.getName().getString(), 0xFFD700, contentWidth, font);
        statsY += statLineHeight;

        // Slayer rank - coming soon
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.slayer_rank"),
                "Coming Soon", 0x5555FF, contentWidth, font);
        statsY += statLineHeight;

        // Breathing style
        String breathingStyle = MovesetHelper.getMovesetId(player);
        String breathingStyleDisplay = breathingStyle != null ?
                Component.translatable("breathing_style." + breathingStyle).getString() :
                Component.translatable("gui.nichirin.home.none").getString();
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.breathing_style"),
                breathingStyleDisplay, 0x55FFFF, contentWidth, font);
        statsY += statLineHeight;

        // Combat stats separator
        statsY += 10;
        graphics.drawString(font, "─────────────────────", contentX, statsY, 0x555555);
        statsY += 15;

        // Demon kill count - coming soon
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.demons_slain"),
                "Coming Soon", 0xFF5555, contentWidth, font);
        statsY += statLineHeight;

        // Time played
        long playtime = player.level().getGameTime() / 20;
        String playtimeStr = formatPlaytime(playtime);
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.playtime"),
                playtimeStr, 0xAAAAAA, contentWidth, font);
        statsY += statLineHeight;

        // Check if stats are loaded
        boolean statsLoaded = PlayerStats.areStatsLoaded();

        // Mobs killed
        String mobsKilledDisplay;
        int mobsKilledColor;
        if (statsLoaded) {
            int mobsKilled = PlayerStats.getMobsKilled();
            mobsKilledDisplay = String.valueOf(mobsKilled);
            mobsKilledColor = 0xFF5555;
        } else {
            mobsKilledDisplay = "Loading...";
            mobsKilledColor = 0x777777;
        }
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.mobs_killed"),
                mobsKilledDisplay, mobsKilledColor, contentWidth, font);
        statsY += statLineHeight;

        // Deaths
        String deathsDisplay;
        int deathsColor;
        if (statsLoaded) {
            int deaths = PlayerStats.getDeaths();
            deathsDisplay = String.valueOf(deaths);
            deathsColor = 0xFF5555;
        } else {
            deathsDisplay = "Loading...";
            deathsColor = 0x777777;
        }
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.deaths"),
                deathsDisplay, deathsColor, contentWidth, font);

        // If stats aren't loaded, show a loading indicator
        if (!statsLoaded) {
            statsY += statLineHeight + 5;
            Component loadingText = Component.literal("Requesting stats from server...")
                    .withStyle(style -> style.withColor(0x777777).withItalic(true));
            graphics.drawString(font, loadingText,
                    centerX - font.width(loadingText) / 2, statsY, 0x777777);
        }

        // Instructions at bottom
        int bottomY = contentHeight - 30;
        Component instructions = Component.translatable("gui.nichirin.home.instructions")
                .withStyle(style -> style.withColor(0x777777).withItalic(true));
        graphics.drawString(font, instructions,
                (contentWidth - font.width(instructions)) / 2,
                bottomY, 0x777777);
    }

    public boolean handleClick(double mouseX, double mouseY, Player player) {
        // You could add a click handler here to manually request stats
        // For example, clicking on the stats area could force a stats refresh
        PlayerStats.requestStatsFromServer();
        return false;
    }

    /**
     * Renders a stat line with label and value
     */
    private void drawStatLine(GuiGraphics graphics, int x, int y, Component label, String value,
                              int valueColor, int contentWidth, Font font) {
        // Draw label
        String labelText = label.getString() + ":";
        graphics.drawString(font, labelText, x, y, 0xAAAAAA);

        // Draw value (right-aligned within content area)
        int valueX = contentWidth - 30 - font.width(value);
        graphics.drawString(font, value, valueX, y, valueColor);

        // Draw connecting dots
        String dots = ".".repeat(Math.max(1, (valueX - x - font.width(labelText) - 5) / 4));
        graphics.drawString(font, dots, x + font.width(labelText), y, 0x444444);
    }

    /**
     * Formats playtime into readable format
     */
    private String formatPlaytime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
}