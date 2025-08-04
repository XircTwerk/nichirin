package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.client.gui.AttackWheelOverlay;
import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.client.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
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
 * FIXED ATTACK WHEEL HANDLER - RELIABLE MOVE EXECUTION
 * Now captures and stores the highlighted move instead of calculating at click time
 */
public class AttackWheelHandler {

    private static boolean wasKeyDown = false;
    private static boolean wasEscDown = false;
    private static AttackWheelOverlay currentWheel = null;
    /**
     * -- GETTER --
     *  Check if wheel is open (for other systems)
     */
    @Getter
    private static boolean wheelOpen = false;
    private static boolean wasAttackDown = false;

    /**
     * -- GETTER --
     *  NEW: Get the currently captured/selected move (for debugging)
     */
    // NEW: Store the currently selected move reliably
    @Getter
    private static int capturedSelectedMove = -1;
    private static int lastHoveredMove = -1;

    // New blocking system
    private static long wheelClosedTime = 0;
    private static final long BLOCK_DURATION_TICKS = 40; // 2 seconds at 20 TPS

    public static void register() {
        System.out.println("DEBUG: AttackWheelHandler.register() called");

        // Create the overlay instance
        currentWheel = new AttackWheelOverlay();
        System.out.println("DEBUG: AttackWheelOverlay created");

        // Register wheel toggle with ESC handling
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (client.player == null) return;

            boolean isKeyDown = NichirinKeybindRegistry.ATTACK_WHEEL_KEY.isDown();
            boolean isEscDown = org.lwjgl.glfw.GLFW.glfwGetKey(client.getWindow().getWindow(), GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;

            // DEBUG: Log key state changes
            if (isKeyDown != wasKeyDown) {
                System.out.println("DEBUG: Attack wheel key state changed: " + isKeyDown);
                if (isKeyDown) {
                    System.out.println("DEBUG: Attack wheel key PRESSED!");
                    System.out.println("DEBUG: Player has katana: " + (client.player.getMainHandItem().getItem() instanceof SimpleKatana));
                    System.out.println("DEBUG: Player has moveset: " + BreathingStyleHelper.hasMoveset(client.player));
                    System.out.println("DEBUG: Current screen: " + (client.screen != null ? client.screen.getClass().getSimpleName() : "null"));
                    System.out.println("DEBUG: Wheel currently open: " + wheelOpen);
                    System.out.println("DEBUG: Player stunned: " + client.player.hasEffect(NichirinEffectRegistry.STUNNED.get()));
                    System.out.println("DEBUG: Player blocking: " + client.player.hasEffect(NichirinEffectRegistry.BLOCKING.get()));
                }
            }

            // Handle ESC when wheel is open - more aggressive for multiplayer
            if (wheelOpen && isEscDown && !wasEscDown) {
                System.out.println("DEBUG: ESC pressed while wheel open - closing wheel");
                closeWheel();

                // MULTIPLAYER FIX: More aggressive pause menu prevention
                // Check immediately and for the next few ticks
                for (int i = 0; i < 3; i++) {
                    if (client.screen != null && client.screen.getClass().getSimpleName().contains("Pause")) {
                        System.out.println("DEBUG: Preventing pause screen #" + i);
                        client.setScreen(null);
                        client.mouseHandler.grabMouse();
                    }
                }
            }

            // Handle attack wheel key
            if (isKeyDown && !wasKeyDown) {
                System.out.println("DEBUG: Processing attack wheel key press");
                if (!wheelOpen) {
                    System.out.println("DEBUG: Attempting to open wheel");
                    openWheel();
                } else {
                    System.out.println("DEBUG: Attempting to close wheel");
                    closeWheel();
                }
            }

            // NEW: Capture highlighted move every tick while wheel is open
            if (wheelOpen && currentWheel != null) {
                int currentHovered = currentWheel.getCurrentlyHoveredMove();
                if (currentHovered != lastHoveredMove) {
                    System.out.println("DEBUG: Hovered move changed from " + lastHoveredMove + " to " + currentHovered);
                    lastHoveredMove = currentHovered;
                    // Capture the move when it becomes highlighted
                    if (currentHovered >= 0) {
                        capturedSelectedMove = currentHovered;
                        System.out.println("DEBUG: Captured selected move: " + capturedSelectedMove);
                    }
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
                    System.out.println("DEBUG: PRE tick - preventing pause screen: " + screenName);
                    client.setScreen(null);
                    client.mouseHandler.grabMouse();
                }
            }
        });

        // Additional multiplayer-specific check in POST tick
        ClientTickEvent.CLIENT_POST.register(client -> {
            // MULTIPLAYER FIX: Double-check for pause screens that might appear delayed
            if (wheelOpen && client.screen != null) {
                String screenName = client.screen.getClass().getSimpleName();
                if (screenName.contains("Pause")) {
                    System.out.println("DEBUG: POST tick - preventing pause screen: " + screenName);
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
                System.out.println("DEBUG: Screen opened while wheel active - closing wheel");
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

            // NUCLEAR OPTION: Consume ALL clicks while wheel is open
            while (client.options.keyAttack.consumeClick()) {
                // Consume attack clicks
            }
            while (client.options.keyUse.consumeClick()) {
                // Consume use clicks
            }

            // IMMEDIATE EXECUTION: Execute and close on first click detection
            if (isAttackDown && !wasAttackDown) {
                System.out.println("DEBUG: Attack button pressed while wheel open - executing move");
                executeWheelMove(); // This closes the wheel immediately
            }

            // Block ALL other inputs aggressively
            for (int i = 0; i < 9; i++) {
                while (client.options.keyHotbarSlots[i].consumeClick()) {
                    // Consume hotbar keys
                }
            }
            while (client.options.keyDrop.consumeClick()) {
                // Consume drop key
            }
            while (client.options.keyInventory.consumeClick()) {
                // Consume inventory key
            }

            wasAttackDown = isAttackDown;
        });

        System.out.println("DEBUG: AttackWheelHandler registration complete");
    }

    /**
     * Open the attack wheel
     */
    private static void openWheel() {
        System.out.println("DEBUG: openWheel() called");
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            System.out.println("DEBUG: openWheel() failed - no player");
            return;
        }

        if (mc.player.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            System.out.println("DEBUG: openWheel() failed - player stunned");
            return;
        }

        // Check if player has blocking effect - CAN'T OPEN WHEEL
        if (mc.player.hasEffect(NichirinEffectRegistry.BLOCKING.get())) {
            System.out.println("DEBUG: openWheel() failed - player blocking");
            return;
        }

        // Check if holding katana
        ItemStack mainHand = mc.player.getMainHandItem();
        if (!(mainHand.getItem() instanceof SimpleKatana)) {
            System.out.println("DEBUG: openWheel() failed - not holding katana");
            System.out.println("DEBUG: Main hand item: " + mainHand.getItem().getClass().getSimpleName());
            return;
        }

        // Check if has breathing style
        if (!BreathingStyleHelper.hasMoveset(mc.player)) {
            System.out.println("DEBUG: openWheel() failed - no breathing style");
            return;
        }

        // Don't open if screen is open
        if (mc.screen != null) {
            System.out.println("DEBUG: openWheel() failed - screen is open: " + mc.screen.getClass().getSimpleName());
            return;
        }

        if (currentWheel != null) {
            System.out.println("DEBUG: Opening attack wheel");
            mc.mouseHandler.releaseMouse();
            currentWheel.activate();
            wheelOpen = true;

            // Reset captured move state
            capturedSelectedMove = -1;
            lastHoveredMove = -1;
            System.out.println("DEBUG: Reset captured move state");

            // Update state (handles server sync)
            MultiplayerInputHandler.setAttackWheelOpen(true, mc.player);
            System.out.println("DEBUG: Attack wheel opened successfully");

            wasAttackDown = false;
        } else {
            System.out.println("DEBUG: openWheel() failed - currentWheel is null");
        }
    }

    /**
     * Close the attack wheel and start blocking timer
     */
    private static void closeWheel() {
        System.out.println("DEBUG: closeWheel() called");
        Minecraft mc = Minecraft.getInstance();

        if (currentWheel != null) {
            currentWheel.deactivate();
            System.out.println("DEBUG: Wheel overlay deactivated");
        }
        wheelOpen = false;

        // Reset captured move state
        capturedSelectedMove = -1;
        lastHoveredMove = -1;

        // START BLOCKING TIMER - Record when wheel was closed
        if (mc.player != null) {
            wheelClosedTime = mc.player.level().getGameTime();
            System.out.println("DEBUG: Wheel close time recorded: " + wheelClosedTime);
        }

        // Update state (handles server sync)
        if (mc.player != null) {
            MultiplayerInputHandler.setAttackWheelOpen(false, mc.player);
            System.out.println("DEBUG: Multiplayer state updated");
        }

        wasAttackDown = false;

        // Recapture mouse
        if (mc.screen == null) {
            mc.mouseHandler.grabMouse();
            System.out.println("DEBUG: Mouse recaptured");
        }

        System.out.println("DEBUG: Attack wheel closed successfully");
    }

    /**
     * FIXED: Execute the captured selected move instead of calculating at click time
     */
    private static void executeWheelMove() {
        System.out.println("DEBUG: executeWheelMove() called");
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || currentWheel == null) {
            System.out.println("DEBUG: executeWheelMove() failed - null player or wheel");
            return;
        }

        // USE CAPTURED MOVE instead of real-time calculation
        int selectedMove = capturedSelectedMove;
        System.out.println("DEBUG: Using captured selected move index: " + selectedMove);
        System.out.println("DEBUG: Real-time hovered move would be: " + currentWheel.getCurrentlyHoveredMove());

        if (selectedMove == -1) {
            System.out.println("DEBUG: No move captured - closing wheel");
            closeWheel();
            return;
        }

        var moveset = BreathingStyleHelper.getMoveset(mc.player);
        if (moveset == null) {
            System.out.println("DEBUG: executeWheelMove() failed - no moveset");
            closeWheel();
            return;
        }

        var moveConfig = moveset.getMove(selectedMove);
        if (moveConfig == null) {
            System.out.println("DEBUG: executeWheelMove() failed - no move config for index " + selectedMove);
            closeWheel();
            return;
        }

        // Check requirements client-side (for immediate feedback)
        String moveName = moveConfig.getDisplayName();
        System.out.println("DEBUG: Executing captured move: " + moveName);

        if (CooldownHUD.isOnCooldown(moveName)) {
            int remaining = CooldownHUD.getRemainingCooldown(moveName);
            System.out.println("DEBUG: Move on cooldown: " + remaining + " ticks remaining");
            mc.player.displayClientMessage(
                    Component.literal("Move on cooldown! " + (remaining / 20.0f) + "s remaining")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            closeWheel();
            return;
        }

        if (moveConfig.hasStaminaCost() && !StaminaManager.hasStamina(mc.player, moveConfig.getStaminaCost())) {
            System.out.println("DEBUG: Not enough stamina");
            mc.player.displayClientMessage(
                    Component.literal("Not enough stamina!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            closeWheel();
            return;
        }

        if (moveConfig.hasBreathCost() && !BreathingManager.hasBreath(mc.player, moveConfig.getBreathCost())) {
            System.out.println("DEBUG: Not enough breath");
            mc.player.displayClientMessage(
                    Component.literal("Not enough breath!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            closeWheel();
            return;
        }

        // Send breathing move to server FIRST (while wheel is still considered open)
        System.out.println("DEBUG: Sending captured breathing move to server: " + selectedMove);
        MultiplayerInputHandler.sendBreathingMove(selectedMove, mc.player);

        // THEN close wheel (this maintains input blocking until move is sent)
        System.out.println("DEBUG: Closing wheel after move execution");
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