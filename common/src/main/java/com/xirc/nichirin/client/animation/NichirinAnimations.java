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

    public static void init() {
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register(NichirinAnimations::onPlayerAnimationRegister);
    }

    private static void onPlayerAnimationRegister(AbstractClientPlayer player, dev.kosmx.playerAnim.api.layered.AnimationStack animationStack) {
        ModifierLayer<IAnimation> animationLayer = new ModifierLayer<>();
        animationStack.addAnimLayer(0, animationLayer);

        var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
        playerData.set(new ResourceLocation("nichirin", "animation_layer"), animationLayer);
    }

    public static void playAnimation(Player player, String animationName) {
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || !minecraft.player.equals(player)) {
            return;
        }

        try {
            KeyframeAnimation animation = findAnimation(animationName);
            if (animation == null) return;

            KeyframeAnimationPlayer animationPlayer = new KeyframeAnimationPlayer(animation);
            playAnimationDirect(clientPlayer, animationPlayer);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static KeyframeAnimation findAnimation(String animationName) {
        ResourceLocation directLoc = new ResourceLocation("nichirin", animationName);
        KeyframeAnimation directResult = PlayerAnimationRegistry.getAnimation(directLoc);
        if (directResult != null) {
            return directResult;
        }

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

    private static void playAnimationDirect(AbstractClientPlayer player, IAnimation animation) {
        try {
            var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var animationLayer = (ModifierLayer<IAnimation>) playerData.get(new ResourceLocation("nichirin", "animation_layer"));

            if (animationLayer != null) {
                IAnimation currentAnim = animationLayer.getAnimation();
                if (currentAnim != null) {
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

    public static void stopAnimation(AbstractClientPlayer player) {
        try {
            var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var animationLayer = (ModifierLayer<IAnimation>) playerData.get(new ResourceLocation("nichirin", "animation_layer"));

            if (animationLayer != null) {
                animationLayer.setAnimation(null);
            }
        } catch (Exception e) {
        }
    }

    public static boolean isAnimationPlaying(AbstractClientPlayer player) {
        try {
            var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var animationLayer = (ModifierLayer<IAnimation>) playerData.get(new ResourceLocation("nichirin", "animation_layer"));

            if (animationLayer != null) {
                IAnimation currentAnim = animationLayer.getAnimation();
                return currentAnim != null && currentAnim.isActive();
            }
        } catch (Exception e) {
        }
        return false;
    }
}