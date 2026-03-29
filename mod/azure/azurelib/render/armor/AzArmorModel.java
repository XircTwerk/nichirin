package mod.azure.azurelib.render.armor;

import F;
import Z;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.render.armor.bone.AzArmorBoneContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AzArmorModel<E extends LivingEntity> extends HumanoidModel<E> {

    private final AzArmorRendererPipeline rendererPipeline;

    public AzArmorModel(AzArmorRendererPipeline rendererPipeline) {
        super(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        this.rendererPipeline = rendererPipeline;
        this.young = false;
    }

    @Override
    public void renderToBuffer(
        @NotNull PoseStack poseStack,
        @Nullable VertexConsumer buffer,
        int packedLight,
        int packedOverlay,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        var mc = Minecraft.getInstance();
        var context = rendererPipeline.context();
        var currentEntity = context.currentEntity();
        var currentStack = context.currentStack();
        MultiBufferSource bufferSource = Minecraft.getInstance().levelRenderer.renderBuffers.bufferSource();

        var shouldOutline = Minecraft.getInstance().levelRenderer.shouldShowEntityOutlines() && mc
            .shouldEntityAppearGlowing(
                currentEntity
            );

        if (shouldOutline) {
            bufferSource = Minecraft.getInstance().levelRenderer.renderBuffers.outlineBufferSource();
        }

        var config = rendererPipeline.config();
        var animatable = context.animatable();
        var partialTick = mc.getFrameTime();
        var textureLocation = config.textureLocation(currentEntity, animatable);
        var renderType = context.getDefaultRenderType(
            animatable,
            textureLocation,
            bufferSource,
            partialTick,
            config.getRenderType(context.currentEntity(), animatable),
            config.alpha(animatable)
        );
        buffer = ItemRenderer.getArmorFoilBuffer(bufferSource, renderType, false, currentStack.hasFoil());

        var model = rendererPipeline.renderer().provider().provideBakedModel(currentEntity, animatable);
        rendererPipeline.render(poseStack, model, animatable, bufferSource, null, buffer, 0, partialTick, packedLight);
    }

    /**
     * Applies settings and transformations pre-render based on the default model
     */
    public void applyBaseModel(HumanoidModel<?> baseModel) {
        this.young = baseModel.young;
        this.crouching = baseModel.crouching;
        this.riding = baseModel.riding;
        this.rightArmPose = baseModel.rightArmPose;
        this.leftArmPose = baseModel.leftArmPose;
    }

    @Override
    public void setAllVisible(boolean pVisible) {
        super.setAllVisible(pVisible);
        var boneContext = rendererPipeline.context().boneContext();
        boneContext.setAllVisible(pVisible);
    }
}
