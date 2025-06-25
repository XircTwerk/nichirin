package com.xirc.nichirin.common.util;

import com.xirc.nichirin.client.BreathOfNichirinClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class AnimationUtils {

    /**
     * Plays an animation for the given player
     * This method handles both client and server calls safely
     */
    public static void playAnimation(Player player, String animationName) {
        System.out.println("DEBUG: AnimationUtils.playAnimation() called");
        System.out.println("DEBUG: Player: " + (player != null ? player.getName().getString() : "null"));
        System.out.println("DEBUG: Animation: " + animationName);
        System.out.println("DEBUG: Side: " + (player != null ? (player.level().isClientSide ? "CLIENT" : "SERVER") : "unknown"));

        if (player == null) {
            System.out.println("DEBUG: Cannot play animation - player is null");
            return;
        }

        if (player.level().isClientSide) {
            System.out.println("DEBUG: Client side - calling playClientAnimation");
            playClientAnimation(player, animationName);
        } else {
            System.out.println("DEBUG: Server side - would send packet to client");
            // Server side - you'd typically send a packet to the client here
            // For now, just log it
        }
    }

    /**
     * Client-side animation playing
     */
    @Environment(EnvType.CLIENT)
    private static void playClientAnimation(Player player, String animationName) {
        System.out.println("DEBUG: playClientAnimation() called");

        // Check if client is ready
        if (!BreathOfNichirinClient.isClientReady()) {
            System.out.println("DEBUG: Client not ready, cannot play animation");
            return;
        }

        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) {
            System.out.println("DEBUG: LocalPlayer is null, cannot play animation");
            return;
        }

        if (!localPlayer.equals(player)) {
            System.out.println("DEBUG: Player mismatch - localPlayer: " + localPlayer.getName().getString() +
                    ", provided player: " + player.getName().getString());
            return;
        }

        System.out.println("DEBUG: All checks passed, mapping animation name");
        ResourceLocation animation = mapAnimationName(animationName);
        System.out.println("DEBUG: Mapped animation: " + animation);

        // Here you would actually trigger your animation system
        // For now, just log success
        System.out.println("DEBUG: Animation '" + animationName + "' would be played for player " + player.getName().getString());
    }

    /**
     * Maps animation names to resource locations
     */
    private static ResourceLocation mapAnimationName(String animationName) {
        System.out.println("DEBUG: Mapping animation name: " + animationName);

        // Handle different naming conventions
        switch (animationName.toLowerCase()) {
            case "light_slash_1":
            case "light-slash-1":
                System.out.println("DEBUG: Mapped to LIGHT_SLASH_1");
                return new ResourceLocation("nichirin", "attacks.basic.light_slash_1");
            case "light_slash_2":
            case "light-slash-2":
                System.out.println("DEBUG: Mapped to LIGHT_SLASH_2");
                return new ResourceLocation("nichirin", "attacks.basic.light_slash_2");
            case "katana_idle":
                System.out.println("DEBUG: Mapped to KATANA_IDLE");
                return new ResourceLocation("nichirin", "katana_idle");
            default:
                // Try to construct a path for attacks/basic folder
                if (animationName.startsWith("light_slash") || animationName.contains("slash")) {
                    ResourceLocation result = new ResourceLocation("nichirin", "attacks.basic." + animationName);
                    System.out.println("DEBUG: Mapped slash animation to: " + result);
                    return result;
                }
                // Default path
                ResourceLocation result = new ResourceLocation("nichirin", animationName);
                System.out.println("DEBUG: Mapped to default path: " + result);
                return result;
        }
    }
}