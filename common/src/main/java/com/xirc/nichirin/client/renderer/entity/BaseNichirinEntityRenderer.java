package com.xirc.nichirin.client.renderer.entity;

import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public abstract class BaseNichirinEntityRenderer<T extends Entity> extends AzEntityRenderer<T> {

    private final ResourceLocation textureLocation;

    protected BaseNichirinEntityRenderer(AzEntityRendererConfig<T> config, EntityRendererProvider.Context context, ResourceLocation texture) {
        super(config, context);
        this.textureLocation = texture;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return textureLocation;
    }
}