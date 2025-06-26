package com.xirc.nichirin.client;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

@Environment(EnvType.CLIENT)
public class BreathOfNichirinClient {

    // Add this field to track initialization state
    private static boolean initialized = false;

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

        // Mark as initialized after all setup is complete
        initialized = true;
    }

    /**
     * Safe method to check if we can perform client-side operations
     */
    public static boolean isClientReady() {
        if (!initialized) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null &&
                minecraft.level != null &&
                minecraft.player != null &&
                !minecraft.player.isRemoved();
    }
}