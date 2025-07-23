package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.client.gui.AttackWheelOverlay;
import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.client.registry.NichirinKeybindRegistry;
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
public class AttackWheelHandler {

    private static boolean wasKeyDown = false;
    private static AttackWheelOverlay currentWheel = null;
    @Getter
    private static boolean wheelOpen = false;
    private static boolean wasAttackDown = false;
    private static long wheelClosedTime = 0; // Track when wheel was closed
    private static final int ATTACK_BLOCK_DELAY = 5; // Ticks to block attacks after wheel closes

    // SIMPLE SOLUTION: Static flag that KatanaInputHandler can check directly
    public static boolean shouldBlockKatanaAttacks() {
        if (wheelOpen) {
            return true; // Always block when wheel is open
        }

        // Also block for a few ticks after wheel closes to prevent lingering clicks
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && wheelClosedTime > 0) {
            long currentTime = mc.level.getGameTime();
            if (currentTime - wheelClosedTime <= ATTACK_BLOCK_DELAY) {
                return true; // Still blocking attacks for a brief period
            }
        }

        return false; // Allow attacks
    }

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

            // If a screen is open and wheel was open, force close the wheel
            if (wheelOpen && mc.screen != null) {
                forceCloseWheel();
                return;
            }

            if (currentWheel != null && currentWheel.isActive()) {
                currentWheel.render(guiGraphics);
            }
        });

        // SIMPLE MOUSE HANDLING: Just check for clicks when wheel is open
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (wheelOpen && client.player != null) {
                boolean isAttackDown = client.options.keyAttack.isDown();

                // CONSUME ALL CLICKS WHILE WHEEL IS OPEN - NUCLEAR APPROACH
                int clicksConsumed = 0;
                while (client.options.keyAttack.consumeClick()) {
                    clicksConsumed++;
                }
                if (clicksConsumed > 0) {
                    System.out.println("DEBUG: Consumed " + clicksConsumed + " clicks while wheel open");
                }

                // Detect click edge for wheel execution
                if (isAttackDown && !wasAttackDown) {
                    System.out.println("DEBUG: Click detected while wheel open - executing");
                    executeAndCloseWheel();
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Check if player is holding a SimpleKatana
        ItemStack mainHand = mc.player.getMainHandItem();
        if (!(mainHand.getItem() instanceof SimpleKatana)) return;

        // Check if player has a breathing style selected
        if (!BreathingStyleHelper.hasMoveset(mc.player)) return;

        // Don't open if a screen is open
        if (mc.screen != null) return;

        if (currentWheel != null) {
            mc.mouseHandler.releaseMouse();
            currentWheel.activate();
            wheelOpen = true; // SET THIS FIRST - CRITICAL FOR BLOCKING
            wasAttackDown = false;
            System.out.println("DEBUG: Wheel opened - katana attacks should now be blocked");
        }
    }

    public static void executeAndCloseWheel() {
        Minecraft mc = Minecraft.getInstance();

        if (currentWheel == null || !wheelOpen) return;

        // Get the selected move BEFORE closing
        int selectedMove = currentWheel.getCurrentlyHoveredMove();

        // Close the wheel FIRST
        currentWheel.deactivate();
        wheelOpen = false; // CLEAR THIS IMMEDIATELY
        wheelClosedTime = mc.level != null ? mc.level.getGameTime() : 0; // Record when we closed
        wasAttackDown = false;

        // Recapture mouse
        if (mc.screen == null) {
            mc.mouseHandler.grabMouse();
        }

        System.out.println("DEBUG: Wheel closed - katana attacks blocked for " + ATTACK_BLOCK_DELAY + " ticks");

        // Execute the move if one was selected
        if (selectedMove != -1) {
            executeMove(selectedMove);
        }
    }

    public static void forceCloseWheel() {
        Minecraft mc = Minecraft.getInstance();

        if (currentWheel != null) {
            currentWheel.deactivate();
        }
        wheelOpen = false; // CLEAR THIS IMMEDIATELY
        wheelClosedTime = mc.level != null ? mc.level.getGameTime() : 0; // Record when we closed
        wasAttackDown = false;

        if (mc.screen == null) {
            mc.mouseHandler.grabMouse();
        }

        System.out.println("DEBUG: Wheel force closed - katana attacks blocked for " + ATTACK_BLOCK_DELAY + " ticks");
    }

    private static void executeMove(int moveIndex) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        var moveset = BreathingStyleHelper.getMoveset(mc.player);
        if (moveset == null) return;

        var moveConfig = moveset.getMove(moveIndex);
        if (moveConfig == null) return;

        // Check requirements
        String moveName = moveConfig.getDisplayName();
        if (CooldownHUD.isOnCooldown(moveName)) {
            int remaining = CooldownHUD.getRemainingCooldown(moveName);
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Move on cooldown! " + (remaining / 20.0f) + " seconds remaining")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            return;
        }

        if (moveConfig.hasStaminaCost() && !StaminaManager.hasStamina(mc.player, moveConfig.getStaminaCost())) {
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Not enough stamina!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            return;
        }

        if (moveConfig.hasBreathCost() && !BreathingManager.hasBreath(mc.player, moveConfig.getBreathCost())) {
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Not enough breath!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            return;
        }

        // Send packet to server
        BreathingMovePacket packet = new BreathingMovePacket(moveIndex, true);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.toBytes(buf);
        NetworkManager.sendToServer(NichirinPacketRegistry.BREATHING_MOVE_ID, buf);
    }
}