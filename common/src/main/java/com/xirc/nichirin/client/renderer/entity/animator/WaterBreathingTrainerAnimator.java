package com.xirc.nichirin.client.renderer.entity.animator;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.entity.npc.WaterBreathingTrainerEntity;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class WaterBreathingTrainerAnimator extends AzEntityAnimator<WaterBreathingTrainerEntity> {

    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
            BreathOfNichirin.MOD_ID,
            "animations/urokodaki_npc.animation.json"
    );

    @Override
    public void registerControllers(AzAnimationControllerContainer<WaterBreathingTrainerEntity> container) {
        container.add(
                AzAnimationController.<WaterBreathingTrainerEntity>builder(this, "main_controller")
                        .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(WaterBreathingTrainerEntity animatable) {
        return ANIMATIONS;
    }
}