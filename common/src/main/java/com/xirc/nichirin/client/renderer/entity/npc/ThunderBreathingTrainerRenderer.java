package com.xirc.nichirin.client.renderer.entity.npc;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.renderer.entity.BaseAZNichirinEntityRenderer;
import com.xirc.nichirin.client.renderer.entity.animator.ThunderBreathingTrainerAnimator;
import com.xirc.nichirin.common.entity.npc.ThunderBreathingTrainerEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ThunderBreathingTrainerRenderer extends BaseAZNichirinEntityRenderer<ThunderBreathingTrainerEntity> {

    private static final ResourceLocation GEO = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "geo/jigoro.geo.json");
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/entity/npc/jigoro.png");

    public ThunderBreathingTrainerRenderer(EntityRendererProvider.Context context) {
        super(
                AzEntityRendererConfig.<ThunderBreathingTrainerEntity>builder(GEO, TEX)
                        .setAnimatorProvider(ThunderBreathingTrainerAnimator::new)
                        .build(),
                context,
                TEX
        );
    }
}