package com.xirc.nichirin.client.animator.entity;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.entity.BoarEntity;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class BoarEntityAnimator extends AzEntityAnimator<BoarEntity> {

    private static final ResourceLocation ANIMATIONS = new ResourceLocation(
            BreathOfNichirin.MOD_ID,
            "animations/boar.animation.json"
    );

    @Override
    public void registerControllers(AzAnimationControllerContainer<BoarEntity> container) {
        container.add(
                AzAnimationController.builder(this, "main_controller")
                        .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(BoarEntity animatable) {
        return ANIMATIONS;
    }
}