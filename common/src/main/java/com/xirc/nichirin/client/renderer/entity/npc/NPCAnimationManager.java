package com.xirc.nichirin.client.renderer.entity.npc;

import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class NPCAnimationManager {

    private static final Map<Integer, AnimationStack> ANIMATION_STACKS = new ConcurrentHashMap<>();
    private static final Map<Integer, ModifierLayer<IAnimation>> ANIMATION_LAYERS = new ConcurrentHashMap<>();

    public static void setAnimationStack(int entityId, AnimationStack stack) {
        ANIMATION_STACKS.put(entityId, stack);
    }

    public static AnimationStack getAnimationStack(int entityId) {
        return ANIMATION_STACKS.get(entityId);
    }

    public static void setAnimationLayer(int entityId, ModifierLayer<IAnimation> layer) {
        ANIMATION_LAYERS.put(entityId, layer);
    }

    public static ModifierLayer<IAnimation> getAnimationLayer(int entityId) {
        return ANIMATION_LAYERS.get(entityId);
    }

    public static void playAnimation(LivingEntity entity, String animationName) {
        if (entity == null || animationName == null) return;

        try {
            System.out.println("NPCAnimationManager: playAnimation called for entity " + entity.getId() + " with animation: " + animationName);

            KeyframeAnimation animation = findAnimation(animationName);
            if (animation == null) {
                System.out.println("NPCAnimationManager: Animation is NULL after findAnimation!");
                return;
            }

            System.out.println("NPCAnimationManager: Animation found! Creating player...");
            KeyframeAnimationPlayer animationPlayer = new KeyframeAnimationPlayer(animation);

            playAnimationDirect(entity, animationPlayer);

            System.out.println("NPCAnimationManager: Successfully set up animation player");

        } catch (Exception e) {
            System.out.println("NPCAnimationManager: Exception in playAnimation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static KeyframeAnimation findAnimation(String animationName) {
        System.out.println("NPCAnimationManager: Searching for animation: " + animationName);

        // Try direct lookup first
        ResourceLocation directLoc = new ResourceLocation("nichirin", animationName);
        System.out.println("NPCAnimationManager: Trying: " + directLoc);
        KeyframeAnimation directResult = PlayerAnimationRegistry.getAnimation(directLoc);
        if (directResult != null) {
            System.out.println("NPCAnimationManager: ✓ Found animation at: " + directLoc);
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
                "basic/" + animationName.replace("_", "/"),
                "npc/" + animationName,
                "npc/combat/" + animationName,
                "npc/basic/" + animationName,
                "demon/" + animationName,
                "demon/attacks/" + animationName
        };

        for (String path : paths) {
            ResourceLocation loc = new ResourceLocation("nichirin", path);
            System.out.println("NPCAnimationManager: Trying: " + loc);
            KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(loc);
            if (animation != null) {
                System.out.println("NPCAnimationManager: ✓ Found animation at: " + loc);
                return animation;
            }
        }

        System.out.println("NPCAnimationManager: ✗ Animation NOT FOUND for: " + animationName);
        System.out.println("NPCAnimationManager: Make sure your animation JSON is at one of these paths:");
        System.out.println("  - assets/nichirin/playeranimator/" + animationName + ".json");
        System.out.println("  - assets/nichirin/playeranimator/attacks/basic/" + animationName + ".json");
        System.out.println("  - assets/nichirin/playeranimator/demon/" + animationName + ".json");

        return null;
    }

    private static void playAnimationDirect(LivingEntity entity, IAnimation animation) {
        try {
            ModifierLayer<IAnimation> animationLayer = getAnimationLayer(entity.getId());

            if (animationLayer == null) {
                System.out.println("NPCAnimationManager: Animation layer is NULL for entity " + entity.getId());
                return;
            }

            System.out.println("NPCAnimationManager: Setting animation on layer...");
            IAnimation currentAnim = animationLayer.getAnimation();
            if (currentAnim != null) {
                animationLayer.replaceAnimationWithFade(
                        AbstractFadeModifier.standardFadeIn(3, Ease.INOUTSINE),
                        animation
                );
            } else {
                animationLayer.setAnimation(animation);
            }
            System.out.println("NPCAnimationManager: Animation set successfully!");
        } catch (Exception e) {
            System.out.println("NPCAnimationManager: Exception in playAnimationDirect: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void stopAnimation(LivingEntity entity) {
        if (entity == null) return;

        try {
            ModifierLayer<IAnimation> animationLayer = getAnimationLayer(entity.getId());
            if (animationLayer != null) {
                animationLayer.setAnimation(null);
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

    public static boolean isAnimationPlaying(LivingEntity entity) {
        if (entity == null) return false;

        try {
            ModifierLayer<IAnimation> animationLayer = getAnimationLayer(entity.getId());
            if (animationLayer != null) {
                IAnimation currentAnim = animationLayer.getAnimation();
                return currentAnim != null && currentAnim.isActive();
            }
        } catch (Exception e) {
            // Silent fail
        }
        return false;
    }

    public static void cleanupEntity(int entityId) {
        ANIMATION_STACKS.remove(entityId);
        ANIMATION_LAYERS.remove(entityId);
    }

    public static void clearAll() {
        ANIMATION_STACKS.clear();
        ANIMATION_LAYERS.clear();
    }

    public static void initializeNPCAnimation(LivingEntity entity) {
        if (entity == null) return;

        System.out.println("NPCAnimationManager: Initializing animation system for entity " + entity.getId());

        AnimationStack stack = new AnimationStack();
        ModifierLayer<IAnimation> animationLayer = new ModifierLayer<>();
        stack.addAnimLayer(0, animationLayer);

        setAnimationStack(entity.getId(), stack);
        setAnimationLayer(entity.getId(), animationLayer);

        System.out.println("NPCAnimationManager: Initialization complete");
    }

    public static void tickAllAnimations() {
        for (ModifierLayer<IAnimation> layer : ANIMATION_LAYERS.values()) {
            try {
                if (layer != null && layer.getAnimation() != null) {
                    layer.tick();
                }
            } catch (Exception e) {
                // Silent fail
            }
        }
    }
}