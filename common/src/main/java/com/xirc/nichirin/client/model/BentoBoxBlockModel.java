package com.xirc.nichirin.client.model;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.blocks.BentoBoxBlock;
import mod.azure.azurelib.model.GeoModel;
import net.minecraft.resources.ResourceLocation;

public class BentoBoxBlockModel extends GeoModel<BentoBoxBlock.BentoBoxBlockEntity> {

    @Override
    public ResourceLocation getModelResource(BentoBoxBlock.BentoBoxBlockEntity blockEntity) {
        return new ResourceLocation(BreathOfNichirin.MOD_ID, "geo/bento_box_block.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BentoBoxBlock.BentoBoxBlockEntity blockEntity) {
        return new ResourceLocation(BreathOfNichirin.MOD_ID, "textures/block/bento_box_block.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BentoBoxBlock.BentoBoxBlockEntity blockEntity) {
        return null; // No animations needed
    }
}