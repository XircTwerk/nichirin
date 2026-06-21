package com.xirc.nichirin.client.gui.biggui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/**
 * Config section - coming soon
 */
public class ConfigSection extends AbstractGuiPage {

    private static final int TOP_MARGIN = 20;

    public void render(GuiGraphics graphics, Player player, Font font, int contentWidth, int contentHeight, int mouseX, int mouseY) {
        drawWorkspaceChrome(graphics, font, contentWidth, contentHeight,
                "Config", "Settings open through the config screen.",
                "external screen", "Use the config command for now", "idle");
        int x = DEFAULT_PAD;
        int y = workspaceBodyY() + DEFAULT_PAD;
        drawInfoCard(graphics, font, x, y, contentWidth - DEFAULT_PAD * 2, 58,
                "Config screen",
                "This page does not own config data directly. It points to the real config screen instead.",
                COLOR_PALETTE.ACCENT.argb());
    }

    public void render(GuiGraphics graphics, Player player, Font font) {
        render(graphics, player, font, 320, 200, -1, -1);
    }

    public boolean handleClick(double mouseX, double mouseY, Player player) {
        return false;
    }
}