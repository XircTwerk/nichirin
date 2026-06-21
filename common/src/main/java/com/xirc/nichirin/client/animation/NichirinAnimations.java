package com.xirc.nichirin.client.animation;

import com.xirc.nichirin.common.item.gun.GenyaDB;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.animation.PlayerRawAnimationBuilder;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
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
import net.minecraft.util.Mth;
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
    /** Extra downward tilt on the gun arm so the barrel lines up with where the player is looking. */
    private static final float GUN_ARM_DOWN_CORRECTION = (float) Math.toRadians(14.0);
    /** Last gun animation triggered for the LOCAL player — used to decide whether to show the body in
     *  first person (only the reload should show; idle/fire stay third-person only). */
    private static String localGunTrigger = "";
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
                            new PlayerAnimationController(player, (controller_, state, setter) -> {
                                // While the gun is held, keep the looping idle (the raised hold pose)
                                // running as the base animation. It keeps the controller active so PAL
                                // renders the player body — and therefore the posed right arm — in first
                                // person, and gives fire/reload a matching pose to hand off to/from.
                                if (player.getMainHandItem().getItem() instanceof GenyaDB) {
                                    RawAnimation idle = gunIdleRaw();
                                    if (idle != null) {
                                        return setter.setAnimation(idle);
                                    }
                                }
                                return PlayState.STOP;
                            });
                    controller.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
                    controller.setFirstPersonConfiguration(FIRST_PERSON_CONFIG);
                    controller.setOverrideEasingType(EasingType.EASE_IN_OUT_SINE);
                    controller.addModifierBefore(crouchingArmModifier(player));
                    controller.addModifierBefore(gunHoldModifier(player));
                    return controller;
                });
    }

    /**
     * Keeps the right arm raised in the gun-holding pose while a {@link GenyaDB} is held and no
     * triggered animation is playing. The fire/reload player animations start and end at this same
     * raised pose, so they take over cleanly: while one is active the controller is "active" and this
     * modifier steps aside, then resumes the hold when the animation finishes. Mirrors the
     * {@link #crouchingArmModifier} pattern.
     */
    private static AdjustmentModifier gunHoldModifier(AbstractClientPlayer player) {
        AdjustmentModifier modifier = new AdjustmentModifier(partName -> {
            if (!(player.getMainHandItem().getItem() instanceof GenyaDB)) {
                return Optional.empty();
            }
            // No look-angle aiming in first person — the held gun model handles aiming there, so the
            // arm just keeps the raw idle/fire/reload pose.
            if (FirstPersonMode.isFirstPersonPass()) {
                return Optional.empty();
            }
            if ("right_arm".equals(partName)) {
                // Aim the gun arm toward where the player looks. The idle/fire/reload animations all
                // supply the ~-90deg arm raise themselves, so this modifier only adds the look offset
                // (+ downward correction) ON TOP. It's the same kind of offset for every animation, so
                // there's no jump when one hands off to another — transitions stay smooth — and applying
                // it during the triggered animations is what makes fire/reload follow the look angle too.
                float pitch = (float) Math.toRadians(player.getXRot());          // up/down
                float yaw = (float) Math.toRadians(                              // side to side
                        Mth.wrapDegrees(player.getYHeadRot() - player.yBodyRot));
                // PartModifier(rotation, offset) — rotation FIRST. X = pitch, Y = yaw.
                return Optional.of(new AdjustmentModifier.PartModifier(
                        new Vec3f(pitch + GUN_ARM_DOWN_CORRECTION, yaw, 0.0f),
                        new Vec3f(0.0f, 0.0f, 0.0f)));
            }
            return Optional.empty();
        });
        modifier.fadeIn = false;
        modifier.fadeOut = false;
        return modifier;
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

        if (clientPlayer == minecraft.player
                && ("fire".equals(animationName) || "reload".equals(animationName))) {
            localGunTrigger = animationName;
        }

        if ("sword.block".equals(animationName)) {
            controller.triggerAnimation(PlayerRawAnimationBuilder.begin().thenPlayAndHold(animation).build());
        } else if ("fire".equals(animationName) || "reload".equals(animationName)) {
            // Ease the gun's fire/reload in from the current pose instead of snapping into the first
            // frame. Fire is short so it gets a quick fade (a long one would swallow the recoil);
            // reload is long enough to afford a softer blend. replaceAnimationWithFade also restarts
            // the animation, so rapid gunfire keeps re-triggering the recoil.
            int fadeTicks = "reload".equals(animationName) ? 5 : 2;
            controller.replaceAnimationWithFade(
                    AbstractFadeModifier.standardFadeIn(fadeTicks, EasingType.EASE_IN_OUT_SINE), animation);
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

    /**
     * Drives whether the LOCAL player's body shows in first person. While holding the gun we keep the
     * controller in {@link FirstPersonMode#NONE} so the idle hold pose and the fire recoil stay
     * third-person only (first person shows the vanilla arm + gun model). Only while a reload is
     * actively playing do we switch to {@link FirstPersonMode#THIRD_PERSON_MODEL} so the reload shows
     * in first person. Anything else (no gun) keeps the normal mode so katana/breathing animations
     * still render in first person. Call once per client tick for the local player.
     */
    public static void tickFirstPersonMode(AbstractClientPlayer player) {
        PlayerAnimationController controller = getController(player);
        if (controller == null) return;
        boolean holdingGun = player.getMainHandItem().getItem() instanceof GenyaDB;
        boolean reloadShowing = "reload".equals(localGunTrigger) && controller.isPlayingTriggeredAnimation();
        controller.setFirstPersonMode(holdingGun && !reloadShowing
                ? FirstPersonMode.NONE
                : FirstPersonMode.THIRD_PERSON_MODEL);
    }

    public static boolean isAnimationPlaying(AbstractClientPlayer player) {
        PlayerAnimationController controller = getController(player);
        return controller != null && controller.isActive();
    }

    /** Lazily-built looping idle used as the gun's base animation (see the controller's state
     *  handler). Cached because it is resolved once the player_animations are loaded. */
    private static RawAnimation gunIdleRaw() {
        if (gunIdleRaw == null) {
            ResourceLocation loc = findAnimation("idle");
            if (loc != null && PlayerAnimResources.hasAnimation(loc)) {
                Animation anim = PlayerAnimResources.getAnimation(loc);
                if (anim != null) {
                    gunIdleRaw = RawAnimation.begin().thenLoop(anim);
                }
            }
        }
        return gunIdleRaw;
    }

    private static RawAnimation gunIdleRaw;

    private static PlayerAnimationController getController(AbstractClientPlayer player) {
        return PlayerAnimationAccess.getPlayerAnimationLayer(player, CONTROLLER_ID)
                instanceof PlayerAnimationController controller ? controller : null;
    }
}
