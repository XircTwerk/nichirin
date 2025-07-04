package com.xirc.nichirin.fabric.client;

import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.fabric.network.FabricPacketHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class BreathOfNichirinFabricClient implements ClientModInitializer {

    private static boolean wasAttackPressed = false;

    @Override
    public void onInitializeClient() {
        // Register client tick event to detect attack key press
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            LocalPlayer player = client.player;
            ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);

            // Check if player is holding a katana
            if (!(mainHand.getItem() instanceof SimpleKatana)) {
                wasAttackPressed = false;
                return;
            }

            // Check if attack key is pressed (left click)
            boolean isAttackPressed = client.options.keyAttack.isDown();

            // Detect fresh press (not held)
            if (isAttackPressed && !wasAttackPressed) {
                // Check if not hitting a block or entity
                if (client.hitResult == null || client.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                    // Send packet to server to perform attack
                    FabricPacketHandler.sendKatanaAttackPacket();
                }
            }

            wasAttackPressed = isAttackPressed;
        });
    }
}