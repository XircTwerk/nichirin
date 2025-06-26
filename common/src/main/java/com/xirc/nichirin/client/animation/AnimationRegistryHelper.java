package com.xirc.nichirin.client.animation;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper to ensure animations are found and cached properly
 */
public class AnimationRegistryHelper {

    private static final Map<String, ResourceLocation> ANIMATION_CACHE = new HashMap<>();

    static {
        // Pre-register known animations
        registerAnimation("light_slash_1", "sword.slash");
        registerAnimation("light_slash_2", "sword.slash2");
    }

    /**
     * Registers an animation mapping
     */
    public static void registerAnimation(String name, String actualPath) {
        ANIMATION_CACHE.put(name, new ResourceLocation("nichirin", actualPath));
    }

    /**
     * Gets an animation by name, trying multiple paths
     */
    public static KeyframeAnimation getAnimation(String animationName) {
        // Check cache first
        ResourceLocation cached = ANIMATION_CACHE.get(animationName);
        if (cached != null) {
            KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(cached);
            if (anim != null) {
                System.out.println("DEBUG: Found cached animation: " + cached);
                return anim;
            }
        }

        // Try various paths
        String[] pathVariations = {
                animationName,                                    // Direct name
                "attacks/basic/" + animationName,                // In folder
                animationName.replace("_", "."),                 // Dots instead of underscores
                "sword." + animationName.replace("light_", ""),  // sword.slash format
                animationName.replace("light_slash_", "sword.slash") // Direct mapping
        };

        for (String path : pathVariations) {
            ResourceLocation loc = new ResourceLocation("nichirin", path);
            KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(loc);
            if (anim != null) {
                System.out.println("DEBUG: Found animation at: " + loc);
                // Cache for next time
                ANIMATION_CACHE.put(animationName, loc);
                return anim;
            }
        }

        System.err.println("ERROR: Could not find animation: " + animationName);
        System.err.println("Tried paths: ");
        for (String path : pathVariations) {
            System.err.println("  - nichirin:" + path);
        }

        return null;
    }

    /**
     * Pre-loads all animations to ensure they're available
     */
    public static void preloadAnimations() {
        System.out.println("DEBUG: Preloading animations...");

        // List all expected animations
        String[] expectedAnimations = {
                "light_slash_1",
                "light_slash_2",
                "sword.slash",
                "sword.slash2"
        };

        for (String animName : expectedAnimations) {
            KeyframeAnimation anim = getAnimation(animName);
            if (anim != null) {
                System.out.println("DEBUG: Successfully preloaded: " + animName);
            } else {
                System.err.println("ERROR: Failed to preload: " + animName);
            }
        }
    }
}