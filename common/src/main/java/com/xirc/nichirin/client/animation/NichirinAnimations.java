package com.xirc.nichirin.client.animation;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Manages player animations for the Nichirin mod
 */
@Environment(EnvType.CLIENT)
public class NichirinAnimations {

    // Animation identifiers - updated to match actual file names
    // The format should match: modid:folder/structure/filename (without .json)
    public static final ResourceLocation LIGHT_SLASH_1 = new ResourceLocation("nichirin", "attacks/basic/sword.slash");
    public static final ResourceLocation LIGHT_SLASH_2 = new ResourceLocation("nichirin", "attacks/basic/sword.slash2");
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

        // Call debug method to test what's actually registered
        debugAnimationRegistry();
    }

    /**
     * Debug method to test animation loading
     */
    public static void debugAnimationRegistry() {
        System.out.println("DEBUG: Testing animation registry...");

        // Test various possible paths for attack
        String[] testPaths = {
                "nichirin:sword.slash",
                "nichirin:sword.slash2",
                "nichirin:attacks/basic/sword.slash",
                "nichirin:attacks/basic/sword.slash2",
                "nichirin:basic/sword.slash",
                "nichirin:basic/sword.slash2"
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
        // FIXED: Safer debug logging that handles null cases
        try {
            String playerName = "Unknown";
            if (player != null && player.getGameProfile() != null && player.getGameProfile().getName() != null) {
                playerName = player.getGameProfile().getName();
            } else if (player != null) {
                playerName = "Player-" + player.getId();
            }
            System.out.println("DEBUG: Registering animation layer for player: " + playerName);
        } catch (Exception e) {
            System.out.println("DEBUG: Registering animation layer for player (name unavailable)");
        }

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
     * Core animation playing method - FIXED TO PREVENT CRASHES
     * @param player The player to animate
     * @param animationId The animation resource location
     */
    public static void playAnimation(AbstractClientPlayer player, ResourceLocation animationId) {
        // CRASH FIX: Exit immediately if player or minecraft instance is null
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            System.out.println("DEBUG: Skipping animation - minecraft or player is null");
            return;
        }

        // Additional safety check - make sure the provided player matches the local player
        if (!minecraft.player.equals(player)) {
            System.out.println("DEBUG: Skipping animation - player mismatch");
            return;
        }

        try {
            System.out.println("DEBUG: Attempting to play animation: " + animationId);

            var animation = PlayerAnimationRegistry.getAnimation(animationId);

            if (animation == null) {
                System.err.println("Animation not found: " + animationId);

                // Debug: Try to find what animations ARE registered
                System.out.println("DEBUG: Trying alternative paths...");

                // Extract just the animation name from the path
                String animName = animationId.getPath();
                if (animName.contains("/")) {
                    animName = animName.substring(animName.lastIndexOf("/") + 1);
                }

                // Try without the folder structure
                ResourceLocation altId1 = new ResourceLocation("nichirin", animName);
                var altAnim1 = PlayerAnimationRegistry.getAnimation(altId1);
                if (altAnim1 != null) {
                    System.out.println("DEBUG: Found animation at: " + altId1);
                    animation = altAnim1;
                } else {
                    // Try with different path combinations
                    String[] pathVariations = {
                            "attacks/basic/" + animName,
                            "basic/" + animName,
                            animName.replace(".", "/"),
                            animName.replace("_", ".")
                    };

                    for (String path : pathVariations) {
                        ResourceLocation testId = new ResourceLocation("nichirin", path);
                        var testAnim = PlayerAnimationRegistry.getAnimation(testId);
                        if (testAnim != null) {
                            System.out.println("DEBUG: Found animation at: " + testId);
                            animation = testAnim;
                            break;
                        }
                    }
                }

                if (animation == null) {
                    System.err.println("DEBUG: No alternative animation paths worked for " + animName);
                    return;
                }
            } else {
                System.out.println("DEBUG: Animation found successfully: " + animationId);
            }

            // Get the stored animation layer
            var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var animationLayer = (ModifierLayer<IAnimation>) playerData.get(new ResourceLocation("nichirin", "animation_layer"));

            if (animationLayer != null) {
                System.out.println("DEBUG: Playing animation on layer");

                // Create animation player with proper configuration
                var animationPlayer = new KeyframeAnimationPlayer(animation);

                // Configure the animation player for smooth playback
                animationPlayer.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);

                // Check if there's already an animation playing and use fade transition
                IAnimation currentAnim = animationLayer.getAnimation();
                if (currentAnim != null) {
                    // Use replaceAnimationWithFade for smooth transition
                    animationLayer.replaceAnimationWithFade(
                            AbstractFadeModifier.standardFadeIn(5, Ease.INOUTSINE),
                            animationPlayer
                    );
                    System.out.println("DEBUG: Animation set with fade transition");
                } else {
                    // First animation, just set it directly
                    animationLayer.setAnimation(animationPlayer);
                    System.out.println("DEBUG: Animation set directly");
                }

                System.out.println("DEBUG: Animation configured successfully");

                System.out.println("DEBUG: Animation set successfully");
            } else {
                System.err.println("DEBUG: Animation layer not found for player");
            }
        } catch (NullPointerException e) {
            // Just ignore null pointer exceptions to prevent crashes
            System.out.println("DEBUG: Skipped animation due to null reference: " + e.getMessage());
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
        // CRASH FIX: Exit immediately if player or minecraft instance is null
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        try {
            var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var animationLayer = (ModifierLayer<IAnimation>) playerData.get(new ResourceLocation("nichirin", "animation_layer"));

            if (animationLayer != null) {
                animationLayer.setAnimation(null);
            }
        } catch (NullPointerException e) {
            // Ignore null pointer exceptions
            System.out.println("DEBUG: Skipped stop animation due to null reference");
        } catch (Exception e) {
            System.err.println("Failed to stop animation: " + e.getMessage());
        }
    }
}