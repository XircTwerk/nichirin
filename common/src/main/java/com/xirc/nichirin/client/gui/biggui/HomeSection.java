package com.xirc.nichirin.client.gui.biggui;

import com.xirc.nichirin.client.data.ClientProgressionCache;
import com.xirc.nichirin.common.data.MovesetData;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.util.PlayerStats;
import com.xirc.nichirin.registry.MovesetRegistry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Home section of THE BIG GUI - shows player stats and 3D model
 */
public class HomeSection extends AbstractGuiPage {

    private static final int TOP_MARGIN = 20;

    public void render(GuiGraphics graphics, int mouseX, int mouseY, Player player,
                       int contentWidth, int contentHeight, Font font) {
        MovesetData movesetData = MovesetHelper.getMovesetData(player);
        int identityAccent = movesetData.hasDemonMoveset() ? COLOR_PALETTE.DANGER.argb() : COLOR_PALETTE.ACCENT.argb();
        drawWorkspaceChrome(graphics, font, contentWidth, contentHeight,
                "Home", player.getName().getString(),
                currentPath(movesetData), "F5 refresh stats",
                PlayerStats.areStatsLoaded() ? "stats ready" : "loading stats");

        int contentX = 20;
        int contentY = workspaceBodyY() + 12;
        int centerX = (contentWidth - 20) / 2;

        // 3D Player Model
        int modelX = centerX;
        int modelY = contentY + 110;
        int modelSize = 60;

        // Draw background for model area
        graphics.fill(modelX - modelSize - 7, modelY - modelSize - 7,
                modelX + modelSize + 7, modelY + modelSize + 7, withAlpha(identityAccent, 0x24));
        graphics.fill(modelX - modelSize - 5, modelY - modelSize - 5,
                modelX + modelSize + 5, modelY + modelSize + 5, identityAccent);
        graphics.fill(modelX - modelSize - 3, modelY - modelSize - 3,
                modelX + modelSize + 3, modelY + modelSize + 3, COLOR_PALETTE.PANEL_MID.argb());
        graphics.fill(modelX - modelSize - 3, modelY - modelSize - 3,
                modelX + modelSize + 3, modelY - modelSize - 1, withAlpha(identityAccent, 0xCC));

        // Render 3D player model (mouse-following like inventory)
        PlayerModelRenderer.renderPlayerFollowingMouse(graphics, modelX, modelY, modelSize,
                mouseX, mouseY, player);

        // Player Stats Section
        int statsY = contentY + 120 + modelSize + 20;
        int statLineHeight = 16;

        // Title
        Component statsTitle = Component.translatable("gui.nichirin.home.player_stats").withStyle(style -> style.withBold(true));
        drawAccentTitle(graphics, font, statsTitle, centerX, statsY, identityAccent);
        statsY += 25;

        // Player name
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.name"),
                player.getName().getString(), COLOR_PALETTE.GOLD.rgb(), contentWidth, font);
        statsY += statLineHeight;

        // Path - based only on currently equipped movesets.
        String faction = currentPath(movesetData);
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.faction"),
                faction, pathColor(movesetData), contentWidth, font);
        statsY += statLineHeight;

        // Current breathing style
        String breathingStyle = movesetData.getBreathingMovesetId();
        String breathingStyleDisplay = breathingStyle != null ?
                Component.translatable("breathing_style." + breathingStyle).getString() :
                Component.translatable("gui.nichirin.home.none").getString();
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.breathing_style"),
                breathingStyleDisplay, COLOR_PALETTE.BREATH_CYAN.rgb(), contentWidth, font);
        statsY += statLineHeight;

        // Current demon art
        String demonArt = movesetData.getDemonMovesetId();
        String demonArtDisplay = demonArt != null ?
                Component.translatable("demon_art." + demonArt).getString() :
                Component.translatable("gui.nichirin.home.none").getString();
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.demon_art"),
                demonArtDisplay, COLOR_PALETTE.DEMON_RED.rgb(), contentWidth, font);
        statsY += statLineHeight;

        // Unlocks separator
        statsY += 10;
        String sectionTitle = "Unlocks";
        graphics.fill(contentX - 5, statsY - 2, contentX - 3, statsY + font.lineHeight + 2, identityAccent);
        graphics.drawString(font, sectionTitle, contentX, statsY, COLOR_PALETTE.TEXT.rgb());
        statsY += 15;

        int unlockedBreathing = ClientProgressionCache.getUnlockedBreathingStyles().size();
        int unlockedDemonArts = ClientProgressionCache.getUnlockedDemonArts().size();
        int totalUnlocked = ClientProgressionCache.getUnlockedMovesets().size();
        int totalRegistered = MovesetRegistry.getAllMovesetIds().size();
        int registeredBreathing = countRegistered("breathing");
        int registeredDemon = countRegistered("demon");
        drawStatLine(graphics, contentX, statsY, Component.literal("Breathing styles"),
                unlockedBreathing + " / " + registeredBreathing, COLOR_PALETTE.BREATH_CYAN.rgb(), contentWidth, font);
        statsY += statLineHeight;
        drawStatLine(graphics, contentX, statsY, Component.literal("Demon arts"),
                unlockedDemonArts + " / " + registeredDemon, COLOR_PALETTE.DEMON_RED.rgb(), contentWidth, font);
        statsY += statLineHeight;
        drawStatLine(graphics, contentX, statsY, Component.literal("Movesets total"),
                totalUnlocked + " / " + totalRegistered, COLOR_PALETTE.ACCENT.rgb(), contentWidth, font);
        statsY += statLineHeight;

        // General stats separator
        statsY += 10;
        graphics.fill(contentX, statsY + 4, contentX + 122, statsY + 5, COLOR_PALETTE.DARK_GRAY.argb());
        graphics.fill(contentX, statsY + 4, contentX + 34, statsY + 5, withAlpha(identityAccent, 0xAA));
        statsY += 15;

        // Time played
        long playtime = player.level().getGameTime() / 20;
        String playtimeStr = formatPlaytime(playtime);
        drawStatLine(graphics, contentX, statsY, Component.literal("World time"),
                playtimeStr, COLOR_PALETTE.GRAY.rgb(), contentWidth, font);
        statsY += statLineHeight;

        // Check if stats are loaded
        boolean statsLoaded = PlayerStats.areStatsLoaded();

        // Mobs killed
        String mobsKilledDisplay;
        int mobsKilledColor;
        if (statsLoaded) {
            int mobsKilled = PlayerStats.getMobsKilled();
            mobsKilledDisplay = String.valueOf(mobsKilled);
            mobsKilledColor = COLOR_PALETTE.DANGER.rgb();
        } else {
            mobsKilledDisplay = "Loading...";
            mobsKilledColor = COLOR_PALETTE.MUTED.rgb();
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
            deathsColor = COLOR_PALETTE.DANGER.rgb();
        } else {
            deathsDisplay = "Loading...";
            deathsColor = COLOR_PALETTE.MUTED.rgb();
        }
        drawStatLine(graphics, contentX, statsY, Component.translatable("gui.nichirin.home.deaths"),
                deathsDisplay, deathsColor, contentWidth, font);

        // If stats aren't loaded, show a loading indicator
        if (!statsLoaded) {
            statsY += statLineHeight + 5;
            Component loadingText = Component.literal("Loading stats...")
                    .withStyle(style -> style.withColor(COLOR_PALETTE.MUTED.rgb()).withItalic(true));
            graphics.drawString(font, loadingText,
                    centerX - font.width(loadingText) / 2, statsY, COLOR_PALETTE.MUTED.rgb());
        }

        // Instructions at bottom
        int bottomY = contentHeight - WORKSPACE_FOOTER_H - 18;
        Component instructions = Component.translatable("gui.nichirin.home.instructions")
                .withStyle(style -> style.withColor(COLOR_PALETTE.MUTED.rgb()).withItalic(true));
        graphics.drawString(font, instructions,
                (contentWidth - font.width(instructions)) / 2,
                bottomY, COLOR_PALETTE.MUTED.rgb());
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
        graphics.drawString(font, labelText, x, y, COLOR_PALETTE.GRAY.rgb());

        // Draw value (right-aligned within content area)
        int valueX = contentWidth - 30 - font.width(value);
        graphics.drawString(font, value, valueX, y, valueColor);

        // Draw connecting dots
        String dots = ".".repeat(Math.max(1, (valueX - x - font.width(labelText) - 5) / 4));
        graphics.drawString(font, dots, x + font.width(labelText), y, COLOR_PALETTE.DOTS.rgb());
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

    private String currentPath(MovesetData data) {
        boolean breathing = data.hasBreathingMoveset();
        boolean demon = data.hasDemonMoveset();
        if (breathing && demon) return "Hybrid";
        if (demon) return "Demon";
        if (breathing) return "Slayer";
        return "Unassigned";
    }

    private int pathColor(MovesetData data) {
        boolean breathing = data.hasBreathingMoveset();
        boolean demon = data.hasDemonMoveset();
        if (breathing && demon) return COLOR_PALETTE.ACCENT.rgb();
        if (demon) return COLOR_PALETTE.DANGER.rgb();
        if (breathing) return COLOR_PALETTE.BREATH_CYAN.rgb();
        return COLOR_PALETTE.TEXT_DIM.rgb();
    }

    private int countRegistered(String token) {
        int count = 0;
        for (String id : MovesetRegistry.getAllMovesetIds()) {
            if (id.contains(token)) count++;
        }
        return count;
    }
}
