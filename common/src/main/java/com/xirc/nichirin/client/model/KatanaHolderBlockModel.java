package com.xirc.nichirin.client.model;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.blocks.KatanaHolderBlock;
import mod.azure.azurelib.model.GeoModel;
import net.minecraft.resources.ResourceLocation;

public class KatanaHolderBlockModel extends GeoModel<KatanaHolderBlock.KatanaHolderBlockEntity> {

    @Override
    public ResourceLocation getModelResource(KatanaHolderBlock.KatanaHolderBlockEntity blockEntity) {
        return new ResourceLocation(BreathOfNichirin.MOD_ID, "geo/katana_holder_block.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KatanaHolderBlock.KatanaHolderBlockEntity blockEntity) {
        return new ResourceLocation(BreathOfNichirin.MOD_ID, "textures/block/katana_holder_block.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KatanaHolderBlock.KatanaHolderBlockEntity blockEntity) {
        return null; // No animations needed
    }
}