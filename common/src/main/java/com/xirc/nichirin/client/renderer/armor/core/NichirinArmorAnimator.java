package com.xirc.nichirin.client.renderer.armor.core;

import com.xirc.nichirin.BreathOfNichirin;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzItemAnimator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class NichirinArmorAnimator extends AzItemAnimator {

    private final String armorName;

    public NichirinArmorAnimator(String armorName) {
        this.armorName = armorName;
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<ItemStack> container) {
        container.add(
                AzAnimationController.builder(this, "movement_controller")
                        .setTransitionLength(5)
                        .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(ItemStack animatable) {
        return BreathOfNichirin.id("animations/" + armorName + ".animation.json");
    }
}