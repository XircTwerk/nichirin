package com.xirc.nichirin.client;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

@Environment(EnvType.CLIENT)
public class BreathOfNichirinClient {

    public static void init() {
        System.out.println("DEBUG: BreathOfNichirinClient.init() called");

        // Register client tick event to monitor player state
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            // Only log occasionally to avoid spam
            if (minecraft.level != null && minecraft.level.getGameTime() % 100 == 0) {
                LocalPlayer player = minecraft.player;
                System.out.println("DEBUG: Client tick - Player: " + (player != null ? "exists" : "null") +
                        ", Level: " + (minecraft.level != null ? "exists" : "null"));
            }
        });

        System.out.println("DEBUG: Client initialization complete");
    }

    /**
     * Safe method to check if we can perform client-side operations
     */
    public static boolean isClientReady() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                System.out.println("DEBUG: Minecraft instance is null");
                return false;
            }

            if (mc.player == null) {
                System.out.println("DEBUG: Minecraft.player is null - client not fully initialized");
                return false;
            }

            if (mc.level == null) {
                System.out.println("DEBUG: Minecraft.level is null - world not loaded");
                return false;
            }

            if (mc.player.isRemoved()) {
                System.out.println("DEBUG: Player is removed from world");
                return false;
            }

            // Additional safety checks
            if (mc.gameMode == null) {
                System.out.println("DEBUG: GameMode is null - client not ready");
                return false;
            }

            System.out.println("DEBUG: Client is ready - Player: " + mc.player.getName().getString() +
                    ", Level: " + mc.level.dimension().location());
            return true;

        } catch (Exception e) {
            System.out.println("DEBUG: Exception in isClientReady(): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}