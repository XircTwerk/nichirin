package com.xirc.nichirin.client.registry;

import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public interface NichirinKeybindRegistry {

    KeyMapping ATTACK_WHEEL_KEY = new KeyMapping(
            "key.nichirin.attack_wheel",
            GLFW.GLFW_KEY_R,
            "key.categories.nichirin"
    );

    KeyMapping OPEN_GUI_KEY = new KeyMapping(
            "key.nichirin.open_gui",
            GLFW.GLFW_KEY_G,
            "key.categories.nichirin"
    );

    KeyMapping BLOCK_KEY = new KeyMapping(
            "key.nichirin.block",
            GLFW.GLFW_KEY_V,
            "key.categories.nichirin"
    );

    static void register() {
        KeyMappingRegistry.register(ATTACK_WHEEL_KEY);
        KeyMappingRegistry.register(OPEN_GUI_KEY);
        KeyMappingRegistry.register(BLOCK_KEY);
    }
}