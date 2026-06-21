package com.xirc.nichirin.client.gui.biggui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/**
 * Bestiary section - coming soon
 */
public class BestiarySection extends AbstractGuiPage {

    private static final int TOP_MARGIN = 20;

    public void render(GuiGraphics graphics, Player player, Font font, int contentWidth, int contentHeight, int mouseX, int mouseY) {
        drawWorkspaceChrome(graphics, font, contentWidth, contentHeight,
                "Bestiary", "Encounter records will live here once that data exists.",
                "no data", "No discovery data yet", "empty");
        int x = DEFAULT_PAD;
        int y = workspaceBodyY() + DEFAULT_PAD;
        drawInfoCard(graphics, font, x, y, contentWidth - DEFAULT_PAD * 2, 58,
                "No encounter data",
                "This page is waiting on a real bestiary data source. It will not invent entries.",
                COLOR_PALETTE.ACCENT.argb());
    }

    public void render(GuiGraphics graphics, Player player, Font font) {
        render(graphics, player, font, 320, 200, -1, -1);
    }

    public boolean handleClick(double mouseX, double mouseY, Player player) {
        return false;
    }
}