package com.xirc.nichirin.client.renderer.entity.attack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.common.entity.attack.ShockwaveEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Empty renderer for {@link ShockwaveEntity}.
 *
 * <p>Visuals are provided entirely by the aura system attached at spawn time
 * ({@link ShockwaveEntity#attachAura()}) — the aura's churning silhouette IS the shockwave.
 * No custom model, sprite, or animated texture is drawn here.</p>
 */
@Environment(EnvType.CLIENT)
public class ShockwaveEntityRenderer extends EntityRenderer<ShockwaveEntity> {

    private static final ResourceLocation EMPTY = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public ShockwaveEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ShockwaveEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Intentionally empty — aura handles the look.
    }

    @Override
    public ResourceLocation getTextureLocation(ShockwaveEntity entity) {
        return EMPTY;
    }
}
