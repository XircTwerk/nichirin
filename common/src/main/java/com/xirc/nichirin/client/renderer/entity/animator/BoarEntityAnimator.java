package com.xirc.nichirin.client.renderer.entity.animator;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.entity.animal.BoarEntity;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class BoarEntityAnimator extends AzEntityAnimator<BoarEntity> {

    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
            BreathOfNichirin.MOD_ID,
            "animations/boar.animation.json"
    );

    @Override
    public void registerControllers(AzAnimationControllerContainer<BoarEntity> container) {
        container.add(
                AzAnimationController.<BoarEntity>builder(this, "main_controller")
                        .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(BoarEntity animatable) {
        return ANIMATIONS;
    }
}