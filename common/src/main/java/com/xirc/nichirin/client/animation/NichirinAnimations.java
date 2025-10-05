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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class NichirinAnimations {

    // Store entity animation states
    private static final Map<Integer, EntityAnimationData> entityAnimations = new HashMap<>();

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
     * Play animation on a player
     */
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

    /**
     * Play animation on an entity (like NPCs) using the actual JSON files
     */
    public static void playEntityAnimation(Entity entity, String animationName) {
        if (entity.level().isClientSide) {
            try {
                // Map entity animation names to actual JSON animation names
                String jsonAnimationName = mapEntityAnimationName(animationName);

                KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(
                        new ResourceLocation("nichirin", jsonAnimationName)
                );

                if (animation == null) return;

                KeyframeAnimationPlayer animationPlayer = new KeyframeAnimationPlayer(animation);

                // Store animation data for the entity
                EntityAnimationData animData = entityAnimations.computeIfAbsent(entity.getId(), k -> new EntityAnimationData());
                animData.currentAnimation = animationPlayer;
                animData.animationName = animationName;
                animData.jsonAnimationName = jsonAnimationName;
                animData.startTime = entity.tickCount;
                animData.keyframeAnimation = animation;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Map entity animation names to actual JSON file names
     */
    private static String mapEntityAnimationName(String entityAnimName) {
        return switch (entityAnimName) {
            case "demon_punch", "gut_punch" -> "demon_gut_punch";
            case "demon_slash", "slash" -> "demon_slash";
            case "demon_kick", "kick" -> "demon_kick";
            case "demon_bite", "bite" -> "demon_bite";
            case "demon_dash_strike", "dashing_strike" -> "demon_dash_strike";
            case "demon_high_jump", "high_jump" -> "demon_high_jump";
            case "demon_stomp", "stomp" -> "demon_stomp";
            // Add more mappings as needed
            default -> entityAnimName;
        };
    }

    /**
     * Stop animation on entity
     */
    public static void stopEntityAnimation(Entity entity) {
        if (entity.level().isClientSide) {
            EntityAnimationData animData = entityAnimations.get(entity.getId());
            if (animData != null) {
                animData.currentAnimation = null;
                animData.animationName = "";
                animData.jsonAnimationName = "";
                animData.keyframeAnimation = null;
            }
        }
    }

    /**
     * Get current animation data for an entity
     */
    public static EntityAnimationData getEntityAnimationData(Entity entity) {
        return entityAnimations.get(entity.getId());
    }

    /**
     * Check if entity is playing animation
     */
    public static boolean isEntityAnimationPlaying(Entity entity) {
        EntityAnimationData animData = entityAnimations.get(entity.getId());
        return animData != null && animData.currentAnimation != null && animData.currentAnimation.isActive();
    }

    /**
     * Tick entity animations - call this from your entity renderer
     */
    public static void tickEntityAnimation(Entity entity) {
        if (!entity.level().isClientSide) return;

        EntityAnimationData animData = entityAnimations.get(entity.getId());
        if (animData != null && animData.currentAnimation != null) {
            // Tick the animation
            if (animData.currentAnimation.isActive()) {
                // Animation is still active
            } else {
                // Animation finished, clean up
                animData.currentAnimation = null;
                animData.animationName = "";
                animData.jsonAnimationName = "";
                animData.keyframeAnimation = null;
            }
        }
    }

    /**
     * Clean up entity animation data when entity is removed
     */
    public static void cleanupEntityAnimation(Entity entity) {
        entityAnimations.remove(entity.getId());
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

    /**
     * Data class to store entity animation state
     */
    public static class EntityAnimationData {
        public KeyframeAnimationPlayer currentAnimation;
        public String animationName = "";
        public String jsonAnimationName = "";
        public int startTime = 0;
        public KeyframeAnimation keyframeAnimation; // Store reference to the actual JSON animation data

        public float getAnimationProgress(int currentTick) {
            if (currentAnimation == null || keyframeAnimation == null) return 0;
            float duration = keyframeAnimation.endTick / 20.0f; // Convert ticks to seconds
            float elapsed = (currentTick - startTime) / 20.0f;
            return Math.min(elapsed / duration, 1.0f);
        }

        public float getAnimationTime(int currentTick) {
            return (currentTick - startTime) / 20.0f; // Time in seconds
        }
    }
}