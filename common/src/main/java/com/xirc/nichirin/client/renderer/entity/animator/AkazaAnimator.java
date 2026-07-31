package com.xirc.nichirin.client.renderer.entity.animator;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.entity.npc.AkazaEntity;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class AkazaAnimator extends AzEntityAnimator<AkazaEntity> {

    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
            BreathOfNichirin.MOD_ID,
            "animations/akaza.animation.json"
    );

    @Override
    public void registerControllers(AzAnimationControllerContainer<AkazaEntity> container) {
        container.add(
                AzAnimationController.<AkazaEntity>builder(this, "main_controller")
                        .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(AkazaEntity animatable) {
        return ANIMATIONS;
    }
}
