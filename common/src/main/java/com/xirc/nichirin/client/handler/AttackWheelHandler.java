package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.client.gui.AttackWheelOverlay;
import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.attack.moveset.DefaultKatanaMoveset;
import com.xirc.nichirin.common.network.c2s.MoveHotkeyPacket;
import com.xirc.nichirin.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.util.BreathingManager;
import com.xirc.nichirin.common.util.StaminaManager;
import com.xirc.nichirin.common.util.MultiplayerInputHandler;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientGuiEvent;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * DEBUG VERSION: Attack wheel handler with extensive logging
 */
public class AttackWheelHandler {

    private static boolean wasKeyDown = false;
    private static boolean wasEscDown = false;
    private static final boolean[] wasNumberKeyDown = new boolean[9];
    private static AttackWheelOverlay currentWheel = null;
    @Getter
    private static boolean wheelOpen = false;
    private static boolean wasAttackDown = false;

    @Getter
    private static int capturedSelectedMove = -1;
    private static int lastHoveredMove = -1;

    // New blocking system
    private static long wheelClosedTime = 0;
    private static final long BLOCK_DURATION_TICKS = 40; // 2 seconds at 20 TPS

    // Track which moveset type the wheel is currently showing
    private static boolean currentWheelIsBreathing = false;
    // True when showing default katana moves (no breathing style) — send MoveHotkeyPacket on execute
    private static boolean isDefaultKatanaWheel = false;

    public static void register() {

        // Create the overlay instance
        currentWheel = new AttackWheelOverlay();

        // Register wheel toggle with ESC handling
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (client.player == null) return;

            boolean isKeyDown = NichirinKeybindRegistry.ATTACK_WHEEL_KEY.isDown();
            boolean isEscDown = org.lwjgl.glfw.GLFW.glfwGetKey(client.getWindow().getWindow(), GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;

            // Debug key state changes
            if (isKeyDown != wasKeyDown) {
            }

            // Handle ESC when wheel is open - more aggressive for multiplayer
            if (wheelOpen && isEscDown && !wasEscDown) {
                closeWheel();

                // Check immediately and for the next few ticks
                for (int i = 0; i < 3; i++) {
                    if (client.screen != null && client.screen.getClass().getSimpleName().contains("Pause")) {
                        client.setScreen(null);
                        client.mouseHandler.grabMouse();
                    }
                }
            }

            // Handle attack wheel key
            if (isKeyDown && !wasKeyDown) {
                if (!wheelOpen) {
                    openWheel();
                } else {
                    closeWheel();
                }
            }

            if (wheelOpen && currentWheel != null) {
                int currentHovered = currentWheel.getCurrentlyHoveredMove();
                if (currentHovered != lastHoveredMove) {
                    // Only log when move actually changes
                    if (currentHovered >= 0) {
                        capturedSelectedMove = currentHovered;
                    }
                    lastHoveredMove = currentHovered;
                }
            }

            wasKeyDown = isKeyDown;
            wasEscDown = isEscDown;
        });

        // Monitor and prevent pause screens during wheel operation - enhanced for multiplayer
        ClientTickEvent.CLIENT_PRE.register(client -> {
            if (wheelOpen && client.screen != null) {
                String screenName = client.screen.getClass().getSimpleName();
                if (screenName.contains("Pause")) {
                    client.setScreen(null);
                    client.mouseHandler.grabMouse();
                }
            }

            // Consume hotbar key clicks BEFORE Minecraft.handleKeybinds() so slots don't switch
            if (wheelOpen) {
                for (int i = 0; i < 9; i++) {
                    while (client.options.keyHotbarSlots[i].consumeClick()) {}
                }
            }
        });

        // Additional multiplayer-specific check in POST tick
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (wheelOpen && client.screen != null) {
                String screenName = client.screen.getClass().getSimpleName();
                if (screenName.contains("Pause")) {
                    client.setScreen(null);
                    client.mouseHandler.grabMouse();
                }
            }
        });

        // Register HUD rendering
        ClientGuiEvent.RENDER_HUD.register((guiGraphics, partialTick) -> {
            Minecraft mc = Minecraft.getInstance();

            // Close wheel if screen opens
            if (wheelOpen && mc.screen != null) {
                closeWheel();
                return;
            }

            if (currentWheel != null && currentWheel.isActive()) {
                currentWheel.render(guiGraphics);
            }
        });

        // Handle clicks while wheel is open - IMMEDIATE EXECUTION AND CLOSE
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (!wheelOpen || client.player == null) {
                wasAttackDown = false;
                return;
            }

            boolean isAttackDown = client.options.keyAttack.isDown();

            while (client.options.keyAttack.consumeClick()) {
            }
            while (client.options.keyUse.consumeClick()) {
            }

            // IMMEDIATE EXECUTION: Execute and close on first click detection
            if (isAttackDown && !wasAttackDown) {
                executeWheelMove(); // This closes the wheel immediately
            }

            // Keys 1-9 execute moves by index (check raw GLFW state to bypass Minecraft's input handling)
            long window = client.getWindow().getWindow();
            for (int i = 0; i < 9; i++) {
                boolean isDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_1 + i) == GLFW.GLFW_PRESS;
                if (isDown && !wasNumberKeyDown[i]) {
                    wasNumberKeyDown[i] = true;
                    executeWheelMoveByIndex(i);
                    return;
                }
                wasNumberKeyDown[i] = isDown;
            }
            while (client.options.keyDrop.consumeClick()) {
                // Consume drop key
            }
            while (client.options.keyInventory.consumeClick()) {
                // Consume inventory key
            }

            wasAttackDown = isAttackDown;
        });

    }

    /**
     * Open the attack wheel - properly uses demon movesets for demons
     */
    private static void openWheel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        if (mc.player.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return;
        }

        // Check if player has blocking effect - CAN'T OPEN WHEEL
        if (mc.player.hasEffect(NichirinEffectRegistry.BLOCKING.get())) {
            return;
        }

        AbstractMoveset moveset = null;
        boolean isBreathingWheel = false;

        // Check held item
        ItemStack mainHand = mc.player.getMainHandItem();
        boolean holdingKatana = mainHand.getItem() instanceof SimpleKatana;

        if (holdingKatana) {
            // Holding katana - use breathing moveset if available, otherwise default katana
            boolean hasBreathing = MovesetHelper.hasBreathingMoveset(mc.player);
            if (hasBreathing) {
                moveset = MovesetHelper.getBreathingMoveset(mc.player);
                isBreathingWheel = true;
            } else {
                // No breathing style — show the 3 default wheel moves (Check, Overhead, Thrust)
                moveset = DefaultKatanaMoveset.INSTANCE;
                isBreathingWheel = false;
                isDefaultKatanaWheel = true;
            }
        } else {
            // Not holding katana - use demon moveset if available
            boolean hasDemon = MovesetHelper.hasDemonMoveset(mc.player);
            if (hasDemon) {
                moveset = MovesetHelper.getDemonMoveset(mc.player);
                isBreathingWheel = false;
            }
        }

        // Must have appropriate moveset to open wheel
        if (moveset == null) {
            return;
        }

        // Don't open if screen is open
        if (mc.screen != null) {
            return;
        }

        if (currentWheel != null) {
            mc.mouseHandler.releaseMouse();
            currentWheel.activate();
            wheelOpen = true;
            currentWheelIsBreathing = isBreathingWheel; // Track which type

            // Reset captured move state
            capturedSelectedMove = -1;
            lastHoveredMove = -1;

            // Update state (handles server sync)
            MultiplayerInputHandler.setAttackWheelOpen(true, mc.player);

            wasAttackDown = false;
        }
    }

    /**
     * Close the attack wheel and start blocking timer
     */
    private static void closeWheel() {
        Minecraft mc = Minecraft.getInstance();

        if (currentWheel != null) {
            currentWheel.deactivate();
        }
        wheelOpen = false;
        currentWheelIsBreathing = false; // Reset
        isDefaultKatanaWheel = false;    // Reset

        // Reset captured move state
        capturedSelectedMove = -1;
        lastHoveredMove = -1;
        java.util.Arrays.fill(wasNumberKeyDown, false);

        // START BLOCKING TIMER - Record when wheel was closed
        if (mc.player != null) {
            wheelClosedTime = mc.player.level().getGameTime();
        }

        // Update state (handles server sync)
        if (mc.player != null) {
            MultiplayerInputHandler.setAttackWheelOpen(false, mc.player);
        }

        wasAttackDown = false;

        // Recapture mouse
        if (mc.screen == null) {
            mc.mouseHandler.grabMouse();
        }
    }

    /** Execute a wheel move by explicit index (from hotkeys 1-9). */
    private static void executeWheelMoveByIndex(int index) {
        capturedSelectedMove = index;
        executeWheelMove();
    }

    /**
     * Execute the selected wheel move - uses the correct moveset type
     */
    private static void executeWheelMove() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || currentWheel == null) {
            return;
        }

        // USE CAPTURED MOVE instead of real-time calculation
        int selectedMove = capturedSelectedMove;

        if (selectedMove == -1) {
            closeWheel();
            return;
        }

        AbstractMoveset moveset = null;

        if (currentWheelIsBreathing) {
            moveset = MovesetHelper.getBreathingMoveset(mc.player);
        } else if (isDefaultKatanaWheel) {
            // Default katana — use the static display moveset for config lookup only
            moveset = DefaultKatanaMoveset.INSTANCE;
        } else {
            moveset = MovesetHelper.getDemonMoveset(mc.player);
        }

        if (moveset == null) {
            closeWheel();
            return;
        }

        var moveConfig = moveset.getMove(selectedMove);
        if (moveConfig == null) {
            closeWheel();
            return;
        }

        // Check requirements client-side (for immediate feedback)
        String moveName = moveConfig.getDisplayName();

        if (CooldownHUD.isOnCooldown(moveName)) {
            int remaining = CooldownHUD.getRemainingCooldown(moveName);
            mc.player.displayClientMessage(
                    Component.literal("Move on cooldown! " + (remaining / 20.0f) + "s remaining")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            closeWheel();
            return;
        }

        // Only check resource costs for breathing arts (demon arts don't use breath/stamina)
        if (currentWheelIsBreathing) {
            if (moveConfig.hasStaminaCost() && !StaminaManager.hasStamina(mc.player, moveConfig.getStaminaCost())) {
                mc.player.displayClientMessage(
                        Component.literal("Not enough stamina!")
                                .withStyle(style -> style.withColor(0xFF5555)),
                        true
                );
                closeWheel();
                return;
            }

            if (moveConfig.hasBreathCost() && !BreathingManager.hasBreath(mc.player, moveConfig.getBreathCost())) {
                mc.player.displayClientMessage(
                        Component.literal("Not enough breath!")
                                .withStyle(style -> style.withColor(0xFF5555)),
                        true
                );
                closeWheel();
                return;
            }
        }

        // Send move to server using appropriate packet type
        if (currentWheelIsBreathing) {
            MultiplayerInputHandler.sendBreathingMove(selectedMove, mc.player);
        } else if (isDefaultKatanaWheel) {
            // Default katana wheel — server handles via SimpleKatana.performWheelMove
            NichirinPacketRegistry.sendToServer(new MoveHotkeyPacket(selectedMove));
            // Mirror cooldown on the client for HUD display
            if (moveConfig.hasCooldown()) {
                CooldownHUD.setCooldown(moveName, moveConfig.getCooldown());
            }
        } else {
            MultiplayerInputHandler.sendDemonMove(selectedMove, mc.player);
        }

        // Close wheel after sending command
        closeWheel();
    }

    /**
     * NEW METHOD: Check if inputs should be blocked due to wheel state
     * This includes:
     * 1. When wheel is currently open
     * 2. For 2 seconds after wheel closes
     */
    public static boolean shouldBlockAttackInputs() {
        Minecraft mc = Minecraft.getInstance();

        // Block if wheel is currently open
        if (wheelOpen) {
            return true;
        }

        // Block for 2 seconds after wheel closes
        if (wheelClosedTime > 0 && mc.player != null) {
            long currentTime = mc.player.level().getGameTime();
            long timeSinceClosed = currentTime - wheelClosedTime;

            if (timeSinceClosed < BLOCK_DURATION_TICKS) {
                // Still within blocking period
                return true;
            } else if (timeSinceClosed >= BLOCK_DURATION_TICKS) {
                // Blocking period over, reset the timer
                wheelClosedTime = 0;
                return false;
            }
        }

        return false;
    }

    /**
     * Get remaining block time in ticks (for UI feedback)
     */
    public static int getRemainingBlockTicks() {
        if (wheelOpen) {
            return -1; // Indefinite while wheel is open
        }

        if (wheelClosedTime > 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                long currentTime = mc.player.level().getGameTime();
                long timeSinceClosed = currentTime - wheelClosedTime;
                long remaining = BLOCK_DURATION_TICKS - timeSinceClosed;
                return Math.max(0, (int) remaining);
            }
        }

        return 0;
    }

    /**
     * ENHANCED: Use the new blocking system instead of the old method
     */
    public static boolean shouldBlockKatanaAttacks() {
        // Use the new comprehensive blocking system
        boolean shouldBlock = shouldBlockAttackInputs() || MultiplayerInputHandler.shouldBlockInputsClient();
        return shouldBlock;
    }
}