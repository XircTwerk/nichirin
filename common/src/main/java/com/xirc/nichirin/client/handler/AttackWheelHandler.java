package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.client.gui.AttackWheelOverlay;
import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.client.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.client.util.InputStateManager;
import com.xirc.nichirin.common.network.BreathingMovePacket;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.util.BreathingManager;
import com.xirc.nichirin.common.util.StaminaManager;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * Handles the attack wheel input and display using HUD overlay
 */
public class AttackWheelHandler { //TODO: fix the issue where it inputs a left click slash whenever you click on an attack on top of the executed attack.

    private static boolean wasKeyDown = false;
    private static AttackWheelOverlay currentWheel = null;
    @Getter
    private static boolean wheelOpen = false;
    private static boolean wasAttackDown = false;

    public static void register() {
        System.out.println("DEBUG: AttackWheelHandler.register() called!");

        // Create the overlay instance
        currentWheel = new AttackWheelOverlay();

        // Register tick handler for wheel toggle
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (client.player == null) return;

            boolean isKeyDown = NichirinKeybindRegistry.ATTACK_WHEEL_KEY.isDown();

            // Key just pressed (not held)
            if (isKeyDown && !wasKeyDown) {
                System.out.println("DEBUG: Attack wheel key pressed. Wheel open: " + wheelOpen);
                if (!wheelOpen) {
                    openWheel();
                } else {
                    forceCloseWheel();
                }
            }

            wasKeyDown = isKeyDown;
        });

        // Register HUD render event and handle screen state changes
        ClientGuiEvent.RENDER_HUD.register((guiGraphics, partialTick) -> {
            Minecraft mc = Minecraft.getInstance();

            // CRITICAL: If a screen is open and wheel was open, force close the wheel
            if (wheelOpen && mc.screen != null) {
                System.out.println("DEBUG: Screen opened while wheel active - force closing wheel");
                forceCloseWheel();
                return;
            }

            if (currentWheel != null && currentWheel.isActive()) {
                currentWheel.render(guiGraphics);
            }
        });

        // Register mouse click handler - NUCLEAR APPROACH: OVERRIDE ATTACK KEY COMPLETELY
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (wheelOpen && client.player != null) {
                // NUCLEAR: Completely override the attack key state
                boolean isAttackDown = client.options.keyAttack.isDown();

                // Force consume ALL attack clicks while wheel is open
                while (client.options.keyAttack.consumeClick()) {
                    System.out.println("DEBUG: CONSUMED attack click while wheel open");
                }

                // Detect click edge for wheel execution
                if (isAttackDown && !wasAttackDown) {
                    System.out.println("DEBUG: WHEEL CLICK -> executing move");
                    if (currentWheel != null) {
                        executeAndCloseWheel();
                    }
                }

                // Block hotbar changes
                for (int i = 0; i < 9; i++) {
                    while (client.options.keyHotbarSlots[i].consumeClick()) {
                        // Eat hotbar changes
                    }
                }

                wasAttackDown = isAttackDown;
            } else {
                wasAttackDown = false;
            }
        });
    }

    private static void openWheel() {
        System.out.println("DEBUG: openWheel() called!");
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            System.out.println("DEBUG: Player is null");
            return;
        }

        // Check if player is holding a SimpleKatana
        ItemStack mainHand = mc.player.getMainHandItem();
        if (!(mainHand.getItem() instanceof SimpleKatana)) {
            System.out.println("DEBUG: Not holding SimpleKatana - item: " + mainHand.getItem().getClass().getSimpleName());
            return;
        }

        // Check if player has a breathing style selected
        if (!BreathingStyleHelper.hasMoveset(mc.player)) {
            System.out.println("DEBUG: No breathing style selected");
            return;
        }

        // Don't open if a screen is open (like inventory, escape menu, etc.)
        if (mc.screen != null) {
            System.out.println("DEBUG: Cannot open wheel - screen is already open: " + mc.screen.getClass().getSimpleName());
            return;
        }

        System.out.println("DEBUG: Activating wheel...");
        if (currentWheel != null) {
            // FREE THE MOUSE - Allow free mouse movement
            System.out.println("DEBUG: About to release mouse. Current mouse grabbed state: " + mc.mouseHandler.isMouseGrabbed());
            mc.mouseHandler.releaseMouse();
            System.out.println("DEBUG: Mouse released. New grabbed state: " + mc.mouseHandler.isMouseGrabbed());

            currentWheel.activate();
            wheelOpen = true;
            InputStateManager.setAttackWheelOpen(true); // Update shared state
            wasAttackDown = false; // Reset click state
            System.out.println("DEBUG: Wheel activated successfully!");
        } else {
            System.out.println("DEBUG: currentWheel is null!");
        }
    }

    public static void executeAndCloseWheel() {
        System.out.println("DEBUG: executeAndCloseWheel() called");
        Minecraft mc = Minecraft.getInstance();

        if (currentWheel == null || !wheelOpen) {
            System.out.println("DEBUG: Wheel not open or null");
            return;
        }

        // CRITICAL FIX: Get the selected move BEFORE deactivating and BEFORE recapturing mouse
        int selectedMove = currentWheel.getCurrentlyHoveredMove();
        System.out.println("DEBUG: Selected move BEFORE deactivate: " + selectedMove);

        // Deactivate the wheel
        currentWheel.deactivate();
        wheelOpen = false;
        InputStateManager.setAttackWheelOpen(false); // Update shared state
        wasAttackDown = false; // Reset click state

        // RECAPTURE THE MOUSE after getting the selection
        System.out.println("DEBUG: About to recapture mouse. Current grabbed state: " + mc.mouseHandler.isMouseGrabbed());
        if (mc.screen == null) {
            mc.mouseHandler.grabMouse();
            System.out.println("DEBUG: Mouse recaptured. New grabbed state: " + mc.mouseHandler.isMouseGrabbed());
        }

        // Execute the move if one was selected
        if (selectedMove != -1) {
            executeMove(selectedMove);
            System.out.println("DEBUG: Executed move: " + selectedMove);
        } else {
            System.out.println("DEBUG: No move selected, not executing");
        }
    }

    public static void forceCloseWheel() {
        System.out.println("DEBUG: forceCloseWheel() called");
        Minecraft mc = Minecraft.getInstance();

        if (currentWheel != null) {
            currentWheel.deactivate();
        }
        wheelOpen = false;
        InputStateManager.setAttackWheelOpen(false); // Update shared state
        wasAttackDown = false; // Reset click state

        // ALWAYS RECAPTURE THE MOUSE when closing
        System.out.println("DEBUG: Force close - about to recapture mouse. Current grabbed state: " + mc.mouseHandler.isMouseGrabbed());
        if (mc.screen == null) {
            mc.mouseHandler.grabMouse();
            System.out.println("DEBUG: Mouse recaptured in force close. New grabbed state: " + mc.mouseHandler.isMouseGrabbed());
        } else {
            System.out.println("DEBUG: Cannot recapture mouse in force close - screen is open");
        }
    }

    private static void executeMove(int moveIndex) {
        System.out.println("DEBUG: Attempting to execute move for index: " + moveIndex);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            System.out.println("DEBUG: No player to execute move");
            return;
        }

        // Check if player has a breathing style
        var moveset = BreathingStyleHelper.getMoveset(mc.player);
        if (moveset == null) {
            System.out.println("DEBUG: No moveset found for player");
            return;
        }

        // Get the move configuration
        var moveConfig = moveset.getMove(moveIndex);
        if (moveConfig == null) {
            System.out.println("DEBUG: No move config found for index: " + moveIndex);
            return;
        }

        // CLIENT-SIDE VALIDATION: Check all requirements before sending packet
        boolean canExecute = true;
        String failureReason = "";

        // Check cooldown using your existing CooldownHUD system
        String moveName = moveConfig.getDisplayName();
        if (CooldownHUD.isOnCooldown(moveName)) {
            int remaining = CooldownHUD.getRemainingCooldown(moveName);
            canExecute = false;
            failureReason = "Move on cooldown! " + (remaining / 20.0f) + " seconds remaining";
        }

        // Check stamina cost
        if (canExecute && moveConfig.hasStaminaCost()) {
            float staminaCost = moveConfig.getStaminaCost();
            if (!StaminaManager.hasStamina(mc.player, staminaCost)) {
                canExecute = false;
                failureReason = "Not enough stamina! Need " + staminaCost;
            }
        }

        // Check breath cost
        if (canExecute && moveConfig.hasBreathCost()) {
            float breathCost = moveConfig.getBreathCost();
            if (!BreathingManager.hasBreath(mc.player, breathCost)) {
                canExecute = false;
                failureReason = "Not enough breath! Need " + breathCost;
            }
        }

        if (!canExecute) {
            System.out.println("DEBUG: Cannot execute move - " + failureReason);
            // Show error message in chat
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(failureReason)
                                .withStyle(style -> style.withColor(0xFF5555)),
                        true
                );
            }
            return;
        }

        // Send packet to server - server will handle the actual cooldown setting via CooldownDisplayPacket
        System.out.println("DEBUG: Sending move packet for index: " + moveIndex);
        BreathingMovePacket packet = new BreathingMovePacket(moveIndex, true);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.toBytes(buf);
        NetworkManager.sendToServer(NichirinPacketRegistry.BREATHING_MOVE_ID, buf);
    }
}