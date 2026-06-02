package com.xirc.nichirin.client.renderer.entity.animator;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.entity.npc.TempleDemonEntity;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class TempleDemonAnimator extends AzEntityAnimator<TempleDemonEntity> {

    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
            BreathOfNichirin.MOD_ID,
            "animations/temple_demon.animation.json"
    );

    @Override
    public void registerControllers(AzAnimationControllerContainer<TempleDemonEntity> container) {
        container.add(
                AzAnimationController.<TempleDemonEntity>builder(this, "main_controller")
                        .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(TempleDemonEntity animatable) {
        return ANIMATIONS;
    }
}