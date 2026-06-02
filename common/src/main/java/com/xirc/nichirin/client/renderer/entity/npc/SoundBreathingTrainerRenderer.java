package com.xirc.nichirin.client.renderer.entity.npc;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.renderer.entity.BaseAZNichirinEntityRenderer;
import com.xirc.nichirin.client.renderer.entity.animator.SoundBreathingTrainerAnimator;
import com.xirc.nichirin.common.entity.npc.SoundBreathingTrainerEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class SoundBreathingTrainerRenderer extends BaseAZNichirinEntityRenderer<SoundBreathingTrainerEntity> {
    private static final ResourceLocation GEO = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "geo/temple_demon.geo.json");
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/entity/npc/temple_demon.png");

    public SoundBreathingTrainerRenderer(EntityRendererProvider.Context context) {
        super(AzEntityRendererConfig.<SoundBreathingTrainerEntity>builder(GEO, TEX)
                .setAnimatorProvider(SoundBreathingTrainerAnimator::new).build(), context, TEX);
    }
}