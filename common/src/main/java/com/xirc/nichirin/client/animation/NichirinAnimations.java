package com.xirc.nichirin.client.animation;

import com.xirc.nichirin.common.util.AnimationUtils;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.api.EnvType;
import org.jetbrains.annotations.NotNull;

/**
 * Manages player animations for the Nichirin mod with easing support
 */
@Environment(EnvType.CLIENT)
public class NichirinAnimations {

    // Animation identifiers - updated to match actual file names
    public static final ResourceLocation LIGHT_SLASH_1 = new ResourceLocation("nichirin", "attacks/basic/sword.slash");
    public static final ResourceLocation LIGHT_SLASH_2 = new ResourceLocation("nichirin", "attacks/basic/sword.slash2");

    /**
     * Custom animation wrapper that applies easing to existing animations
     */
    public static class EasedAnimationPlayer implements IAnimation {
        private final KeyframeAnimationPlayer basePlayer;
        private final AnimationUtils.AnimationEasingConfig easingConfig;
        private final float originalLength;
        private float currentTick = 0f;

        public EasedAnimationPlayer(KeyframeAnimation animation, AnimationUtils.AnimationEasingConfig easingConfig) {
            this.basePlayer = new KeyframeAnimationPlayer(animation);
            this.easingConfig = easingConfig;
            this.originalLength = animation.endTick;
        }

        @Override
        public void tick() {
            currentTick++;
            basePlayer.tick();
        }

        @Override
        public boolean isActive() {
            return basePlayer.isActive();
        }

        @Override
        @NotNull
        public Vec3f get3DTransform(@NotNull String modelName, @NotNull TransformType type, float tickDelta, @NotNull Vec3f value0) {
            // Apply easing to the animation progress before getting transform
            float progress = getEasedProgress(tickDelta);

            // Create a modified tickDelta based on our easing
            float easedTickDelta = progress;

            return basePlayer.get3DTransform(modelName, type, easedTickDelta, value0);
        }

        @Override
        public void setupAnim(float tickDelta) {
            float easedTickDelta = getEasedProgress(tickDelta);
            basePlayer.setupAnim(easedTickDelta);
        }

        @Override
        @NotNull
        public FirstPersonMode getFirstPersonMode(float tickDelta) {
            return FirstPersonMode.THIRD_PERSON_MODEL;
        }

        @Override
        @NotNull
        public FirstPersonConfiguration getFirstPersonConfiguration(float tickDelta) {
            return basePlayer.getFirstPersonConfiguration(tickDelta);
        }

        /**
         * Calculates eased progress based on current animation state
         */
        private float getEasedProgress(float tickDelta) {
            if (originalLength <= 0) return tickDelta;

            float totalProgress = (currentTick + tickDelta) / originalLength;
            totalProgress = Math.max(0f, Math.min(1f, totalProgress));

            // Apply easing function
            double easedProgress = easingConfig.apply(totalProgress);

            return (float) easedProgress;
        }

        /**
         * Gets the underlying animation player for advanced operations
         */
        public KeyframeAnimationPlayer getBasePlayer() {
            return basePlayer;
        }
    }

    /**
     * This method should be called during client initialization to set up the animation system
     */
    public static void init() {
        // Register event listener for when players are created
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register(NichirinAnimations::onPlayerAnimationRegister);

        // Call debug method to test what's actually registered
        debugAnimationRegistry();
    }

    /**
     * Debug method to test animation loading
     */
    public static void debugAnimationRegistry() {
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
        } catch (Exception e) {
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
     * NEW METHOD: Plays animation with easing support
     * This is called from AnimationUtils.playAnimationWithEasing
     */
    public static void playAnimationWithEasing(AbstractClientPlayer player, ResourceLocation animationId, AnimationUtils.AnimationEasingConfig easingConfig) {
        // Safety checks
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        if (!minecraft.player.equals(player)) {
            return;
        }

        try {
            // Get the animation from registry
            var animation = PlayerAnimationRegistry.getAnimation(animationId);

            if (animation == null) {
                // Try alternative paths
                animation = tryAlternativePaths(animationId);
                if (animation == null) {
                    return;
                }
            }

            // Create eased animation player
            EasedAnimationPlayer easedPlayer = new EasedAnimationPlayer(animation, easingConfig);

            // Play the eased animation
            playAnimationDirect(player, easedPlayer);

        } catch (NullPointerException e) {
            // Silent fail for null pointer
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Plays a katana slash animation
     */
    public static void playSlashAnimation(AbstractClientPlayer player, int slashNumber) {
        ResourceLocation animationId = slashNumber == 1 ? LIGHT_SLASH_1 : LIGHT_SLASH_2;
        playAnimation(player, animationId);
    }

    /**
     * Core animation playing method with safety checks (no easing)
     */
    public static void playAnimation(AbstractClientPlayer player, ResourceLocation animationId) {
        // Safety checks
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        if (!minecraft.player.equals(player)) {
            return;
        }

        try {
            // Get the animation from registry - this returns KeyframeAnimation
            var animation = PlayerAnimationRegistry.getAnimation(animationId);

            if (animation == null) {
                // Try alternative paths
                animation = tryAlternativePaths(animationId);
                if (animation == null) {
                    return;
                }
            }

            // Play the animation directly - pass KeyframeAnimation
            playAnimationDirect(player, animation);

        } catch (NullPointerException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Plays an animation directly with the provided KeyframeAnimation object
     */
    public static void playAnimationDirect(AbstractClientPlayer player, KeyframeAnimation animation) {
        try {
            // Get the stored animation layer
            var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var animationLayer = (ModifierLayer<IAnimation>) playerData.get(new ResourceLocation("nichirin", "animation_layer"));

            if (animationLayer != null) {
                // Create animation player with proper configuration
                var animationPlayer = new KeyframeAnimationPlayer(animation);

                // Check if there's already an animation playing and use fade transition
                IAnimation currentAnim = animationLayer.getAnimation();
                if (currentAnim != null) {
                    // Use replaceAnimationWithFade for smooth transition
                    animationLayer.replaceAnimationWithFade(
                            AbstractFadeModifier.standardFadeIn(5, Ease.INOUTSINE),
                            animationPlayer
                    );
                } else {
                    // First animation, just set it directly
                    animationLayer.setAnimation(animationPlayer);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Overloaded method to play an animation from an IAnimation interface
     */
    public static void playAnimationDirect(AbstractClientPlayer player, IAnimation animation) {
        try {
            // Get the stored animation layer
            var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var animationLayer = (ModifierLayer<IAnimation>) playerData.get(new ResourceLocation("nichirin", "animation_layer"));

            if (animationLayer != null) {
                // Check if there's already an animation playing and use fade transition
                IAnimation currentAnim = animationLayer.getAnimation();
                if (currentAnim != null) {
                    // Use replaceAnimationWithFade for smooth transition
                    animationLayer.replaceAnimationWithFade(
                            AbstractFadeModifier.standardFadeIn(5, Ease.INOUTSINE),
                            animation
                    );
                } else {
                    // First animation, just set it directly
                    animationLayer.setAnimation(animation);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Advanced easing method with custom parameters
     */
    public static void playAnimationWithAdvancedEasing(AbstractClientPlayer player, ResourceLocation animationId,
                                                       AnimationUtils.EasingType easingType, float speedMultiplier, int fadeInTicks) {
        try {
            var animation = PlayerAnimationRegistry.getAnimation(animationId);
            if (animation == null) {
                animation = tryAlternativePaths(animationId);
                if (animation == null) return;
            }

            // Create easing config
            AnimationUtils.AnimationEasingConfig easingConfig = createEasingConfig(easingType);

            // Create eased player with speed modification
            EasedAnimationPlayer easedPlayer = new EasedAnimationPlayer(animation, easingConfig) {
                @Override
                public void tick() {
                    // Apply speed multiplier
                    for (int i = 0; i < Math.max(1, (int) speedMultiplier); i++) {
                        super.tick();
                    }
                }
            };

            // Get animation layer and play with custom fade
            var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var animationLayer = (ModifierLayer<IAnimation>) playerData.get(new ResourceLocation("nichirin", "animation_layer"));

            if (animationLayer != null) {
                IAnimation currentAnim = animationLayer.getAnimation();
                if (currentAnim != null) {
                    animationLayer.replaceAnimationWithFade(
                            AbstractFadeModifier.standardFadeIn(fadeInTicks, mapEasingTypeToPlayerAnimEase(easingType)),
                            easedPlayer
                    );
                } else {
                    animationLayer.setAnimation(easedPlayer);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Maps our easing types to PlayerAnim's Ease enum
     */
    private static Ease mapEasingTypeToPlayerAnimEase(AnimationUtils.EasingType easingType) {
        return switch (easingType) {
            case LINEAR -> Ease.LINEAR;
            case EASE_IN -> Ease.INSINE;
            case EASE_OUT -> Ease.OUTSINE;
            case EASE_IN_OUT -> Ease.INOUTSINE;
            case EASE_IN_CUBIC -> Ease.INCUBIC;
            case EASE_OUT_CUBIC -> Ease.OUTCUBIC;
            case EASE_IN_OUT_CUBIC -> Ease.INOUTCUBIC;
            case EASE_IN_BACK -> Ease.INBACK;
            case EASE_OUT_BACK -> Ease.OUTBACK;
            case EASE_IN_OUT_BACK -> Ease.INOUTBACK;
        };
    }

    /**
     * Creates easing configuration based on easing type
     */
    private static AnimationUtils.AnimationEasingConfig createEasingConfig(AnimationUtils.EasingType easing) {
        return switch (easing) {
            case LINEAR -> new AnimationUtils.AnimationEasingConfig(t -> t);
            case EASE_IN -> new AnimationUtils.AnimationEasingConfig(t -> t * t);
            case EASE_OUT -> new AnimationUtils.AnimationEasingConfig(t -> 1 - (1 - t) * (1 - t));
            case EASE_IN_OUT -> new AnimationUtils.AnimationEasingConfig(t ->
                    t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2);
            case EASE_IN_CUBIC -> new AnimationUtils.AnimationEasingConfig(t -> t * t * t);
            case EASE_OUT_CUBIC -> new AnimationUtils.AnimationEasingConfig(t ->
                    1 - Math.pow(1 - t, 3));
            case EASE_IN_OUT_CUBIC -> new AnimationUtils.AnimationEasingConfig(t ->
                    t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);
            case EASE_IN_BACK -> new AnimationUtils.AnimationEasingConfig(t -> {
                double c1 = 1.70158;
                double c3 = c1 + 1;
                return c3 * t * t * t - c1 * t * t;
            });
            case EASE_OUT_BACK -> new AnimationUtils.AnimationEasingConfig(t -> {
                double c1 = 1.70158;
                double c3 = c1 + 1;
                return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
            });
            case EASE_IN_OUT_BACK -> new AnimationUtils.AnimationEasingConfig(t -> {
                double c1 = 1.70158;
                double c2 = c1 * 1.525;
                return t < 0.5
                        ? (Math.pow(2 * t, 2) * ((c2 + 1) * 2 * t - c2)) / 2
                        : (Math.pow(2 * t - 2, 2) * ((c2 + 1) * (t * 2 - 2) + c2) + 2) / 2;
            });
        };
    }

    /**
     * Tries alternative animation paths when the primary path fails
     */
    private static KeyframeAnimation tryAlternativePaths(ResourceLocation animationId) {
        String animName = animationId.getPath();
        if (animName.contains("/")) {
            animName = animName.substring(animName.lastIndexOf("/") + 1);
        }

        // Try without the folder structure
        ResourceLocation altId1 = new ResourceLocation("nichirin", animName);
        var altAnim1 = PlayerAnimationRegistry.getAnimation(altId1);
        if (altAnim1 != null) {
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
                return testAnim;
            }
        }

        return null;
    }

    /**
     * Maps animation names to correct resource locations
     */
    public static ResourceLocation mapAnimationName(String animationName) {
        switch (animationName.toLowerCase()) {
            case "light_slash_1":
            case "light-slash-1":
            case "sword.slash":
                return new ResourceLocation("nichirin", "attacks/basic/sword.slash");
            case "light_slash_2":
            case "light-slash-2":
            case "sword.slash2":
                return new ResourceLocation("nichirin", "attacks/basic/sword.slash2");
            default:
                // Try to construct a path for attacks/basic folder
                if (animationName.startsWith("light_slash") || animationName.contains("slash")) {
                    ResourceLocation result = new ResourceLocation("nichirin", "attacks/basic/" + animationName);
                    return result;
                }
                // Default path
                ResourceLocation result = new ResourceLocation("nichirin", "attacks/basic/" + animationName);
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
            }
        } catch (NullPointerException e) {
        } catch (Exception e) {
        }
    }

    /**
     * Utility method to check if an animation is currently playing
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

    /**
     * Gets the current animation progress (0.0 to 1.0)
     */
    public static float getCurrentAnimationProgress(AbstractClientPlayer player) {
        try {
            var playerData = PlayerAnimationAccess.getPlayerAssociatedData(player);
            var animationLayer = (ModifierLayer<IAnimation>) playerData.get(new ResourceLocation("nichirin", "animation_layer"));

            if (animationLayer != null) {
                IAnimation currentAnim = animationLayer.getAnimation();
                if (currentAnim instanceof EasedAnimationPlayer easedPlayer) {
                    return easedPlayer.getEasedProgress(0f);
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
        return 0f;
    }
}