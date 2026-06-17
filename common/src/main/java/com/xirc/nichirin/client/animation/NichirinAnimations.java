package com.xirc.nichirin.client.animation;

import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.animation.PlayerRawAnimationBuilder;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.SpeedModifier;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.PlayState;
import com.zigythebird.playeranimcore.math.Vec3f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class NichirinAnimations {

    private static final Logger LOGGER = LoggerFactory.getLogger("NichirinAnimations");
    private static final ResourceLocation CONTROLLER_ID =
            ResourceLocation.fromNamespaceAndPath("nichirin", "animation_controller");
    private static final FirstPersonConfiguration FIRST_PERSON_CONFIG =
            new FirstPersonConfiguration(true, true, true, true);
    private static boolean initialized;

    private NichirinAnimations() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                CONTROLLER_ID,
                1001,
                player -> {
                    PlayerAnimationController controller =
                            new PlayerAnimationController(player, (current, state, setter) -> PlayState.STOP);
                    controller.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
                    controller.setFirstPersonConfiguration(FIRST_PERSON_CONFIG);
                    controller.setOverrideEasingType(EasingType.EASE_IN_OUT_SINE);
                    controller.addModifierBefore(crouchingArmModifier(player));
                    return controller;
                });
    }

    private static AdjustmentModifier crouchingArmModifier(AbstractClientPlayer player) {
        AdjustmentModifier modifier = new AdjustmentModifier(partName -> {
            if (!player.isCrouching() || !FirstPersonMode.isFirstPersonPass()) {
                return Optional.empty();
            }

            return switch (partName) {
                case "right_arm" -> Optional.of(new AdjustmentModifier.PartModifier(
                        new Vec3f(-0.22f, 0.0f, -0.08f),
                        new Vec3f(0.0f, -1.5f, -0.75f)));
                case "left_arm" -> Optional.of(new AdjustmentModifier.PartModifier(
                        new Vec3f(-0.22f, 0.0f, 0.08f),
                        new Vec3f(0.0f, -1.5f, -0.75f)));
                default -> Optional.empty();
            };
        });
        modifier.fadeIn = false;
        modifier.fadeOut = false;
        return modifier;
    }

    public static void playAnimation(Player player, String animationName) {
        playAnimation(player, animationName, 1.0f);
    }

    public static void playAnimation(Player player, String animationName, float speed) {
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        if (animationName == null || animationName.isEmpty()) {
            PlayerAnimationController ctrl = getController(clientPlayer);
            if (ctrl != null) {
                setControllerSpeed(ctrl, 1.0f);
            }
            stopAnimation(clientPlayer);
            return;
        }

        PlayerAnimationController controller = getController(clientPlayer);
        if (controller == null) {
            LOGGER.error("[Nichirin] Animation controller is missing for player '{}'.", player.getScoreboardName());
            return;
        }

        ResourceLocation animation = findAnimation(animationName);
        if (animation == null) {
            LOGGER.error("[Nichirin] Could not find animation '{}' for player '{}'.",
                    animationName, player.getScoreboardName());
            return;
        }

        setControllerSpeed(controller, speed);

        if ("sword.block".equals(animationName)) {
            controller.triggerAnimation(PlayerRawAnimationBuilder.begin().thenPlayAndHold(animation).build());
        } else if (isHitAnimation(animationName)) {
            // Re-trigger from the start on every hit so rapid hits keep restarting the flinch
            // instead of being ignored while one is mid-play.
            controller.stopTriggeredAnimation();
            controller.triggerAnimation(animation);
        } else {
            controller.triggerAnimation(animation);
        }
    }

    /** Updates the controller's SpeedModifier in-place, or adds one if none exists. */
    private static void setControllerSpeed(PlayerAnimationController controller, float speed) {
        for (var mod : controller.getModifiers()) {
            if (mod instanceof SpeedModifier sm) {
                sm.speed = speed;
                return;
            }
        }
        if (speed != 1.0f) {
            controller.addModifierLast(new SpeedModifier(speed));
        }
    }

    private static boolean isHitAnimation(String animationName) {
        return "small_hit".equals(animationName)
                || "medium_hit".equals(animationName)
                || "large_hit".equals(animationName);
    }

    private static ResourceLocation findAnimation(String animationName) {
        String safeName = animationName.replace(' ', '_').toLowerCase();
        ResourceLocation direct = ResourceLocation.fromNamespaceAndPath("nichirin", safeName);
        if (PlayerAnimResources.hasAnimation(direct)) return direct;

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
            ResourceLocation candidate =
                    ResourceLocation.fromNamespaceAndPath("nichirin", prefix + safeName);
            if (PlayerAnimResources.hasAnimation(candidate)) return candidate;
        }

        ResourceLocation slashed =
                ResourceLocation.fromNamespaceAndPath("nichirin", safeName.replace("_", "/"));
        if (PlayerAnimResources.hasAnimation(slashed)) return slashed;

        return null;
    }

    public static void stopAnimation(AbstractClientPlayer player) {
        PlayerAnimationController controller = getController(player);
        if (controller != null) {
            controller.stopTriggeredAnimation();
            controller.stop();
        }
    }

    public static boolean isAnimationPlaying(AbstractClientPlayer player) {
        PlayerAnimationController controller = getController(player);
        return controller != null && controller.isActive();
    }

    private static PlayerAnimationController getController(AbstractClientPlayer player) {
        return PlayerAnimationAccess.getPlayerAnimationLayer(player, CONTROLLER_ID)
                instanceof PlayerAnimationController controller ? controller : null;
    }
}
