package com.xirc.nichirin.client.renderer.entity.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.xirc.nichirin.common.entity.effect.PlayerCloneEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class PlayerCloneRenderer extends HumanoidMobRenderer<PlayerCloneEntity, PlayerModel<PlayerCloneEntity>> {

    private final PlayerModel<PlayerCloneEntity> normalModel;
    private final PlayerModel<PlayerCloneEntity> slimModel;

    public PlayerCloneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.normalModel = this.model;
        this.slimModel   = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void render(PlayerCloneEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Pick the right arm-width model based on the master player's skin type
        this.model = CloneSkinTracker.isSlimFor(entity) ? slimModel : normalModel;
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(PlayerCloneEntity entity) {
        return CloneSkinTracker.getSkinFor(entity, MinecraftProfileTexture.Type.SKIN);
    }
}
