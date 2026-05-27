package com.xirc.nichirin.client.animation;

import com.xirc.nichirin.common.util.INichirinAnimatedPlayer;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
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

            layer.setAnimation(new KeyframeAnimationPlayer(postProcess(animationName, animation)));

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

    /**
     * Post-processes a loaded animation:
     * 1. Plain [x,y,z] arrays in JSON are parsed by PlayerAnim as Ease.CONSTANT (instant snap).
     *    We replace CONSTANT with INOUTSINE so all animations interpolate smoothly, matching
     *    the original catmullrom intent. (The linter strips lerp_mode from JSON, so this is
     *    the only reliable way to get smooth interpolation.)
     * 2. sword.block forces returnTick == endTick so it holds its last frame instead of
     *    restarting.
     */
    private static KeyframeAnimation postProcess(String animationName, KeyframeAnimation animation) {
        KeyframeAnimation.AnimationBuilder builder = animation.mutableCopy();

        for (String partName : animation.getBodyParts().keySet()) {
            KeyframeAnimation.StateCollection part = builder.getPart(partName);
            if (part == null) continue;
            fixEasing(part.pitch);
            fixEasing(part.yaw);
            fixEasing(part.roll);
            fixEasing(part.x);
            fixEasing(part.y);
            fixEasing(part.z);
            if (part.bend != null) fixEasing(part.bend);
            if (part.bendDirection != null) fixEasing(part.bendDirection);
        }

        // sword.block: force loop and hold on its last frame so it never snaps back to
        // the start. returnTick = endTick means "when done, jump to the last frame" — a freeze.
        // We guard endTick > 0 in case the linter strips animation_length again.
        if ("sword.block".equals(animationName) && builder.endTick > 0) {
            builder.isLooped   = true;
            builder.returnTick = builder.endTick;
        }

        return builder.build();
    }

    /** Replace Ease.CONSTANT (plain array default) with Ease.INOUTSINE on every keyframe. */
    private static void fixEasing(KeyframeAnimation.StateCollection.State state) {
        if (state == null) return;
        for (int i = 0; i < state.getKeyFrames().size(); i++) {
            if (state.getKeyFrames().get(i).ease == Ease.CONSTANT) {
                state.replaceEase(i, Ease.INOUTSINE);
            }
        }
    }

    private static ModifierLayer<IAnimation> getLayer(AbstractClientPlayer player) {
        if (player instanceof INichirinAnimatedPlayer animated) {
            return animated.nichirin_getAnimLayer();
        }
        LOGGER.warn("[Nichirin] Player '{}' does not implement INichirinAnimatedPlayer — AbstractClientPlayerMixin missing?", player.getScoreboardName());
        return null;
    }
}
