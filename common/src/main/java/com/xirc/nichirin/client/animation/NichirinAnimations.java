package com.xirc.nichirin.client.animation;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

@Environment(EnvType.CLIENT)
public class NichirinAnimations {

    /**
     * Initialize the animation system
     */
    public static void init() {
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register(NichirinAnimations::onPlayerAnimationRegister);
        System.out.println("[NichirinAnimations] Animation system initialized!");

        // Delay bone scanning until player is available
        scheduleDelayedScan();
    }

    /**
     * Schedule bone scanning for when player is available
     */
    private static void scheduleDelayedScan() {
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Wait 5 seconds for game to load
                scanForBoneNames();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Scan and log available bone names and animation registry
     */
    private static void scanForBoneNames() {
        System.out.println("[NichirinAnimations] === ANIMATION DIAGNOSTIC ===");

        // Check what animations are actually loaded with exact keys
        System.out.println("[NichirinAnimations] Loaded animations in registry:");
        var allAnimations = PlayerAnimationRegistry.getAnimations();
        if (allAnimations.isEmpty()) {
            System.out.println("  No animations found in registry!");
        } else {
            for (var entry : allAnimations.entrySet()) {
                ResourceLocation key = entry.getKey();
                System.out.println("  ✓ Full key: " + key + " (namespace: '" + key.getNamespace() + "', path: '" + key.getPath() + "')");
            }
        }

        // Test exact lookups with what we expect
        System.out.println("[NichirinAnimations] Testing exact lookups:");
        String[] testNames = {"sword.slash", "sword.doubleslash", "sword.vertical"};

        for (String name : testNames) {
            ResourceLocation loc = new ResourceLocation("nichirin", name);
            var animation = PlayerAnimationRegistry.getAnimation(loc);
            System.out.println("  Looking for: " + loc + " -> " + (animation != null ? "FOUND" : "NOT FOUND"));
        }

        // Test bone names
        testBoneNames();

        System.out.println("[NichirinAnimations] === END DIAGNOSTIC ===");
    }

    /**
     * Test bone names by attempting to play a simple test animation
     */
    private static void testBoneNames() {
        System.out.println("[NichirinAnimations] Testing bone names by checking sword.slash (working animation)...");

        // Get the working animation to see what bone names it uses
        var workingAnimation = PlayerAnimationRegistry.getAnimation(new ResourceLocation("nichirin", "sword.slash"));
        if (workingAnimation != null) {
            System.out.println("[NichirinAnimations] sword.slash animation found - this means these bone names work:");
            System.out.println("  Check your sword.slash.json file to see the exact bone names it uses");
            System.out.println("  Then use those EXACT same bone names in sword.doubleslash and sword.vertical");
        } else {
            System.out.println("[NichirinAnimations] sword.slash not found - this shouldn't happen since it works");
        }

        System.out.println("[NichirinAnimations] Common bone name variations to try:");
        String[] boneNameSets = {
                "Head, Body, Right Arm, Left Arm, Right Leg, Left Leg",
                "head, body, right_arm, left_arm, right_leg, left_leg",
                "head, torso, rightArm, leftArm, rightLeg, leftLeg",
                "HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG"
        };

        for (String boneSet : boneNameSets) {
            System.out.println("  - " + boneSet);
        }
    }

    /**
     * Called when a player's animation system is initialized
     */
    private static void onPlayerAnimationRegister(AbstractClientPlayer player, dev.kosmx.playerAnim.api.layered.AnimationStack animationStack) {
        ModifierLayer<IAnimation> animationLayer = new ModifierLayer<>();
        animationStack.addAnimLayer(0, animationLayer);

        var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
        playerData.set(new ResourceLocation("nichirin", "animation_layer"), animationLayer);
    }

    /**
     * Main method to play animations
     */
    public static void playAnimation(Player player, String animationName) {
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || !minecraft.player.equals(player)) {
            return;
        }

        try {
            // Find the animation
            KeyframeAnimation animation = findAnimation(animationName);
            if (animation == null) return;

            // Create animation player - no custom easing needed, it's in the JSON
            KeyframeAnimationPlayer animationPlayer = new KeyframeAnimationPlayer(animation);

            // Play the animation
            playAnimationDirect(clientPlayer, animationPlayer);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Find animation by name, trying multiple paths
     */
    private static KeyframeAnimation findAnimation(String animationName) {
        System.out.println("[NichirinAnimations] findAnimation called for: " + animationName);

        // Try direct lookup first
        ResourceLocation directLoc = new ResourceLocation("nichirin", animationName);
        KeyframeAnimation directResult = PlayerAnimationRegistry.getAnimation(directLoc);
        if (directResult != null) {
            System.out.println("[NichirinAnimations] Found animation '" + animationName + "' at direct path: " + animationName);
            return directResult;
        } else {
            System.out.println("[NichirinAnimations] Direct lookup failed for: " + animationName);
        }

        // Try common subdirectory patterns
        String[] paths = {
                "attacks/basic/" + animationName,
                "attacks/" + animationName,
                "basic/" + animationName,
                "combat/" + animationName,
                "sword/" + animationName,
                "katana/" + animationName,
                "special/" + animationName,
                "moves/" + animationName,
                animationName.replace("_", "/"),
                "attacks/basic/" + animationName.replace("_", "/"),
                "basic/" + animationName.replace("_", "/")
        };

        for (String path : paths) {
            ResourceLocation loc = new ResourceLocation("nichirin", path);
            KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(loc);
            if (animation != null) {
                System.out.println("[NichirinAnimations] Found animation '" + animationName + "' at path: " + path);
                return animation;
            }
        }

        System.out.println("[NichirinAnimations] Animation '" + animationName + "' not found in any path");
        return null;
    }

    /**
     * Play animation directly on the player
     */
    private static void playAnimationDirect(AbstractClientPlayer player, IAnimation animation) {
        try {
            var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var animationLayer = (ModifierLayer<IAnimation>) playerData.get(new ResourceLocation("nichirin", "animation_layer"));

            if (animationLayer != null) {
                IAnimation currentAnim = animationLayer.getAnimation();
                if (currentAnim != null) {
                    // Smooth transition between animations
                    animationLayer.replaceAnimationWithFade(
                            AbstractFadeModifier.standardFadeIn(3, Ease.INOUTSINE),
                            animation
                    );
                } else {
                    animationLayer.setAnimation(animation);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop current animation
     */
    public static void stopAnimation(AbstractClientPlayer player) {
        try {
            var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var animationLayer = (ModifierLayer<IAnimation>) playerData.get(new ResourceLocation("nichirin", "animation_layer"));

            if (animationLayer != null) {
                animationLayer.setAnimation(null);
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

    /**
     * Check if animation is playing
     */
    public static boolean isAnimationPlaying(AbstractClientPlayer player) {
        try {
            var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var animationLayer = (ModifierLayer<IAnimation>) playerData.get(new ResourceLocation("nichirin", "animation_layer"));

            if (animationLayer != null) {
                IAnimation currentAnim = animationLayer.getAnimation();
                return currentAnim != null && currentAnim.isActive();
            }
        } catch (Exception e) {
            // Silent fail
        }
        return false;
    }
}