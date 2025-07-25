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
 * CLEAN ATTACK WHEEL HANDLER
 *
 * Simplified to work with UnifiedInputSystem:
 * - No complex state management
 * - No packet handling (handled by UnifiedInputSystem)
 * - Just UI and basic input consumption
 */
public class AttackWheelHandler {

    private static boolean wasKeyDown = false;
    private static AttackWheelOverlay currentWheel = null;
    private static boolean wheelOpen = false;
    private static boolean wasAttackDown = false;

    public static void register() {
        System.out.println("DEBUG: AttackWheelHandler.register() - Using MultiplayerInputHandler");

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

            if (clicksConsumed > 0) {
                System.out.println("DEBUG: AttackWheelHandler - CONSUMED " + clicksConsumed + " clicks while wheel open");
            }

            // IMMEDIATE EXECUTION: Execute and close on first click detection
            if (isAttackDown && !wasAttackDown) {
                System.out.println("DEBUG: AttackWheelHandler - Click detected, executing and closing IMMEDIATELY");
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
            System.out.println("DEBUG: AttackWheelHandler - Wheel opened");
        }
    }

    /**
     * Close the attack wheel
     */
    private static void closeWheel() {
        Minecraft mc = Minecraft.getInstance();

        if (currentWheel != null) {
            currentWheel.deactivate();
        }
        wheelOpen = false;

        // Update state (handles server sync)
        if (mc.player != null) {
            MultiplayerInputHandler.setAttackWheelOpen(false, mc.player);
        }

        wasAttackDown = false;

        // Recapture mouse
        if (mc.screen == null) {
            mc.mouseHandler.grabMouse();
        }

        System.out.println("DEBUG: AttackWheelHandler - Wheel closed");
    }

    /**
     * Execute the currently hovered wheel move
     */
    private static void executeWheelMove() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || currentWheel == null) return;

        int selectedMove = currentWheel.getCurrentlyHoveredMove();
        System.out.println("DEBUG: executeWheelMove - selectedMove: " + selectedMove);

        if (selectedMove == -1) {
            System.out.println("DEBUG: executeWheelMove - No move selected, closing wheel");
            closeWheel();
            return;
        }

        var moveset = BreathingStyleHelper.getMoveset(mc.player);
        if (moveset == null) {
            System.out.println("DEBUG: executeWheelMove - No moveset, closing wheel");
            closeWheel();
            return;
        }

        var moveConfig = moveset.getMove(selectedMove);
        if (moveConfig == null) {
            System.out.println("DEBUG: executeWheelMove - No move config, closing wheel");
            closeWheel();
            return;
        }

        // Check requirements client-side (for immediate feedback)
        String moveName = moveConfig.getDisplayName();
        System.out.println("DEBUG: executeWheelMove - Attempting to execute: " + moveName);

        if (CooldownHUD.isOnCooldown(moveName)) {
            int remaining = CooldownHUD.getRemainingCooldown(moveName);
            System.out.println("DEBUG: executeWheelMove - Move on cooldown, closing wheel");
            mc.player.displayClientMessage(
                    Component.literal("Move on cooldown! " + (remaining / 20.0f) + "s remaining")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            closeWheel();
            return;
        }

        if (moveConfig.hasStaminaCost() && !StaminaManager.hasStamina(mc.player, moveConfig.getStaminaCost())) {
            System.out.println("DEBUG: executeWheelMove - Not enough stamina, closing wheel");
            mc.player.displayClientMessage(
                    Component.literal("Not enough stamina!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            closeWheel();
            return;
        }

        if (moveConfig.hasBreathCost() && !BreathingManager.hasBreath(mc.player, moveConfig.getBreathCost())) {
            System.out.println("DEBUG: executeWheelMove - Not enough breath, closing wheel");
            mc.player.displayClientMessage(
                    Component.literal("Not enough breath!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            closeWheel();
            return;
        }

        System.out.println("DEBUG: executeWheelMove - All checks passed, sending breathing move");

        // Send breathing move to server FIRST (while wheel is still considered open)
        MultiplayerInputHandler.sendBreathingMove(selectedMove, mc.player);

        // THEN close wheel (this maintains input blocking until move is sent)
        closeWheel();

        System.out.println("DEBUG: AttackWheelHandler - Executed move " + selectedMove + " (" + moveName + ")");
    }

    /**
     * Check if wheel is open (for other systems)
     */
    public static boolean isWheelOpen() {
        return wheelOpen;
    }

    /**
     * DEPRECATED: Use UnifiedInputSystem instead
     * @deprecated Use UnifiedInputSystem.shouldBlockInputsClient()
     */
    @Deprecated
    public static boolean shouldBlockKatanaAttacks() {
        return MultiplayerInputHandler.shouldBlockInputsClient();
    }
}