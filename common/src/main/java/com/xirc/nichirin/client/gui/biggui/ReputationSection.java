package com.xirc.nichirin.client.gui.biggui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Reputation section - coming soon
 */
public class ReputationSection extends AbstractGuiPage {

    private static final int TOP_MARGIN = 20;

    public void render(GuiGraphics graphics, Player player, Font font, int contentWidth, int contentHeight, int mouseX, int mouseY) {
        drawWorkspaceChrome(graphics, font, contentWidth, contentHeight,
                "Reputation", "Standing and faction trust will live here once that data exists.",
                "no ledger", "No reputation data yet", "neutral");
        int x = DEFAULT_PAD;
        int y = workspaceBodyY() + DEFAULT_PAD;
        drawInfoCard(graphics, font, x, y, contentWidth - DEFAULT_PAD * 2, 58,
                "No reputation ledger",
                "There is no reputation system feeding this page yet. No made-up rank, no fake standing.",
                COLOR_PALETTE.BREATH_CYAN.argb());
    }

    public void render(GuiGraphics graphics, Player player, Font font) {
        render(graphics, player, font, 320, 200, -1, -1);
    }

    public boolean handleClick(double mouseX, double mouseY, Player player) {
        return false;
    }
}
