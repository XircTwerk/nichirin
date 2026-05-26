package com.xirc.nichirin.client.renderer.entity.effect;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.xirc.nichirin.common.entity.effect.PlayerCloneEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class PlayerCloneRenderer extends HumanoidMobRenderer<PlayerCloneEntity, PlayerModel<PlayerCloneEntity>> {

    public PlayerCloneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(PlayerCloneEntity entity) {
        return CloneSkinTracker.getSkinFor(entity, MinecraftProfileTexture.Type.SKIN);
    }
}
