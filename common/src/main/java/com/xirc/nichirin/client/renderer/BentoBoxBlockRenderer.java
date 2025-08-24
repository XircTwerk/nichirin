package com.xirc.nichirin.client.renderer;

import com.xirc.nichirin.client.model.BentoBoxBlockModel;
import com.xirc.nichirin.common.blocks.BentoBoxBlock;
import mod.azure.azurelib.renderer.GeoBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class BentoBoxBlockRenderer extends GeoBlockRenderer<BentoBoxBlock.BentoBoxBlockEntity> {

    public BentoBoxBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new BentoBoxBlockModel());
    }
}