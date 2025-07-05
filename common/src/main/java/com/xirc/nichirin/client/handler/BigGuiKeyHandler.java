package com.xirc.nichirin.client.handler;

import com.mojang.blaze3d.platform.InputConstants;
import com.xirc.nichirin.client.gui.TheBigGui;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Handles the keybind for opening THE BIG GUI
 */
public class BigGuiKeyHandler {

    public static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
            "key.nichirin.open_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G, // Default to G key
            "key.categories.nichirin"
    );

    public static void register() {
        // Register the keybinding
        KeyMappingRegistry.register(OPEN_GUI_KEY);

        // Register tick handler
        ClientTickEvent.CLIENT_PRE.register(client -> {
            if (OPEN_GUI_KEY.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    Minecraft.getInstance().setScreen(new TheBigGui(client.player));
                }
            }
        });
    }
}