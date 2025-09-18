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
        // Try direct lookup first
        ResourceLocation directLoc = new ResourceLocation("nichirin", animationName);
        KeyframeAnimation directResult = PlayerAnimationRegistry.getAnimation(directLoc);
        if (directResult != null) {
            return directResult;
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
                return animation;
            }
        }

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