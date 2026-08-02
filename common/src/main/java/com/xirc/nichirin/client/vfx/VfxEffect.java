package com.xirc.nichirin.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;

public interface VfxEffect {
    int lifetimeTicks();

    default void tick(VfxInstance instance) {}

    void render(VfxInstance instance, PoseStack poseStack, Camera camera, float partialTick);
}
