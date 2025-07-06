package com.xirc.nichirin.client.registry;

import com.mojang.blaze3d.platform.InputConstants;
import com.xirc.nichirin.client.handler.AttackWheelHandler;
import com.xirc.nichirin.client.handler.BigGuiKeyHandler;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Centralized registry for all keybindings
 */
public class NichirinKeybindRegistry {

    // Define all keybindings
    public static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
            "key.nichirin.open_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.nichirin"
    );

    public static final KeyMapping ATTACK_WHEEL_KEY = new KeyMapping(
            "key.nichirin.attack_wheel",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.nichirin"
    );

    public static void init() {
        // Register all keybindings
        KeyMappingRegistry.register(OPEN_GUI_KEY);
        KeyMappingRegistry.register(ATTACK_WHEEL_KEY);

        System.out.println("DEBUG: Registered keybindings - GUI: " + OPEN_GUI_KEY.getName() + ", Wheel: " + ATTACK_WHEEL_KEY.getName());
    }
}