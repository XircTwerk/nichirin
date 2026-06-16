package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.client.gui.AttackWheelOverlay;
import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.attack.moveset.CqcMoveset;
import com.xirc.nichirin.common.attack.moveset.DefaultKatanaMoveset;
import com.xirc.nichirin.common.attack.moveset.DefaultGunMoveset;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.item.gun.GenyaDB;
import com.xirc.nichirin.common.network.c2s.MoveHotkeyPacket;
import com.xirc.nichirin.common.util.BreathingManager;
import com.xirc.nichirin.common.util.MultiplayerInputHandler;
import com.xirc.nichirin.common.util.StaminaManager;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

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
    private static boolean currentWheelIsFighting = false;
    // True when showing default katana moves (no breathing style) — send MoveHotkeyPacket on execute
    private static boolean isDefaultKatanaWheel = false;
    // True when showing the gun (Genya DB) moves — also sends MoveHotkeyPacket on execute
    private static boolean isDefaultGunWheel = false;

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

        if (mc.player.hasEffect(NichirinEffectRegistry.stunned())) {
            return;
        }

        // Check if player has blocking effect - CAN'T OPEN WHEEL
        if (mc.player.hasEffect(NichirinEffectRegistry.blocking())) {
            return;
        }

        AbstractMoveset moveset = null;
        boolean isBreathingWheel = false;
        boolean isFightingWheel = false;
        isDefaultKatanaWheel = false;
        isDefaultGunWheel = false;

        // Check held item
        ItemStack mainHand = mc.player.getMainHandItem();
        boolean holdingKatana = mainHand.getItem() instanceof SimpleKatana;
        boolean holdingGun = mainHand.getItem() instanceof GenyaDB;

        if (holdingGun) {
            // Gun has its own neutral moveset (Gun Bash / Grab).
            moveset = DefaultGunMoveset.INSTANCE;
            isDefaultGunWheel = true;
        } else if (holdingKatana) {
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
            // Empty hand - fighting styles take priority over demon arts.
            if (mainHand.isEmpty() && MovesetHelper.hasFightingMoveset(mc.player)) {
                moveset = MovesetHelper.getFightingMoveset(mc.player);
                isFightingWheel = true;
            } else if (MovesetHelper.hasDemonMoveset(mc.player)) {
                moveset = MovesetHelper.getDemonMoveset(mc.player);
                isBreathingWheel = false;
            }
        }

        // Must have appropriate moveset to open wheel
        if (moveset == null) {
            return;
        }
        if (!hasOpenableWheelMoves(moveset, mc.player)) {
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
            currentWheelIsFighting = isFightingWheel;

            // Reset captured move state
            capturedSelectedMove = -1;
            lastHoveredMove = -1;

            // Update state (handles server sync)
            MultiplayerInputHandler.setAttackWheelOpen(true, mc.player);

            wasAttackDown = false;
        }
    }

    private static boolean hasOpenableWheelMoves(AbstractMoveset moveset, Player player) {
        if (isDefaultKatanaWheel) {
            return true;
        }

        final boolean[] hasMove = {false};
        Runnable checkMoves = () -> {
            int count = moveset.getMoveCount();
            for (int i = 0; i < count; i++) {
                if (moveset.getMove(i) != null) {
                    hasMove[0] = true;
                    return;
                }
            }
        };

        if (moveset instanceof CqcMoveset) {
            CqcMoveset.withPlayer(player, checkMoves);
        } else {
            checkMoves.run();
        }

        return hasMove[0];
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
        currentWheelIsFighting = false;  // Reset
        isDefaultKatanaWheel = false;    // Reset
        isDefaultGunWheel = false;       // Reset

        // Reset captured move state
        capturedSelectedMove = -1;
        lastHoveredMove = -1;
        Arrays.fill(wasNumberKeyDown, false);

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
        } else if (currentWheelIsFighting) {
            moveset = MovesetHelper.getFightingMoveset(mc.player);
        } else if (isDefaultKatanaWheel) {
            // Default katana — use the static display moveset for config lookup only
            moveset = DefaultKatanaMoveset.INSTANCE;
        } else if (isDefaultGunWheel) {
            // Gun — static display moveset for config lookup only
            moveset = DefaultGunMoveset.INSTANCE;
        } else {
            moveset = MovesetHelper.getDemonMoveset(mc.player);
        }

        if (moveset == null) {
            closeWheel();
            return;
        }

        final AbstractMoveset selectedMoveset = moveset;
        final AbstractMoveset.MoveConfiguration[] moveConfigHolder = new AbstractMoveset.MoveConfiguration[1];
        Runnable resolveMove = () -> moveConfigHolder[0] = selectedMoveset.getMove(selectedMove);
        if (selectedMoveset instanceof CqcMoveset) {
            CqcMoveset.withPlayer(mc.player, resolveMove);
        } else {
            resolveMove.run();
        }

        var moveConfig = moveConfigHolder[0];
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

        // Breathing and fighting styles can spend stamina. Demon arts don't use this path.
        if (currentWheelIsBreathing || currentWheelIsFighting) {
            if (moveConfig.hasStaminaCost() && !StaminaManager.hasStamina(mc.player, moveConfig.getStaminaCost())) {
                mc.player.displayClientMessage(
                        Component.literal("Not enough stamina!")
                                .withStyle(style -> style.withColor(0xFF5555)),
                        true
                );
                closeWheel();
                return;
            }

            if (currentWheelIsBreathing && moveConfig.hasBreathCost() && !BreathingManager.hasBreath(mc.player, moveConfig.getBreathCost())) {
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
        } else if (currentWheelIsFighting) {
            MultiplayerInputHandler.sendDemonMove(selectedMove, mc.player);
        } else if (isDefaultKatanaWheel) {
            // Default katana wheel — server handles via SimpleKatana.performWheelMove
            NichirinPacketRegistry.sendToServer(new MoveHotkeyPacket(selectedMove));
        } else if (isDefaultGunWheel) {
            // Gun wheel — server handles via GenyaDB.performWheelMove
            NichirinPacketRegistry.sendToServer(new MoveHotkeyPacket(selectedMove));
        } else {
            MultiplayerInputHandler.sendDemonMove(selectedMove, mc.player);
        }

        // Close wheel after sending command
        closeWheel();
    }

    /**
     * Check if inputs should be blocked due to wheel state.
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
     * Check all client-side attack blocking gates.
     */
    public static boolean shouldBlockKatanaAttacks() {
        boolean shouldBlock = shouldBlockAttackInputs() || MultiplayerInputHandler.shouldBlockInputsClient();
        return shouldBlock;
    }
}
