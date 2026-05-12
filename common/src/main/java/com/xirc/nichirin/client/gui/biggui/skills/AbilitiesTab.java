package com.xirc.nichirin.client.gui.biggui.skills;

import com.xirc.nichirin.client.gui.biggui.AbstractGuiPage;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/** Abilities sub-tab placeholder (Slayer Mark, Red Blade, etc.) — coming soon. */
public class AbilitiesTab extends AbstractGuiPage {

    public void render(GuiGraphics graphics, Player player, Font font, int contentWidth, int contentHeight, int mouseX, int mouseY) {
        int cx = contentWidth / 2;
        int cy = contentHeight / 2 - 14;
        String title = "Abilities";
        String body = "Slayer Mark, Red Blade, and awakened techniques will appear here.";
        graphics.drawString(font, title, cx - font.width(title) / 2, cy, COLOR_PALETTE.TEXT.rgb(), false);
        graphics.drawString(font, body, cx - font.width(body) / 2, cy + 16, COLOR_PALETTE.TEXT_DIM.rgb(), false);
    }

    public boolean handleClick(double mouseX, double mouseY, Player player) {
        return false;
    }
}
