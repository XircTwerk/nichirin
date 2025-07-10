package com.xirc.nichirin.common.util;

import com.xirc.nichirin.client.BreathOfNichirinClient;
import com.xirc.nichirin.client.animation.AnimationRegistryHelper;
import com.xirc.nichirin.client.animation.NichirinAnimations;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class AnimationUtils {

    /**
     * Plays an animation for the given player
     * This method handles both client and server calls safely
     */
    public static void playAnimation(Player player, String animationName) {

        if (player == null) {
            return;
        }

        if (player.level().isClientSide) {
            playClientAnimation(player, animationName);
        } else {
            // Server side - you'd typically send a packet to the client here
        }
    }

    /**
     * Client-side animation playing with comprehensive safety checks
     */
    @Environment(EnvType.CLIENT)
    private static void playClientAnimation(Player player, String animationName) {

        // Check if Minecraft instance is available
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        // Check if we're in a valid game state
        if (minecraft.level == null) {
            return;
        }

        // Check if client is ready
        if (!BreathOfNichirinClient.isClientReady()) {
            return;
        }

        // CRITICAL: Check if LocalPlayer exists before accessing
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer == null) {
            return;
        }

        // Additional safety check - ensure player is alive and in world
        if (localPlayer.isRemoved()) {
            return;
        }

        if (!localPlayer.equals(player)) {
            return;
        }

        // Play the animation using NichirinAnimations
        if (player instanceof AbstractClientPlayer clientPlayer) {

            // First, try to get the animation through the mapped resource location
            ResourceLocation animLoc = mapAnimationName(animationName);
            NichirinAnimations.playAnimation(clientPlayer, animLoc);
        } else {
        }
    }

    /**
     * Checks if an animation is currently playing for the player
     */
    public static boolean isAnimationPlaying(Player player, String animationName) {

        if (player == null) {
            return false;
        }

        if (player.level().isClientSide) {
            return isClientAnimationPlaying(player, animationName);
        }

        // Server side - you'd check server-side animation state here
        return false;
    }

    /**
     * Client-side animation playing check with safety checks
     */
    @Environment(EnvType.CLIENT)
    private static boolean isClientAnimationPlaying(Player player, String animationName) {

        // Safety checks similar to playClientAnimation
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return false;
        }

        if (!BreathOfNichirinClient.isClientReady()) {
            return false;
        }

        // TODO: Implement actual animation state checking
        // For now, always return false
        return false;
    }

    /**
     * Stops an animation for the player
     */
    public static void stopAnimation(Player player, String animationName) {

        if (player == null) {
            return;
        }

        if (player.level().isClientSide) {
            stopClientAnimation(player, animationName);
        } else {
        }
    }

    /**
     * Client-side animation stopping with safety checks
     */
    @Environment(EnvType.CLIENT)
    private static void stopClientAnimation(Player player, String animationName) {

        // Safety checks
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return;
        }

        if (!BreathOfNichirinClient.isClientReady()) {
            return;
        }

        // FIXED: Actually stop the animation using NichirinAnimations
        if (player instanceof AbstractClientPlayer clientPlayer) {
            NichirinAnimations.stopAnimation(clientPlayer);
        }
    }

    /**
     * Gets the current animation playing for the player
     */
    public static String getCurrentAnimation(Player player) {

        if (player == null) {
            return null;
        }

        if (player.level().isClientSide) {
            return getCurrentClientAnimation(player);
        }

        // Server side
        return null;
    }

    /**
     * Client-side current animation check with safety checks
     */
    @Environment(EnvType.CLIENT)
    private static String getCurrentClientAnimation(Player player) {

        // Safety checks
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return null;
        }

        if (!BreathOfNichirinClient.isClientReady()) {
            return null;
        }

        // TODO: Implement actual current animation retrieval
        return null;
    }

    /**
     * Safely checks if the client is in a valid state for animations
     */
    @Environment(EnvType.CLIENT)
    public static boolean isClientValidForAnimations() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null &&
                minecraft.level != null &&
                minecraft.player != null &&
                !minecraft.player.isRemoved() &&
                BreathOfNichirinClient.isClientReady();
    }

    /**
     * Maps animation names to resource locations
     */
    private static ResourceLocation mapAnimationName(String animationName) {

        // Handle different naming conventions
        switch (animationName.toLowerCase()) {
            case "light_slash_1":
            case "light-slash-1":
                return new ResourceLocation("nichirin", "attacks/basic/sword.slash");
            case "light_slash_2":
            case "light-slash-2":
                return new ResourceLocation("nichirin", "attacks/basic/sword.slash2");
            case "katana_idle":
                return new ResourceLocation("nichirin", "katana_idle");
            default:
                // Try to construct a path for attacks/basic folder
                if (animationName.startsWith("light_slash") || animationName.contains("slash")) {
                    ResourceLocation result = new ResourceLocation("nichirin", "attacks/basic/" + animationName);
                    return result;
                }
                // Default path
                ResourceLocation result = new ResourceLocation("nichirin", "attacks/basic/" + animationName);
                return result;
        }
    }
}