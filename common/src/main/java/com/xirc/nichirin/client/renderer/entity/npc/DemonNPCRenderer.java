package com.xirc.nichirin.client.renderer.entity.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.common.entity.npc.DemonNPCEntity;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for Demon NPCs that uses PlayerAnimator for animations.
 * This allows NPCs to use the same animation system as players.
 */
public class DemonNPCRenderer extends LivingEntityRenderer<DemonNPCEntity, PlayerModel<DemonNPCEntity>> {

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation("nichirin", "textures/entity/npc/demon_npc.png");

    public DemonNPCRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public void render(DemonNPCEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        poseStack.pushPose();

        // Apply scaling if set on the entity
        float scale = entity.getRenderScale();
        if (scale != 1.0f) {
            poseStack.scale(scale, scale, scale);
        }

        // Update animation state before rendering
        updateAnimationState(entity, partialTick);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }

    /**
     * Update the animation state for this NPC
     */
    private void updateAnimationState(DemonNPCEntity entity, float partialTick) {
        try {
            // Get or create animation stack for this NPC
            AnimationStack animationStack = getOrCreateAnimationStack(entity);

            if (animationStack != null) {
                // Apply animation transformations to the model
                PlayerModel<DemonNPCEntity> model = this.getModel();

                // This is where the magic happens - apply PlayerAnimator transforms
                applyAnimationToModel(entity, model, animationStack, partialTick);
            }
        } catch (Exception e) {
            // Silent fail to prevent crashes
        }
    }

    /**
     * Get or create an animation stack for the NPC entity
     */
    private AnimationStack getOrCreateAnimationStack(DemonNPCEntity entity) {
        // Check if animation stack exists
        if (NPCAnimationManager.getAnimationStack(entity.getId()) == null) {
            NPCAnimationManager.initializeNPCAnimation(entity);
        }

        return NPCAnimationManager.getAnimationStack(entity.getId());
    }

    /**
     * Apply animation transformations to the player model
     */
    private void applyAnimationToModel(DemonNPCEntity entity, PlayerModel<DemonNPCEntity> model,
                                       AnimationStack animationStack, float partialTick) {
        try {
            ModifierLayer<IAnimation> layer = NPCAnimationManager.getAnimationLayer(entity.getId());
            if (layer != null && layer.getAnimation() != null && layer.getAnimation().isActive()) {
                // Apply the animation tick
                layer.tick();

                // The animation system will modify the model's rotations and positions
                // PlayerAnimator handles this automatically when properly integrated
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

    @Override
    public ResourceLocation getTextureLocation(DemonNPCEntity entity) {
        // You can customize this based on entity data
        String demonType = entity.getDemonType();
        if (demonType != null && !demonType.isEmpty()) {
            return new ResourceLocation("nichirin", "textures/entity/npc/demon_" + demonType + ".png");
        }
        return DEFAULT_TEXTURE;
    }

    @Override
    protected boolean shouldShowName(DemonNPCEntity entity) {
        // Show name if the entity has a custom name
        return entity.hasCustomName();
    }

    @Override
    protected void scale(DemonNPCEntity livingEntity, PoseStack poseStack, float partialTickTime) {
        // Additional scaling is already handled in the main render method
        super.scale(livingEntity, poseStack, partialTickTime);
    }
}