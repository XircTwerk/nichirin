package com.xirc.nichirin.client.renderer.entity.animal;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.renderer.entity.BaseAZNichirinEntityRenderer;
import com.xirc.nichirin.client.renderer.entity.animator.BoarEntityAnimator;
import com.xirc.nichirin.common.entity.animal.BoarEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class BoarEntityRenderer extends BaseAZNichirinEntityRenderer<BoarEntity> {

    private static final ResourceLocation GEO = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "geo/boar.geo.json");
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/entity/boar.png");

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