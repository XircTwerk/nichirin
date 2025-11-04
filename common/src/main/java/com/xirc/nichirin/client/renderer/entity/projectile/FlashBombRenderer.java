package com.xirc.nichirin.client.renderer.entity.projectile;

import com.xirc.nichirin.common.entity.projectile.FlashBombEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class FlashBombRenderer extends ThrownItemRenderer<FlashBombEntity> {

    public FlashBombRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}