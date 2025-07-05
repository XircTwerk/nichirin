package com.xirc.nichirin.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Helper methods for THE BIG GUI
 */
public class BigGuiHelper {

    /**
     * Opens THE BIG GUI
     */
    public static void openGui() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new TheBigGui(mc.player));
        }
    }

    /**
     * Opens THE BIG GUI to a specific section
     */
    public static void openGui(TheBigGui.GuiSection section) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            TheBigGui gui = new TheBigGui(mc.player);
            mc.setScreen(gui);
            // TODO: Add method to switch to specific section after init
        }
    }

    /**
     * Checks if THE BIG GUI is currently open
     */
    public static boolean isGuiOpen() {
        return Minecraft.getInstance().screen instanceof TheBigGui;
    }

    /**
     * Gets the current GUI instance if open
     */
    public static TheBigGui getCurrentGui() {
        if (isGuiOpen()) {
            return (TheBigGui) Minecraft.getInstance().screen;
        }
        return null;
    }
}