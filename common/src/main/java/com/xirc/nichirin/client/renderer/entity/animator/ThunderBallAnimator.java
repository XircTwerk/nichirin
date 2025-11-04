package com.xirc.nichirin.client.renderer.entity.animator.entity;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.entity.attack.ThunderBallEntity;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ThunderBallAnimator extends AzEntityAnimator<ThunderBallEntity> {

    private static final ResourceLocation ANIMATIONS = new ResourceLocation(
            BreathOfNichirin.MOD_ID,
            "animations/thunder_ball.animation.json"
    );

    @Override
    public void registerControllers(AzAnimationControllerContainer<ThunderBallEntity> container) {
        container.add(
                AzAnimationController.builder(this, "spin_controller")
                        .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(ThunderBallEntity animatable) {
        return ANIMATIONS;
    }
}