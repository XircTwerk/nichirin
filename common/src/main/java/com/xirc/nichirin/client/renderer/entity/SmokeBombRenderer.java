package com.xirc.nichirin.client.renderer.entity;

import com.xirc.nichirin.common.entity.SmokeBombEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;

public class SmokeBombRenderer extends ThrownItemRenderer<SmokeBombEntity> {

    public SmokeBombRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}