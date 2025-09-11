package com.xirc.nichirin.common.util;

import com.xirc.nichirin.client.BreathOfNichirinClient;
import com.xirc.nichirin.client.animation.NichirinAnimations;
import com.xirc.nichirin.common.network.s2c.PlayerAnimationPacket;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class AnimationUtils {

    /**
     * Animation easing types for smooth transitions
     */
    public enum EasingType {
        LINEAR,
        EASE_IN,
        EASE_OUT,
        EASE_IN_OUT,
        EASE_IN_CUBIC,
        EASE_OUT_CUBIC,
        EASE_IN_OUT_CUBIC,
        EASE_IN_BACK,
        EASE_OUT_BACK,
        EASE_IN_OUT_BACK
    }

    /**
     * Default easing configuration for different animation types
     */
    private static final EasingType DEFAULT_COMBAT_EASING = EasingType.EASE_OUT_CUBIC;
    private static final EasingType DEFAULT_MOVEMENT_EASING = EasingType.EASE_IN_OUT;
    private static final EasingType DEFAULT_SPECIAL_EASING = EasingType.EASE_IN_OUT_BACK;

    /**
     * Plays an animation with automatic easing based on animation type
     */
    public static void playAnimation(Player player, String animationName) {
        playAnimation(player, animationName, getDefaultEasing(animationName));
    }

    /**
     * Plays an animation with specified easing
     */
    public static void playAnimation(Player player, String animationName, EasingType easing) {
        if (player == null) return;

        if (player.level().isClientSide) {
      //      playClientAnimation(player, animationName, easing);
        } else {
            // Server side - send packet to client
            if (player instanceof ServerPlayer serverPlayer) {
                PlayerAnimationPacket packet = new PlayerAnimationPacket(player.getId(), animationName, easing);
                NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
            }
        }
    }

    /**
     * Determines default easing based on animation name
     */
    private static EasingType getDefaultEasing(String animationName) {
        String lowerName = animationName.toLowerCase();

        // Combat animations - snappy and impactful
        if (lowerName.contains("slash") || lowerName.contains("attack") || lowerName.contains("sword")) {
            return DEFAULT_COMBAT_EASING;
        }

        // Special/breathing techniques - dramatic with overshoot
        if (lowerName.contains("thunder") || lowerName.contains("flame") || lowerName.contains("water") ||
                lowerName.contains("breath") || lowerName.contains("form")) {
            return DEFAULT_SPECIAL_EASING;
        }

        // Movement animations - smooth transitions
        if (lowerName.contains("walk") || lowerName.contains("run") || lowerName.contains("idle")) {
            return DEFAULT_MOVEMENT_EASING;
        }

        // Default for unknown animations
        return DEFAULT_COMBAT_EASING;
    }

    /**
     * Client-side animation playing with easing
     */
    @Environment(EnvType.CLIENT)
    private static void playClientAnimation(Player player, String animationName, EasingType easing) {
        // Safety checks
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) return;

        if (!BreathOfNichirinClient.isClientReady()) return;

        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer == null || localPlayer.isRemoved()) return;

        if (!localPlayer.equals(player)) return;

        // Play the animation with easing
        if (player instanceof AbstractClientPlayer clientPlayer) {
            ResourceLocation animLoc = mapAnimationName(animationName);

            // Apply easing curve to the animation
            playAnimationWithEasing(clientPlayer, animLoc, easing);
        }
    }

    /**
     * Plays animation with specified easing curve
     */
    @Environment(EnvType.CLIENT)
    private static void playAnimationWithEasing(AbstractClientPlayer player, ResourceLocation animLoc, EasingType easing) {
        // Create easing configuration
        AnimationEasingConfig config = createEasingConfig(easing);

        // Apply the easing to the animation system
        NichirinAnimations.playAnimationWithEasing(player, animLoc, config);
    }

    /**
     * Creates easing configuration based on easing type
     */
    @Environment(EnvType.CLIENT)
    private static AnimationEasingConfig createEasingConfig(EasingType easing) {
        return switch (easing) {
            case LINEAR -> new AnimationEasingConfig(t -> t);
            case EASE_IN -> new AnimationEasingConfig(t -> t * t);
            case EASE_OUT -> new AnimationEasingConfig(t -> 1 - (1 - t) * (1 - t));
            case EASE_IN_OUT -> new AnimationEasingConfig(t ->
                    t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2);
            case EASE_IN_CUBIC -> new AnimationEasingConfig(t -> t * t * t);
            case EASE_OUT_CUBIC -> new AnimationEasingConfig(t ->
                    1 - Math.pow(1 - t, 3));
            case EASE_IN_OUT_CUBIC -> new AnimationEasingConfig(t ->
                    t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);
            case EASE_IN_BACK -> new AnimationEasingConfig(t -> {
                double c1 = 1.70158;
                double c3 = c1 + 1;
                return c3 * t * t * t - c1 * t * t;
            });
            case EASE_OUT_BACK -> new AnimationEasingConfig(t -> {
                double c1 = 1.70158;
                double c3 = c1 + 1;
                return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
            });
            case EASE_IN_OUT_BACK -> new AnimationEasingConfig(t -> {
                double c1 = 1.70158;
                double c2 = c1 * 1.525;
                return t < 0.5
                        ? (Math.pow(2 * t, 2) * ((c2 + 1) * 2 * t - c2)) / 2
                        : (Math.pow(2 * t - 2, 2) * ((c2 + 1) * (t * 2 - 2) + c2) + 2) / 2;
            });
        };
    }

    /**
     * Easing configuration class
     */
    @Environment(EnvType.CLIENT)
    public static class AnimationEasingConfig {
        private final EasingFunction function;

        public AnimationEasingConfig(EasingFunction function) {
            this.function = function;
        }

        public double apply(double t) {
            return function.apply(Math.max(0, Math.min(1, t)));
        }

        @FunctionalInterface
        public interface EasingFunction {
            double apply(double t);
        }
    }

    // Existing methods with easing support added...

    /**
     * Checks if an animation is currently playing for the player
     */
    public static boolean isAnimationPlaying(Player player, String animationName) {
        if (player == null) return false;

        if (player.level().isClientSide) {
            return isClientAnimationPlaying(player, animationName);
        }
        return false;
    }

    @Environment(EnvType.CLIENT)
    private static boolean isClientAnimationPlaying(Player player, String animationName) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return false;
        }

        if (!BreathOfNichirinClient.isClientReady()) return false;
        return false; // TODO: Implement actual animation state checking
    }

    /**
     * Stops an animation for the player
     */
    public static void stopAnimation(Player player, String animationName) {
        if (player == null) return;

        if (player.level().isClientSide) {
            stopClientAnimation(player, animationName);
        }
    }

    @Environment(EnvType.CLIENT)
    private static void stopClientAnimation(Player player, String animationName) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null || minecraft.player == null) return;

        if (!BreathOfNichirinClient.isClientReady()) return;

        if (player instanceof AbstractClientPlayer clientPlayer) {
            NichirinAnimations.stopAnimation(clientPlayer);
        }
    }

    /**
     * Maps animation names to resource locations
     */
    private static ResourceLocation mapAnimationName(String animationName) {
        switch (animationName.toLowerCase()) {
            case "sword_slash":
            case "light_slash_1":
            case "light-slash-1":
                return new ResourceLocation("nichirin", "attacks/basic/sword.slash");
            case "light_slash_2":
            case "light-slash-2":
                return new ResourceLocation("nichirin", "attacks/basic/sword.slash2");
            case "sword_doubleslash":
            case "double_slash":
                return new ResourceLocation("nichirin", "attacks/basic/sword_doubleslash");
            case "sword_vertical":
            case "rising_slash":
                return new ResourceLocation("nichirin", "attacks/basic/sword_vertical");
            case "katana_idle":
                return new ResourceLocation("nichirin", "katana_idle");
            default:
                // Try to construct a path for attacks/basic folder
                if (animationName.startsWith("light_slash") || animationName.contains("slash")) {
                    return new ResourceLocation("nichirin", "attacks/basic/" + animationName);
                }
                // Default path
                return new ResourceLocation("nichirin", "attacks/basic/" + animationName);
        }
    }
}