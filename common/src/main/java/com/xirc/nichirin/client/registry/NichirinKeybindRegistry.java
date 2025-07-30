package com.xirc.nichirin.client.registry;

import com.mojang.blaze3d.platform.InputConstants;
import com.xirc.nichirin.common.network.MovementInputPacket;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Extended keybind registry with movement system
 */
@Environment(EnvType.CLIENT)
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

    KeyMapping MOVEMENT_KEY = new KeyMapping(
            "key.nichirin.movement",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_5,
            "key.categories.nichirin"
    );

    static void register() {
        KeyMappingRegistry.register(ATTACK_WHEEL_KEY);
        KeyMappingRegistry.register(OPEN_GUI_KEY);
        KeyMappingRegistry.register(BLOCK_KEY);
        KeyMappingRegistry.register(MOVEMENT_KEY);

        // Register tick event for movement input - ARCHITECTURY WAY
        ClientTickEvent.CLIENT_POST.register(NichirinKeybindRegistry::onClientTick);
    }

    /**
     * Handle client tick for key input
     */
    private static void onClientTick(Minecraft client) {
        if (client.player == null) return;

        // Check if movement key was pressed
        while (MOVEMENT_KEY.consumeClick()) {
            handleMovementKeyPress();
        }
    }

    /**
     * Handle movement key press
     */
    private static void handleMovementKeyPress() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // Send movement input packet to server
        MovementInputPacket packet = new MovementInputPacket();
        NichirinPacketRegistry.sendToServer(packet);

        System.out.println("DEBUG: Movement key (MB5) pressed, packet sent to server");
    }

    /**
     * Get the movement key mapping (for UI display, etc.)
     */
    static KeyMapping getMovementKey() {
        return MOVEMENT_KEY;
    }
}