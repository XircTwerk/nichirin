package com.xirc.nichirin.client.animation;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
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
    public static final ResourceLocation LIGHT_SLASH_1 = new ResourceLocation("nichirin", "attacks/basic/sword.slash");
    public static final ResourceLocation LIGHT_SLASH_2 = new ResourceLocation("nichirin", "attacks/basic/sword.slash2");

    /**
     * This method should be called during client initialization to set up the animation system
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
     */
    private static void onPlayerAnimationRegister(AbstractClientPlayer player, dev.kosmx.playerAnim.api.layered.AnimationStack animationStack) {
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

        // Add the layer at priority 0
        animationStack.addAnimLayer(0, animationLayer);

        // Store the layer reference for later use
        var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
        playerData.set(new ResourceLocation("nichirin", "animation_layer"), animationLayer);
    }

    /**
     * Plays a katana slash animation
     */
    public static void playSlashAnimation(AbstractClientPlayer player, int slashNumber) {
        ResourceLocation animationId = slashNumber == 1 ? LIGHT_SLASH_1 : LIGHT_SLASH_2;
        playAnimation(player, animationId);
    }

    /**
     * Core animation playing method with safety checks
     */
    public static void playAnimation(AbstractClientPlayer player, ResourceLocation animationId) {
        // Safety checks
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            System.out.println("DEBUG: Skipping animation - minecraft or player is null");
            return;
        }

        if (!minecraft.player.equals(player)) {
            System.out.println("DEBUG: Skipping animation - player mismatch");
            return;
        }

        try {
            System.out.println("DEBUG: Attempting to play animation: " + animationId);

            // Get the animation from registry - this returns KeyframeAnimation
            var animation = PlayerAnimationRegistry.getAnimation(animationId);

            if (animation == null) {
                System.err.println("Animation not found: " + animationId);

                // Try alternative paths
                animation = tryAlternativePaths(animationId);

                if (animation == null) {
                    System.err.println("DEBUG: No alternative animation paths worked");
                    return;
                }
            } else {
                System.out.println("DEBUG: Animation found successfully: " + animationId);
            }

            // Play the animation directly - pass KeyframeAnimation
            playAnimationDirect(player, animation);

        } catch (NullPointerException e) {
            System.out.println("DEBUG: Skipped animation due to null reference: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Failed to play animation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Plays an animation directly with the provided KeyframeAnimation object
     */
    public static void playAnimationDirect(AbstractClientPlayer player, KeyframeAnimation animation) {
        try {
            System.out.println("DEBUG: Playing animation directly");

            // Get the stored animation layer
            var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var animationLayer = (ModifierLayer<IAnimation>) playerData.get(new ResourceLocation("nichirin", "animation_layer"));

            if (animationLayer != null) {
                System.out.println("DEBUG: Playing animation on layer");

                // Create animation player with proper configuration
                var animationPlayer = new KeyframeAnimationPlayer(animation);
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
            } else {
                System.err.println("DEBUG: Animation layer not found for player");
            }
        } catch (Exception e) {
            System.err.println("Failed to play animation directly: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Overloaded method to play an animation from an IAnimation interface
     */
    public static void playAnimationDirect(AbstractClientPlayer player, IAnimation animation) {
        try {
            System.out.println("DEBUG: Playing IAnimation directly");

            // Get the stored animation layer
            var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var animationLayer = (ModifierLayer<IAnimation>) playerData.get(new ResourceLocation("nichirin", "animation_layer"));

            if (animationLayer != null) {
                System.out.println("DEBUG: Playing animation on layer");

                IAnimation animationToPlay = animation;

                // Check if we need to set first person mode
                if (animation instanceof KeyframeAnimationPlayer) {
                    ((KeyframeAnimationPlayer) animation).setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
                }

                // Check if there's already an animation playing and use fade transition
                IAnimation currentAnim = animationLayer.getAnimation();
                if (currentAnim != null) {
                    // Use replaceAnimationWithFade for smooth transition
                    animationLayer.replaceAnimationWithFade(
                            AbstractFadeModifier.standardFadeIn(5, Ease.INOUTSINE),
                            animationToPlay
                    );
                    System.out.println("DEBUG: Animation set with fade transition");
                } else {
                    // First animation, just set it directly
                    animationLayer.setAnimation(animationToPlay);
                    System.out.println("DEBUG: Animation set directly");
                }

                System.out.println("DEBUG: Animation configured successfully");
            } else {
                System.err.println("DEBUG: Animation layer not found for player");
            }
        } catch (Exception e) {
            System.err.println("Failed to play IAnimation directly: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Tries alternative animation paths when the primary path fails
     */
    private static KeyframeAnimation tryAlternativePaths(ResourceLocation animationId) {
        System.out.println("DEBUG: Trying alternative paths...");

        String animName = animationId.getPath();
        if (animName.contains("/")) {
            animName = animName.substring(animName.lastIndexOf("/") + 1);
        }

        // Try without the folder structure
        ResourceLocation altId1 = new ResourceLocation("nichirin", animName);
        var altAnim1 = PlayerAnimationRegistry.getAnimation(altId1);
        if (altAnim1 != null) {
            System.out.println("DEBUG: Found animation at: " + altId1);
            return altAnim1;
        }

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
                return testAnim;
            }
        }

        return null;
    }

    /**
     * Maps animation names to correct resource locations
     */
    public static ResourceLocation mapAnimationName(String animationName) {
        System.out.println("DEBUG: Mapping animation name: " + animationName);

        switch (animationName.toLowerCase()) {
            case "light_slash_1":
            case "light-slash-1":
            case "sword.slash":
                System.out.println("DEBUG: Mapped to sword.slash");
                return new ResourceLocation("nichirin", "attacks/basic/sword.slash");
            case "light_slash_2":
            case "light-slash-2":
            case "sword.slash2":
                System.out.println("DEBUG: Mapped to sword.slash2");
                return new ResourceLocation("nichirin", "attacks/basic/sword.slash2");
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

    /**
     * Stops the current animation
     */
    public static void stopAnimation(AbstractClientPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        try {
            var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var animationLayer = (ModifierLayer<IAnimation>) playerData.get(new ResourceLocation("nichirin", "animation_layer"));

            if (animationLayer != null) {
                animationLayer.setAnimation(null);
                System.out.println("DEBUG: Animation stopped");
            }
        } catch (NullPointerException e) {
            System.out.println("DEBUG: Skipped stop animation due to null reference");
        } catch (Exception e) {
            System.err.println("Failed to stop animation: " + e.getMessage());
        }
    }
}