package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.client.gui.TheBigGui;
import com.xirc.nichirin.client.registry.NichirinKeybindRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;

/**
 * Handles the keybind for opening THE BIG GUI
 */
public class BigGuiKeyHandler {

    public static void register() {
        // Register tick handler
        ClientTickEvent.CLIENT_PRE.register(client -> {
            if (NichirinKeybindRegistry.OPEN_GUI_KEY.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    Minecraft.getInstance().setScreen(new TheBigGui(client.player));
                }
            }
        });
    }
}