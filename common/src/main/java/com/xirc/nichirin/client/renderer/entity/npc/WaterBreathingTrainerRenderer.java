package com.xirc.nichirin.client.renderer.entity.npc;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.renderer.entity.BaseAZNichirinEntityRenderer;
import com.xirc.nichirin.client.renderer.entity.animator.WaterBreathingTrainerAnimator;
import com.xirc.nichirin.common.entity.npc.WaterBreathingTrainerEntity;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class WaterBreathingTrainerRenderer extends BaseAZNichirinEntityRenderer<WaterBreathingTrainerEntity> {

    private static final ResourceLocation GEO = new ResourceLocation(BreathOfNichirin.MOD_ID, "geo/temple_demon.geo.json");
    private static final ResourceLocation TEX = new ResourceLocation(BreathOfNichirin.MOD_ID, "textures/entity/npc/temple_demon.png");

    public WaterBreathingTrainerRenderer(EntityRendererProvider.Context context) {
        super(
                AzEntityRendererConfig.<WaterBreathingTrainerEntity>builder(GEO, TEX)
                        .setAnimatorProvider(WaterBreathingTrainerAnimator::new)
                        .build(),
                context,
                TEX
        );
    }
}
