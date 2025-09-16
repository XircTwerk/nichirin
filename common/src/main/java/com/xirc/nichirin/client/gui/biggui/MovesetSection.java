package com.xirc.nichirin.client.gui.biggui;

import com.xirc.nichirin.common.data.MovesetHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Moveset section - shows current breathing style moves and details
 */
public class MovesetSection {

    private static final int TOP_MARGIN = 20;

    public void render(GuiGraphics graphics, Player player, Font font) {
        int contentX = 20;
        int contentY = TOP_MARGIN + 10;

        Component title = Component.translatable("gui.nichirin.moveset.title");
        graphics.drawString(font, title, contentX, contentY, 0xFFFFFF);
        contentY += 20;

        // Show current breathing style moveset
        String currentStyle = MovesetHelper.getMovesetId(player);
        if (currentStyle != null) {
            Component styleLabel = Component.translatable("gui.nichirin.moveset.current_style",
                    Component.translatable("breathing_style." + currentStyle));
            graphics.drawString(font, styleLabel, contentX, contentY, 0x55FFFF);
            contentY += 15;

            Component moveDetails = Component.translatable("gui.nichirin.moveset.move_details_coming_soon");
            graphics.drawString(font, moveDetails, contentX, contentY, 0xAAAAAA);
        } else {
            Component selectStyle = Component.translatable("gui.nichirin.moveset.select_style");
            graphics.drawString(font, selectStyle, contentX, contentY, 0xAAAAAA);
        }
    }

    public boolean handleClick(double mouseX, double mouseY, Player player) {
        // No click handling needed for moveset section yet
        return false;
    }
}