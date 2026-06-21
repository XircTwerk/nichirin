package com.xirc.nichirin.client.gui.biggui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/**
 * Reputation section - coming soon
 */
public class ReputationSection extends AbstractGuiPage {

    private static final int TOP_MARGIN = 20;

    public void render(GuiGraphics graphics, Player player, Font font, int contentWidth, int contentHeight, int mouseX, int mouseY) {
        drawWorkspaceChrome(graphics, font, contentWidth, contentHeight,
                "Factions", "Faction trust will live here once that data exists.",
                "no ledger", "No faction data yet", null);
        int x = DEFAULT_PAD;
        int y = workspaceBodyY() + DEFAULT_PAD;
        drawInfoCard(graphics, font, x, y, contentWidth - DEFAULT_PAD * 2, 58,
                "No faction ledger",
                "There is no faction system feeding this page yet.",
                COLOR_PALETTE.BREATH_CYAN.argb());
    }

    public void render(GuiGraphics graphics, Player player, Font font) {
        render(graphics, player, font, 320, 200, -1, -1);
    }

    public boolean handleClick(double mouseX, double mouseY, Player player) {
        return false;
    }
}