package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.client.gui.AttackWheelOverlay;
import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.client.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.util.BreathingManager;
import com.xirc.nichirin.common.util.StaminaManager;
import com.xirc.nichirin.common.util.MultiplayerInputHandler;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * ENHANCED ATTACK WHEEL HANDLER WITH INPUT BLOCKING
 *
 * Features:
 * - Blocks left-click attacks when wheel is open
 * - Blocks left-click attacks for 2 seconds after wheel closes
 * - Proper state management for multiplayer sync
 */
public class AttackWheelHandler {

    private static boolean wasKeyDown = false;
    private static AttackWheelOverlay currentWheel = null;
    private static boolean wheelOpen = false;
    private static boolean wasAttackDown = false;

    // New blocking system
    private static long wheelClosedTime = 0;
    private static final long BLOCK_DURATION_TICKS = 40; // 2 seconds at 20 TPS

    public static void register() {
        // Create the overlay instance
        currentWheel = new AttackWheelOverlay();

        // Register wheel toggle
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (client.player == null) return;

            boolean isKeyDown = NichirinKeybindRegistry.ATTACK_WHEEL_KEY.isDown();

            // Key just pressed (not held)
            if (isKeyDown && !wasKeyDown) {
                if (!wheelOpen) {
                    openWheel();
                } else {
                    closeWheel();
                }
            }

            wasKeyDown = isKeyDown;
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

            // NUCLEAR OPTION: Consume ALL clicks while wheel is open
            int clicksConsumed = 0;
            while (client.options.keyAttack.consumeClick()) {
                clicksConsumed++;
            }
            while (client.options.keyUse.consumeClick()) {
                clicksConsumed++;
            }

            // IMMEDIATE EXECUTION: Execute and close on first click detection
            if (isAttackDown && !wasAttackDown) {
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
    }

    /**
     * Open the attack wheel
     */
    private static void openWheel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Check if holding katana
        ItemStack mainHand = mc.player.getMainHandItem();
        if (!(mainHand.getItem() instanceof SimpleKatana)) return;

        // Check if has breathing style
        if (!BreathingStyleHelper.hasMoveset(mc.player)) return;

        // Don't open if screen is open
        if (mc.screen != null) return;

        if (currentWheel != null) {
            mc.mouseHandler.releaseMouse();
            currentWheel.activate();
            wheelOpen = true;

            // Update state (handles server sync)
            MultiplayerInputHandler.setAttackWheelOpen(true, mc.player);

            wasAttackDown = false;

            System.out.println("DEBUG: Attack wheel opened - inputs now blocked");
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

        // START BLOCKING TIMER - Record when wheel was closed
        if (mc.player != null) {
            wheelClosedTime = mc.player.level().getGameTime();
            System.out.println("DEBUG: Attack wheel closed - inputs blocked for 2 seconds");
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

    /**
     * Execute the currently hovered wheel move
     */
    private static void executeWheelMove() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || currentWheel == null) return;

        int selectedMove = currentWheel.getCurrentlyHoveredMove();

        if (selectedMove == -1) {
            closeWheel();
            return;
        }

        var moveset = BreathingStyleHelper.getMoveset(mc.player);
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

        // Send breathing move to server FIRST (while wheel is still considered open)
        MultiplayerInputHandler.sendBreathingMove(selectedMove, mc.player);

        // THEN close wheel (this maintains input blocking until move is sent)
        closeWheel();
    }

    /**
     * Check if wheel is open (for other systems)
     */
    public static boolean isWheelOpen() {
        return wheelOpen;
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
                System.out.println("DEBUG: Attack input blocking period ended");
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

        if (shouldBlock) {
            System.out.println("DEBUG: Katana attacks blocked - wheel state or multiplayer input handler");
        }

        return shouldBlock;
    }
}