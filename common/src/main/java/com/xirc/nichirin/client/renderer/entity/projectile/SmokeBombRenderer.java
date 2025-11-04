package com.xirc.nichirin.client.renderer.entity.projectile;

import com.xirc.nichirin.common.entity.projectile.SmokeBombEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class SmokeBombRenderer extends ThrownItemRenderer<SmokeBombEntity> {

    public SmokeBombRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}