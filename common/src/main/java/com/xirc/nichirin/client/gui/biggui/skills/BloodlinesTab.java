package com.xirc.nichirin.client.gui.biggui.skills;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Bloodlines / Traits sub-tab placeholder — coming soon. */
public class BloodlinesTab {

    public void render(GuiGraphics graphics, Player player, Font font, int contentWidth, int contentHeight, int mouseX, int mouseY) {
        graphics.drawString(font, Component.literal("Bloodlines — Coming Soon").withStyle(style -> style.withColor(0xAAAAAA)), 20, 30, 0xFFFFFF, false);
        graphics.drawString(font, Component.literal("Rare bloodline traits and passive bonuses will appear here.").withStyle(style -> style.withColor(0x666666)), 20, 50, 0xFFFFFF, false);
    }

    public boolean handleClick(double mouseX, double mouseY, Player player) {
        return false;
    }
}
