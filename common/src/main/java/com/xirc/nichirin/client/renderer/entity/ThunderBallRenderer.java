package com.xirc.nichirin.client.renderer.entity;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.animator.entity.ThunderBallAnimator;
import com.xirc.nichirin.common.entity.ThunderBallEntity;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ThunderBallRenderer extends BaseNichirinEntityRenderer<ThunderBallEntity> {

    private static final ResourceLocation GEO = new ResourceLocation(BreathOfNichirin.MOD_ID, "geo/thunder_ball.geo.json");
    private static final ResourceLocation TEX = new ResourceLocation(BreathOfNichirin.MOD_ID, "textures/entity/thunder_ball.png");

    public ThunderBallRenderer(EntityRendererProvider.Context context) {
        super(
                AzEntityRendererConfig.<ThunderBallEntity>builder(GEO, TEX)
                        .setAnimatorProvider(ThunderBallAnimator::new)
                        .setScale(2.0f)
                        .build(),
                context,
                TEX
        );
    }
}