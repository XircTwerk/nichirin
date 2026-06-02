package com.xirc.nichirin.client.renderer.entity.animator;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.entity.attack.ThunderBallEntity;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.dispatch.AzDispatchSide;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ThunderBallAnimator extends AzEntityAnimator<ThunderBallEntity> {

    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
            BreathOfNichirin.MOD_ID,
            "animations/thunder_ball.animation.json"
    );

    private static final AzCommand SPIN = AzCommand.create("spin_controller", "spin", AzPlayBehaviors.LOOP);

    private boolean started = false;

    @Override
    public void registerControllers(AzAnimationControllerContainer<ThunderBallEntity> container) {
        container.add(
                AzAnimationController.builder(this, "spin_controller")
                        .build()
        );
    }

    @Override
    public void setCustomAnimations(ThunderBallEntity entity, float partialTicks) {
        if (!started) {
            SPIN.actions().forEach(action -> action.handle(AzDispatchSide.CLIENT, this));
            started = true;
        }
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(ThunderBallEntity animatable) {
        return ANIMATIONS;
    }
}