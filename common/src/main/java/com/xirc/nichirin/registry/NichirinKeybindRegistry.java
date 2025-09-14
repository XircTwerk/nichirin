package com.xirc.nichirin.registry;

import com.mojang.blaze3d.platform.InputConstants;
import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.network.c2s.MovementInputPacket;
import com.xirc.nichirin.common.network.c2s.MoveHotkeyPacket;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * Extended keybind registry with movement system and move index hotkeys
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

    // Dynamic move index hotkeys - will be populated during registration
    Map<Integer, KeyMapping> MOVE_HOTKEYS = new HashMap<>();

    // Maximum number of move slots to support (covers all movesets)
    int MAX_MOVE_SLOTS = 12;

    static void register() {
        KeyMappingRegistry.register(ATTACK_WHEEL_KEY);
        KeyMappingRegistry.register(OPEN_GUI_KEY);
        KeyMappingRegistry.register(BLOCK_KEY);
        KeyMappingRegistry.register(MOVEMENT_KEY);

        // Register move index hotkeys
        registerMoveHotkeys();

        // Register tick event for input handling - ARCHITECTURY WAY
        ClientTickEvent.CLIENT_POST.register(NichirinKeybindRegistry::onClientTick);
    }

    /**
     * Register hotkeys for move indexes 0-11 (supports up to 12 moves per moveset)
     * All default to NOT_BOUND and are in "Nichirin Hotkeys" category
     */
    private static void registerMoveHotkeys() {
        for (int i = 0; i < MAX_MOVE_SLOTS; i++) {
            KeyMapping moveHotkey = new KeyMapping(
                    "key.nichirin.move_" + i,
                    InputConstants.UNKNOWN.getValue(), // NOT_BOUND by default
                    "key.categories.nichirin_hotkeys"
            );

            MOVE_HOTKEYS.put(i, moveHotkey);
            KeyMappingRegistry.register(moveHotkey);
        }
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

        // Check move hotkeys
        for (Map.Entry<Integer, KeyMapping> entry : MOVE_HOTKEYS.entrySet()) {
            int moveIndex = entry.getKey();
            KeyMapping hotkey = entry.getValue();

            while (hotkey.consumeClick()) {
                handleMoveHotkeyPress(moveIndex);
            }
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
    }

    /**
     * Handle move hotkey press
     */
    private static void handleMoveHotkeyPress(int moveIndex) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // Check if holding katana first (same as attack wheel)
        ItemStack mainHand = client.player.getMainHandItem();
        if (!(mainHand.getItem() instanceof SimpleKatana)) {
            return; // No katana - hotkey does nothing silently
        }

        // Check cooldown before sending packet
        var moveset = BreathingStyleHelper.getMoveset(client.player);
        if (moveset != null) {
            var moveConfig = moveset.getMove(moveIndex);
            if (moveConfig != null) {
                String moveName = moveConfig.getDisplayName();
                if (CooldownHUD.isOnCooldown(moveName)) {
                    int remaining = CooldownHUD.getRemainingCooldown(moveName);
                    client.player.displayClientMessage(
                            Component.literal("Move on cooldown! " + (remaining / 20.0f) + "s remaining")
                                    .withStyle(style -> style.withColor(0xFF5555)),
                            true
                    );
                    return;
                }
            }
        }

        // Send move hotkey packet to server
        MoveHotkeyPacket packet = new MoveHotkeyPacket(moveIndex);
        NichirinPacketRegistry.sendToServer(packet);
    }

    /**
     * Get the movement key mapping (for UI display, etc.)
     */
    static KeyMapping getMovementKey() {
        return MOVEMENT_KEY;
    }

    /**
     * Get a move hotkey by index
     */
    static KeyMapping getMoveHotkey(int index) {
        return MOVE_HOTKEYS.get(index);
    }

    /**
     * Get all move hotkeys
     */
    static Map<Integer, KeyMapping> getAllMoveHotkeys() {
        return new HashMap<>(MOVE_HOTKEYS);
    }
}