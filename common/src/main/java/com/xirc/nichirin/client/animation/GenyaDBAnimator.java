package com.xirc.nichirin.client.animation;

import com.xirc.nichirin.BreathOfNichirin;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.dispatch.AzDispatchSide;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.impl.AzItemAnimator;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehavior;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class GenyaDBAnimator extends AzItemAnimator {

    private static final ResourceLocation ANIMATION_LOC =
            ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "animations/genya_db.animation.json");

    private static final String CONTROLLER_NAME = "gun";

    // Request signal set by GunAnimationPacket on the client thread. Each shot/reload publishes the
    // animation name and then bumps requestSeq. We cannot store the request on the ItemStack because
    // AzureLib animates a render copy, so it has to live in a static the animator polls each frame.
    // Write order matters: set requestedAnim first, then bump requestSeq, so a reader that observes
    // the new seq is guaranteed to also see the matching name (volatile happens-before).
    public static volatile String requestedAnim = null;
    public static volatile long requestSeq = 0;

    // Per-instance request bookkeeping. setCustomAnimations fires once per display context per frame
    // (FIRST_PERSON, THIRD_PERSON, GUI, …) and Minecraft may use distinct ItemStack instances per
    // context, each with its own animator. Tracking the last consumed request per instance lets every
    // animator play a freshly requested animation exactly once instead of racing on shared statics.
    private long lastConsumedRequest = Long.MIN_VALUE;
    // Whether this animator has already settled back into the looping idle since its last request.
    private boolean idleDispatched = false;
    // Double-shot: the game tick at which to fire the second (right-barrel) flash, or MIN_VALUE if
    // none pending. We play fireleft immediately, then fireright a couple ticks later so it cancels
    // the still-playing fireleft, giving a fast left-then-right double flash.
    private long secondFlashTick = Long.MIN_VALUE;
    // Ticks between the left and right flash of a double-shot.
    private static final long DOUBLE_FLASH_GAP_TICKS = 2;

    private AzAnimationController<ItemStack> controller;

    @Override
    public void registerControllers(AzAnimationControllerContainer<ItemStack> container) {
        controller = AzAnimationController.builder(this, CONTROLLER_NAME).build();
        container.add(controller);
    }

    @Override
    public void setCustomAnimations(ItemStack stack, float partialTicks) {
        if (controller == null) return;

        long now = currentGameTick();

        long seq = requestSeq;
        if (seq != lastConsumedRequest) {
            lastConsumedRequest = seq;
            String anim = requestedAnim;
            if (anim != null) {
                idleDispatched = false;
                if ("fire".equals(anim)) {
                    // Double-shot: left flash now, right flash scheduled to cut it off shortly.
                    secondFlashTick = now + DOUBLE_FLASH_GAP_TICKS;
                    dispatch("fireleft", AzPlayBehaviors.PLAY_ONCE);
                } else {
                    secondFlashTick = Long.MIN_VALUE;
                    dispatch(anim, AzPlayBehaviors.PLAY_ONCE);
                }
                return;
            }
        }

        // Double-shot second stage: fire the right flash, cancelling the in-progress fireleft.
        if (secondFlashTick != Long.MIN_VALUE && now >= secondFlashTick) {
            secondFlashTick = Long.MIN_VALUE;
            dispatch("fireright", AzPlayBehaviors.PLAY_ONCE);
            return;
        }

        // Once a one-shot (fire/reload) finishes the controller is stopped/paused; settle back into the
        // looping idle pose. idle is dispatched with an explicit LOOP behavior so it actually loops and
        // stays in the play state — this is what stops it from being re-triggered every tick.
        if (!idleDispatched
                && !controller.stateMachine().isPlaying()
                && !controller.stateMachine().isTransitioning()) {
            idleDispatched = true;
            dispatch("idle", AzPlayBehaviors.LOOP);
        }
    }

    /**
     * Dispatch an animation on this animator's controller with an EXPLICIT play behavior. We must pass
     * the behavior explicitly (rather than letting AzureLib infer it from the JSON loop type) because a
     * bare queued stage falls back to the shared, mutable {@code AzAnimationStageProperties.EMPTY} whose
     * play behavior is non-deterministic across the game. This mirrors how the armor renderers dispatch.
     */
    private void dispatch(String anim, AzPlayBehavior behavior) {
        AzCommand.create(CONTROLLER_NAME, anim, behavior)
                .actions().forEach(action -> action.handle(AzDispatchSide.CLIENT, this));
    }

    private static long currentGameTick() {
        var level = Minecraft.getInstance().level;
        return level != null ? level.getGameTime() : 0L;
    }

    /** Name of the animation currently playing on the gun controller, or null if none. */
    public String currentAnimationName() {
        if (controller == null) return null;
        var current = controller.currentAnimation();
        return current != null ? current.animation().name() : null;
    }

    @Override
    public ResourceLocation getAnimationLocation(ItemStack stack) {
        return ANIMATION_LOC;
    }
}
