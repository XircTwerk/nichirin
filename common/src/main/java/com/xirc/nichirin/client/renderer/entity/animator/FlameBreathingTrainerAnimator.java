package com.xirc.nichirin.client.renderer.entity.animator;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.entity.npc.FlameBreathingTrainerEntity;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class FlameBreathingTrainerAnimator extends AzEntityAnimator<FlameBreathingTrainerEntity> {
    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
            BreathOfNichirin.MOD_ID, "animations/temple_demon.animation.json");

    @Override
    public void registerControllers(AzAnimationControllerContainer<FlameBreathingTrainerEntity> container) {
        container.add(AzAnimationController.<FlameBreathingTrainerEntity>builder(this, "main_controller").build());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(FlameBreathingTrainerEntity animatable) {
        return ANIMATIONS;
    }
}