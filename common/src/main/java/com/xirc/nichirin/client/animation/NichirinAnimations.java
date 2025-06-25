// First, update your NichirinAnimations class to match your folder structure:

package com.xirc.nichirin.client.animation;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Manages player animations for the Nichirin mod
 */
@Environment(EnvType.CLIENT)
public class NichirinAnimations {

    // Animation identifiers - updated to match your folder structure
    public static final ResourceLocation LIGHT_SLASH_1 = new ResourceLocation("nichirin", "attacks.basic.light_slash_1");
    public static final ResourceLocation LIGHT_SLASH_2 = new ResourceLocation("nichirin", "attacks.basic.light_slash_2");
    //public static final ResourceLocation KATANA_IDLE = new ResourceLocation("nichirin", "katana_idle");
    //public static final ResourceLocation KATANA_WALK = new ResourceLocation("nichirin", "katana_walk");
    //public static final ResourceLocation KATANA_BLOCK = new ResourceLocation("nichirin", "katana_block");

    /**
     * This method should be called during client initialization to set up the animation system
     * The actual animations are loaded from JSON files in assets/nichirin/player_animation/
     */
    public static void init() {
        System.out.println("DEBUG: Initializing NichirinAnimations");

        // Register event listener for when players are created
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register(NichirinAnimations::onPlayerAnimationRegister);

        // Debug: List what animations are registered
        System.out.println("DEBUG: Expected animation paths:");
        System.out.println("  - " + LIGHT_SLASH_1);
        System.out.println("  - " + LIGHT_SLASH_2);
    }

    /**
     * Debug method to test animation loading
     */
    public static void debugAnimationRegistry() {
        System.out.println("DEBUG: Testing animation registry...");

        // Test various possible paths
        String[] testPaths = {
                "nichirin:light_slash_2",
                "nichirin:attacks.basic.light_slash_2",
                "nichirin:attacks/basic/light_slash_2",
                "nichirin:basic.light_slash_2",
                "nichirin:basic/light_slash_2"
        };

        for (String path : testPaths) {
            ResourceLocation testId = new ResourceLocation(path);
            var animation = PlayerAnimationRegistry.getAnimation(testId);
            System.out.println("  " + path + ": " + (animation != null ? "FOUND" : "NOT FOUND"));
        }
    }

    /**
     * Called when a player's animation system is initialized
     * This is where you set up the animation layer for the player
     */
    private static void onPlayerAnimationRegister(AbstractClientPlayer player, dev.kosmx.playerAnim.api.layered.AnimationStack animationStack) {
        System.out.println("DEBUG: Registering animation layer for player: " + player.getName().getString());

        // Create a single ModifierLayer for all animations
        ModifierLayer<IAnimation> animationLayer = new ModifierLayer<>();

        // Add the layer at priority 0 (you can adjust this if needed)
        animationStack.addAnimLayer(0, animationLayer);

        // Store the layer reference for later use
        var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
        playerData.set(new ResourceLocation("nichirin", "animation_layer"), animationLayer);
    }

    /**
     * Plays a katana slash animation
     * @param player The player
     * @param slashNumber Which slash animation to play (1 or 2)
     */
    public static void playSlashAnimation(AbstractClientPlayer player, int slashNumber) {
        ResourceLocation animationId = slashNumber == 1 ? LIGHT_SLASH_1 : LIGHT_SLASH_2;
        playAnimation(player, animationId);
    }

    /**
     * Core animation playing method
     * @param player The player to animate
     * @param animationId The animation resource location
     */
    public static void playAnimation(AbstractClientPlayer player, ResourceLocation animationId) {
        System.out.println("DEBUG: Attempting to play animation: " + animationId);

        var animation = PlayerAnimationRegistry.getAnimation(animationId);

        if (animation == null) {
            System.err.println("Animation not found: " + animationId);

            // Debug: Try to find what animations ARE registered
            System.out.println("DEBUG: Trying alternative paths...");

            // Try without the folder structure
            ResourceLocation altId1 = new ResourceLocation("nichirin", "light_slash_2");
            var altAnim1 = PlayerAnimationRegistry.getAnimation(altId1);
            if (altAnim1 != null) {
                System.out.println("DEBUG: Found animation at: " + altId1);
                animation = altAnim1;
                animationId = altId1;
            } else {
                // Try with underscores replaced by dots
                ResourceLocation altId2 = new ResourceLocation("nichirin", animationId.getPath().replace("_", "."));
                var altAnim2 = PlayerAnimationRegistry.getAnimation(altId2);
                if (altAnim2 != null) {
                    System.out.println("DEBUG: Found animation at: " + altId2);
                    animation = altAnim2;
                    animationId = altId2;
                }
            }

            if (animation == null) {
                System.err.println("DEBUG: No alternative animation paths worked");
                return;
            }
        } else {
            System.out.println("DEBUG: Animation found successfully: " + animationId);
        }

        try {
            // Get the stored animation layer
            var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var animationLayer = (ModifierLayer<IAnimation>) playerData.get(new ResourceLocation("nichirin", "animation_layer"));

            if (animationLayer != null) {
                System.out.println("DEBUG: Playing animation on layer");

                // Create animation player
                var animationPlayer = new KeyframeAnimationPlayer(animation)
                        .setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);

                // Set the animation on the layer
                animationLayer.setAnimation(animationPlayer);
                System.out.println("DEBUG: Animation set successfully");
            } else {
                System.err.println("DEBUG: Animation layer not found for player");
            }
        } catch (Exception e) {
            System.err.println("Failed to play animation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Stops the current animation
     * @param player The player
     */
    public static void stopAnimation(AbstractClientPlayer player) {
        try {
            var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var animationLayer = (ModifierLayer<IAnimation>) playerData.get(new ResourceLocation("nichirin", "animation_layer"));

            if (animationLayer != null) {
                animationLayer.setAnimation(null);
            }
        } catch (Exception e) {
            System.err.println("Failed to stop animation: " + e.getMessage());
        }
    }
}
