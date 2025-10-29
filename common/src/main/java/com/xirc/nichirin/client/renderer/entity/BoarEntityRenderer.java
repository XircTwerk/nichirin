package com.xirc.nichirin.client.renderer.entity;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.animator.entity.BoarEntityAnimator;
import com.xirc.nichirin.common.entity.BoarEntity;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class BoarEntityRenderer extends BaseNichirinEntityRenderer<BoarEntity> {

    private static final ResourceLocation GEO = new ResourceLocation(BreathOfNichirin.MOD_ID, "geo/boar.geo.json");
    private static final ResourceLocation TEX = new ResourceLocation(BreathOfNichirin.MOD_ID, "textures/entity/boar.png");

    public BoarEntityRenderer(EntityRendererProvider.Context context) {
        super(
                AzEntityRendererConfig.<BoarEntity>builder(GEO, TEX)
                        .setAnimatorProvider(BoarEntityAnimator::new)
                        .setScale(entity -> entity.isBaby() ? 0.8f : 1.25f)
                        .build(),
                context,
                TEX
        );
    }
}