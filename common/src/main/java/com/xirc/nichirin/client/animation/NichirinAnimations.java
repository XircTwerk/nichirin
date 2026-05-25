package com.xirc.nichirin.client.animation;

import com.xirc.nichirin.common.util.INichirinAnimatedPlayer;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class NichirinAnimations {

    private static final Logger LOGGER = LoggerFactory.getLogger("NichirinAnimations");

    // No init() needed — layer registration is handled by AbstractClientPlayerMixin.
    public static void init() {}

    public static void playAnimation(Player player, String animationName) {
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) return;

        if (animationName == null || animationName.isEmpty()) {
            stopAnimation(clientPlayer);
            return;
        }

        try {
            KeyframeAnimation animation = findAnimation(animationName);
            if (animation == null) {
                LOGGER.error("[Nichirin] Could not find animation '{}' for player '{}' — check that the animation JSON is registered under the correct resource path.", animationName, player.getScoreboardName());
                return;
            }

            ModifierLayer<IAnimation> layer = getLayer(clientPlayer);
            if (layer == null) {
                LOGGER.error("[Nichirin] Animation layer is null for player '{}' — mixin may not have fired.", player.getScoreboardName());
                return;
            }

            layer.setAnimation(new KeyframeAnimationPlayer(animation));

        } catch (Exception e) {
            LOGGER.error("[Nichirin] Exception while playing animation '{}': {}", animationName, e.getMessage(), e);
        }
    }

    private static KeyframeAnimation findAnimation(String animationName) {
        // Try direct lookup first (e.g. "nichirin:water/first_form")
        ResourceLocation directLoc = new ResourceLocation("nichirin", animationName);
        KeyframeAnimation result = PlayerAnimationRegistry.getAnimation(directLoc);
        if (result != null) return result;

        String[] prefixes = {
                "attacks/basic/",
                "attacks/demon/basic/",
                "attacks/katana/basic/",
                "attacks/",
                "basic/",
                "combat/",
                "sword/",
                "katana/",
                "special/",
                "moves/"
        };

        for (String prefix : prefixes) {
            result = PlayerAnimationRegistry.getAnimation(new ResourceLocation("nichirin", prefix + animationName));
            if (result != null) return result;
        }

        // Last-ditch: replace underscores with slashes
        result = PlayerAnimationRegistry.getAnimation(new ResourceLocation("nichirin", animationName.replace("_", "/")));
        if (result != null) return result;

        LOGGER.warn("[Nichirin] Animation '{}' not found in any known path. Tried: nichirin:{} and {} prefix variants.", animationName, animationName, prefixes.length);
        return null;
    }

    public static void stopAnimation(AbstractClientPlayer player) {
        ModifierLayer<IAnimation> layer = getLayer(player);
        if (layer != null) {
            layer.setAnimation(null);
        }
    }

    public static boolean isAnimationPlaying(AbstractClientPlayer player) {
        ModifierLayer<IAnimation> layer = getLayer(player);
        if (layer == null) return false;
        IAnimation current = layer.getAnimation();
        return current != null && current.isActive();
    }

    private static ModifierLayer<IAnimation> getLayer(AbstractClientPlayer player) {
        if (player instanceof INichirinAnimatedPlayer animated) {
            return animated.nichirin_getAnimLayer();
        }
        LOGGER.warn("[Nichirin] Player '{}' does not implement INichirinAnimatedPlayer — AbstractClientPlayerMixin missing?", player.getScoreboardName());
        return null;
    }
}
