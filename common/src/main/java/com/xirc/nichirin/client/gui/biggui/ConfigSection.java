package com.xirc.nichirin.client.gui.biggui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Config section - coming soon
 */
public class ConfigSection {

    private static final int TOP_MARGIN = 20;

    public void render(GuiGraphics graphics, Player player, Font font) {
        Component comingSoon = Component.translatable("gui.nichirin.coming_soon");
        graphics.drawString(font, comingSoon, 20, TOP_MARGIN + 10, 0xFFFFFF);
    }

    public boolean handleClick(double mouseX, double mouseY, Player player) {
        return false;
    }
}