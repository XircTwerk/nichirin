package com.xirc.nichirin.client.renderer.entity.npc;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.renderer.entity.BaseAZNichirinEntityRenderer;
import com.xirc.nichirin.client.renderer.entity.animator.WaterBreathingTrainerAnimator;
import com.xirc.nichirin.common.entity.npc.WaterBreathingTrainerEntity;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import mod.azure.azurelib.render.layer.AzBlockAndItemLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class WaterBreathingTrainerRenderer extends BaseAZNichirinEntityRenderer<WaterBreathingTrainerEntity> {

    private static final ResourceLocation GEO = new ResourceLocation(BreathOfNichirin.MOD_ID, "geo/urokodaki_npc.geo.json");
    private static final ResourceLocation TEX = new ResourceLocation(BreathOfNichirin.MOD_ID, "textures/entity/npc/urokodaki_skin.png");

    public WaterBreathingTrainerRenderer(EntityRendererProvider.Context context) {
        super(
                AzEntityRendererConfig.<WaterBreathingTrainerEntity>builder(GEO, TEX)
                        .setAnimatorProvider(WaterBreathingTrainerAnimator::new)
                        .addRenderLayer(new AzBlockAndItemLayer<>() {
                            @Override
                            public ItemStack itemStackForBone(AzBone bone, WaterBreathingTrainerEntity entity) {
                                if ("RightHandLocator".equals(bone.getName())) {
                                    return entity.getItemBySlot(EquipmentSlot.MAINHAND);
                                }
                                return null;
                            }

                            @Override
                            protected ItemDisplayContext getTransformTypeForStack(AzBone bone, ItemStack stack, WaterBreathingTrainerEntity entity) {
                                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
                            }
                        })
                        .build(),
                context,
                TEX
        );
    }
}
