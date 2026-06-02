package com.xirc.nichirin.client.renderer.entity.npc;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.renderer.entity.BaseAZNichirinEntityRenderer;
import com.xirc.nichirin.client.renderer.entity.animator.FlameBreathingTrainerAnimator;
import com.xirc.nichirin.common.entity.npc.FlameBreathingTrainerEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class FlameBreathingTrainerRenderer extends BaseAZNichirinEntityRenderer<FlameBreathingTrainerEntity> {
    private static final ResourceLocation GEO = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "geo/temple_demon.geo.json");
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/entity/npc/temple_demon.png");

    public FlameBreathingTrainerRenderer(EntityRendererProvider.Context context) {
        super(AzEntityRendererConfig.<FlameBreathingTrainerEntity>builder(GEO, TEX)
                .setAnimatorProvider(FlameBreathingTrainerAnimator::new).build(), context, TEX);
    }
}