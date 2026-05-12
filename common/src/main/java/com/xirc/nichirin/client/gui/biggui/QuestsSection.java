package com.xirc.nichirin.client.gui.biggui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Quests section - coming soon
 */
public class QuestsSection extends AbstractGuiPage {

    private static final int TOP_MARGIN = 20;

    public void render(GuiGraphics graphics, Player player, Font font, int contentWidth, int contentHeight, int mouseX, int mouseY) {
        drawWorkspaceChrome(graphics, font, contentWidth, contentHeight,
                "Quests", "Tracked objectives will live here once quest data exists.",
                "no tracker", "No quest data yet", "empty");
        int x = DEFAULT_PAD;
        int y = workspaceBodyY() + DEFAULT_PAD;
        drawInfoCard(graphics, font, x, y, contentWidth - DEFAULT_PAD * 2, 58,
                "No quest tracker",
                "There is no quest source wired into this screen yet, so this page stays honest.",
                COLOR_PALETTE.GREEN.argb());
    }

    public void render(GuiGraphics graphics, Player player, Font font) {
        render(graphics, player, font, 320, 200, -1, -1);
    }

    public boolean handleClick(double mouseX, double mouseY, Player player) {
        return false;
    }
}
