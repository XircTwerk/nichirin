package com.xirc.nichirin.client.renderer.entity.animator;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.entity.npc.ThunderBreathingTrainerEntity;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ThunderBreathingTrainerAnimator extends AzEntityAnimator<ThunderBreathingTrainerEntity> {
    private static final ResourceLocation ANIMATIONS = new ResourceLocation(
            BreathOfNichirin.MOD_ID, "animations/jigoro.animation.json");

    @Override
    public void registerControllers(AzAnimationControllerContainer<ThunderBreathingTrainerEntity> container) {
        container.add(AzAnimationController.<ThunderBreathingTrainerEntity>builder(this, "main_controller").build());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(ThunderBreathingTrainerEntity animatable) {
        return ANIMATIONS;
    }
}
