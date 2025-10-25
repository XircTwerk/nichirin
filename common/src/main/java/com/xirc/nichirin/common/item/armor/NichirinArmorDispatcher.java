package com.xirc.nichirin.common.item.armor;

import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class NichirinArmorDispatcher {

    private static final AzCommand IDLE = AzCommand.create(
            "movement_controller",
            "idle",
            AzPlayBehaviors.LOOP
    );

    private static final AzCommand WALKING = AzCommand.create(
            "movement_controller",
            "walking",
            AzPlayBehaviors.LOOP
    );

    public void idle(Entity entity, ItemStack stack) {
        IDLE.sendForItem(entity, stack);
    }

    public void walking(Entity entity, ItemStack stack) {
        WALKING.sendForItem(entity, stack);
    }
}