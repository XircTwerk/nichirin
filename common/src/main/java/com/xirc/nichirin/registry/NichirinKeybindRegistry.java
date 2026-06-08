package com.xirc.nichirin.registry;

import com.mojang.blaze3d.platform.InputConstants;
import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.attack.moveset.CqcMoveset;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.network.c2s.MovementInputPacket;
import com.xirc.nichirin.common.network.c2s.MoveHotkeyPacket;
import com.xirc.nichirin.common.util.BreathingManager;
import com.xirc.nichirin.common.util.StaminaManager;
import com.xirc.nichirin.common.util.MultiplayerInputHandler;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Extended keybind registry with movement system and move index hotkeys
 * Attack wheel key is now handled by AttackWheelHandler, not here
 */
@Environment(EnvType.CLIENT)
public interface NichirinKeybindRegistry {

    KeyMapping ATTACK_WHEEL_KEY = new KeyMapping(
            "key.nichirin.attack_wheel",
            GLFW.GLFW_KEY_R,
            "key.categories.nichirin"
    );

    KeyMapping SHEATHE_KEY = new KeyMapping(
            "key.nichirin.sheathe",
            GLFW.GLFW_KEY_Y,
            "key.categories.nichirin"
    );

    KeyMapping OPEN_GUI_KEY = new KeyMapping(
            "key.nichirin.open_gui",
            GLFW.GLFW_KEY_G,
            "key.categories.nichirin"
    );

    KeyMapping CQC_KEY = new KeyMapping(
            "key.nichirin.cqc",
            GLFW.GLFW_KEY_BACKSLASH,
            "key.categories.nichirin"
    );

    // Blocking moved to right-click (hold) — handled in BlockingInputHandler. No dedicated key.

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

    AtomicBoolean MOVE_HOTKEYS_REGISTERED = new AtomicBoolean(false);
    AtomicBoolean CLIENT_TICK_REGISTERED = new AtomicBoolean(false);

    static void register() {
        registerKeyMappings(KeyMappingRegistry::register);
        registerClientTickHandler();
    }

    static void registerKeyMappings(Consumer<KeyMapping> registrar) {
        registrar.accept(ATTACK_WHEEL_KEY);
        registrar.accept(SHEATHE_KEY);
        registrar.accept(OPEN_GUI_KEY);
        registrar.accept(CQC_KEY);
        registrar.accept(MOVEMENT_KEY);

        // Register move index hotkeys
        registerMoveHotkeys(registrar);
    }

    static void registerClientTickHandler() {
        // Register tick event for input handling - ARCHITECTURY WAY
        // NOTE: Attack wheel is handled by AttackWheelHandler, not here
        if (CLIENT_TICK_REGISTERED.compareAndSet(false, true)) {
            ClientTickEvent.CLIENT_POST.register(NichirinKeybindRegistry::onClientTick);
        }
    }

    /**
     * Register hotkeys for move indexes 0-11 (supports up to 12 moves per moveset)
     * All default to NOT_BOUND and are in "Nichirin Hotkeys" category
     */
    private static void registerMoveHotkeys(Consumer<KeyMapping> registrar) {
        if (!MOVE_HOTKEYS_REGISTERED.compareAndSet(false, true)) {
            MOVE_HOTKEYS.values().forEach(registrar);
            return;
        }

        for (int i = 0; i < MAX_MOVE_SLOTS; i++) {
            KeyMapping moveHotkey = new KeyMapping(
                    "key.nichirin.move_" + i,
                    InputConstants.UNKNOWN.getValue(), // NOT_BOUND by default
                    "key.categories.nichirin_hotkeys"
            );

            MOVE_HOTKEYS.put(i, moveHotkey);
            registrar.accept(moveHotkey);
        }
    }

    /**
     * Handle client tick for key input
     * NOTE: Attack wheel R key is handled by AttackWheelHandler
     */
    private static void onClientTick(Minecraft client) {
        if (client.player == null) return;

        // Check if movement key was pressed
        while (MOVEMENT_KEY.consumeClick()) {
            handleMovementKeyPress();
        }

        while (CQC_KEY.consumeClick()) {
            handleCqcKeyPress();
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

    private static void handleCqcKeyPress() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        NichirinPacketRegistry.requestMovesetChange("cqc");
    }

    /**
     * Handle move hotkey press - SAME LOGIC AS ATTACK WHEEL
     */
    private static void handleMoveHotkeyPress(int moveIndex) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // Same checks as attack wheel
        if (client.player.hasEffect(NichirinEffectRegistry.stunned())) {
            return;
        }

        // Check if player has blocking effect - CAN'T USE HOTKEYS
        if (client.player.hasEffect(NichirinEffectRegistry.blocking())) {
            return;
        }

        // SAME LOGIC AS ATTACK WHEEL: Determine moveset based on what player has, not what they're holding
        AbstractMoveset moveset = null;
        boolean isBreathingMove = false;

        // Check held item
        ItemStack mainHand = client.player.getMainHandItem();
        boolean holdingKatana = mainHand.getItem() instanceof SimpleKatana;

        if (holdingKatana) {
            // Holding katana - use breathing moveset if available
            if (MovesetHelper.hasBreathingMoveset(client.player)) {
                moveset = MovesetHelper.getBreathingMoveset(client.player);
                isBreathingMove = true;
            }
        } else {
            // Empty hand fighting styles take priority over demon arts.
            if (mainHand.isEmpty() && MovesetHelper.hasFightingMoveset(client.player)) {
                moveset = MovesetHelper.getFightingMoveset(client.player);
                isBreathingMove = false;
            } else if (MovesetHelper.hasDemonMoveset(client.player)) {
                moveset = MovesetHelper.getDemonMoveset(client.player);
                isBreathingMove = false;
            }
        }

        // Default katana case: holding katana with no breathing style assigned
        // Route through MoveHotkeyPacket so SimpleKatana.performWheelMove handles it server-side
        if (moveset == null) {
            if (holdingKatana && moveIndex <= 2) {
                NichirinPacketRegistry.sendToServer(new MoveHotkeyPacket(moveIndex));
            }
            return;
        }

        // Check if move exists in the moveset
        final AbstractMoveset selectedMoveset = moveset;
        final AbstractMoveset.MoveConfiguration[] moveConfigHolder = new AbstractMoveset.MoveConfiguration[1];
        Runnable resolveMove = () -> moveConfigHolder[0] = selectedMoveset.getMove(moveIndex);
        if (selectedMoveset instanceof CqcMoveset) {
            CqcMoveset.withPlayer(client.player, resolveMove);
        } else {
            resolveMove.run();
        }
        var moveConfig = moveConfigHolder[0];
        if (moveConfig == null) {
            return; // Move doesn't exist - hotkey does nothing silently
        }

        // Check cooldown before sending packet (SAME AS ATTACK WHEEL)
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

        // Only check resource costs for breathing arts (demon arts don't use breath/stamina)
        if (isBreathingMove) {
            if (moveConfig.hasStaminaCost() && !StaminaManager.hasStamina(client.player, moveConfig.getStaminaCost())) {
                client.player.displayClientMessage(
                        Component.literal("Not enough stamina!")
                                .withStyle(style -> style.withColor(0xFF5555)),
                        true
                );
                return;
            }

            if (moveConfig.hasBreathCost() && !BreathingManager.hasBreath(client.player, moveConfig.getBreathCost())) {
                client.player.displayClientMessage(
                        Component.literal("Not enough breath!")
                                .withStyle(style -> style.withColor(0xFF5555)),
                        true
                );
                return;
            }
        }

        // Send move to server using appropriate packet type (SAME AS ATTACK WHEEL)
        if (isBreathingMove) {
            MultiplayerInputHandler.sendBreathingMove(moveIndex, client.player);
        } else {
            MultiplayerInputHandler.sendDemonMove(moveIndex, client.player);
        }
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
