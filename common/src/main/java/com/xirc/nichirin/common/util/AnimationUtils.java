package com.xirc.nichirin.common.util;

import com.xirc.nichirin.client.BreathOfNichirinClient;
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
     * Client-side animation playing with comprehensive safety checks
     */
    @Environment(EnvType.CLIENT)
    private static void playClientAnimation(Player player, String animationName) {
        System.out.println("DEBUG: playClientAnimation() called");

        // Check if Minecraft instance is available
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            System.out.println("DEBUG: Minecraft instance is null, cannot play animation");
            return;
        }

        // Check if we're in a valid game state
        if (minecraft.level == null) {
            System.out.println("DEBUG: Minecraft level is null, cannot play animation");
            return;
        }

        // Check if client is ready
        if (!BreathOfNichirinClient.isClientReady()) {
            System.out.println("DEBUG: Client not ready, cannot play animation");
            return;
        }

        // CRITICAL: Check if LocalPlayer exists before accessing
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer == null) {
            System.out.println("DEBUG: LocalPlayer is null, cannot play animation - client may not be fully initialized");
            return;
        }

        // Additional safety check - ensure player is alive and in world
        if (localPlayer.isRemoved()) {
            System.out.println("DEBUG: LocalPlayer is removed, cannot play animation");
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

        // FIXED: Actually play the animation using NichirinAnimations
        if (player instanceof AbstractClientPlayer clientPlayer) {
            System.out.println("DEBUG: Calling NichirinAnimations.playAnimation()");
            NichirinAnimations.playAnimation(clientPlayer, animation);
        } else {
            System.err.println("DEBUG: Player is not an AbstractClientPlayer, cannot play animation");
        }
    }

    /**
     * Checks if an animation is currently playing for the player
     */
    public static boolean isAnimationPlaying(Player player, String animationName) {
        System.out.println("DEBUG: isAnimationPlaying() called for " + animationName);

        if (player == null) {
            System.out.println("DEBUG: Player is null, returning false");
            return false;
        }

        if (player.level().isClientSide) {
            return isClientAnimationPlaying(player, animationName);
        }

        // Server side - you'd check server-side animation state here
        System.out.println("DEBUG: Server side animation check - returning false for now");
        return false;
    }

    /**
     * Client-side animation playing check with safety checks
     */
    @Environment(EnvType.CLIENT)
    private static boolean isClientAnimationPlaying(Player player, String animationName) {
        System.out.println("DEBUG: isClientAnimationPlaying() called");

        // Safety checks similar to playClientAnimation
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            System.out.println("DEBUG: Client not in valid state, returning false");
            return false;
        }

        if (!BreathOfNichirinClient.isClientReady()) {
            System.out.println("DEBUG: Client not ready, returning false");
            return false;
        }

        // TODO: Implement actual animation state checking
        // For now, always return false
        System.out.println("DEBUG: Animation state check not implemented, returning false");
        return false;
    }

    /**
     * Stops an animation for the player
     */
    public static void stopAnimation(Player player, String animationName) {
        System.out.println("DEBUG: stopAnimation() called for " + animationName);

        if (player == null) {
            System.out.println("DEBUG: Player is null, cannot stop animation");
            return;
        }

        if (player.level().isClientSide) {
            stopClientAnimation(player, animationName);
        } else {
            System.out.println("DEBUG: Server side - would send stop packet to client");
        }
    }

    /**
     * Client-side animation stopping with safety checks
     */
    @Environment(EnvType.CLIENT)
    private static void stopClientAnimation(Player player, String animationName) {
        System.out.println("DEBUG: stopClientAnimation() called");

        // Safety checks
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            System.out.println("DEBUG: Client not in valid state, cannot stop animation");
            return;
        }

        if (!BreathOfNichirinClient.isClientReady()) {
            System.out.println("DEBUG: Client not ready, cannot stop animation");
            return;
        }

        // FIXED: Actually stop the animation using NichirinAnimations
        if (player instanceof AbstractClientPlayer clientPlayer) {
            System.out.println("DEBUG: Calling NichirinAnimations.stopAnimation()");
            NichirinAnimations.stopAnimation(clientPlayer);
        }
    }

    /**
     * Gets the current animation playing for the player
     */
    public static String getCurrentAnimation(Player player) {
        System.out.println("DEBUG: getCurrentAnimation() called");

        if (player == null) {
            System.out.println("DEBUG: Player is null, returning null");
            return null;
        }

        if (player.level().isClientSide) {
            return getCurrentClientAnimation(player);
        }

        // Server side
        System.out.println("DEBUG: Server side - returning null for now");
        return null;
    }

    /**
     * Client-side current animation check with safety checks
     */
    @Environment(EnvType.CLIENT)
    private static String getCurrentClientAnimation(Player player) {
        System.out.println("DEBUG: getCurrentClientAnimation() called");

        // Safety checks
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            System.out.println("DEBUG: Client not in valid state, returning null");
            return null;
        }

        if (!BreathOfNichirinClient.isClientReady()) {
            System.out.println("DEBUG: Client not ready, returning null");
            return null;
        }

        // TODO: Implement actual current animation retrieval
        System.out.println("DEBUG: Current animation check not implemented, returning null");
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
        System.out.println("DEBUG: Mapping animation name: " + animationName);

        // Handle different naming conventions
        switch (animationName.toLowerCase()) {
            case "light_slash_1":
            case "light-slash-1":
                System.out.println("DEBUG: Mapped to sword.slash");
                return new ResourceLocation("nichirin", "attacks/basic/sword.slash");
            case "light_slash_2":
            case "light-slash-2":
                System.out.println("DEBUG: Mapped to sword.slash2");
                return new ResourceLocation("nichirin", "attacks/basic/sword.slash2");
            case "katana_idle":
                System.out.println("DEBUG: Mapped to KATANA_IDLE");
                return new ResourceLocation("nichirin", "katana_idle");
            default:
                // Try to construct a path for attacks/basic folder
                if (animationName.startsWith("light_slash") || animationName.contains("slash")) {
                    ResourceLocation result = new ResourceLocation("nichirin", "attacks/basic/" + animationName);
                    System.out.println("DEBUG: Mapped slash animation to: " + result);
                    return result;
                }
                // Default path
                ResourceLocation result = new ResourceLocation("nichirin", "attacks/basic/" + animationName);
                System.out.println("DEBUG: Mapped to default path: " + result);
                return result;
        }
    }
}