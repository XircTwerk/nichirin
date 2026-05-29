package com.xirc.nichirin.client.renderer.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.system.sheathing.PlayerSheathData;
import com.xirc.nichirin.common.system.sheathing.SheathPosition;
import com.xirc.nichirin.common.system.sheathing.SheathSlotData;
import com.xirc.nichirin.common.system.sheathing.SheathState;
import com.xirc.nichirin.common.system.sheathing.SheathingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

public class SheathedKatanaLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public SheathedKatanaLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        PlayerSheathData data = SheathingManager.get(player);
        for (SheathSlotData slot : data.getSlots()) {
            if (!shouldRender(slot)) continue;
            ItemStack stack = slot.hasStoredSword()
                    ? slot.getStoredSword()
                    : player.getInventory().getItem(slot.getLinkedHotbarSlot());
            if (!(stack.getItem() instanceof SimpleKatana)) continue;
            renderSlot(poseStack, buffer, packedLight, player, slot, stack);
        }
    }

    private boolean shouldRender(SheathSlotData slot) {
        return slot.isEnabled()
                && slot.isVisible()
                && (slot.getState() == SheathState.SHEATHED || slot.getState() == SheathState.SHEATHING);
    }

    private void renderSlot(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                            AbstractClientPlayer player, SheathSlotData slot, ItemStack stack) {
        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        applySlotTransform(poseStack, slot.getPosition());
        poseStack.scale(0.72f, 0.72f, 0.72f);
        ItemStack renderStack = stack.copy();
        renderStack.getOrCreateTag().putBoolean("nichirin_sheathed_render", true);
        Minecraft.getInstance().getItemRenderer().renderStatic(player, renderStack, displayContext(slot.getPosition()),
                false, poseStack, buffer, player.level(), packedLight, OverlayTexture.NO_OVERLAY,
                player.getId() + slot.getPosition().ordinal());
        poseStack.popPose();
    }

    private void applySlotTransform(PoseStack poseStack, SheathPosition position) {
        switch (position) {
            case LEFT_HIP -> {
                poseStack.translate(0.25, 0.7, 0);
                poseStack.mulPose(rotation(70, 0, 0));
            }
            case RIGHT_HIP -> {
                poseStack.translate(-0.25, 0.70, 0);
                poseStack.mulPose(rotation(70, 0, 0));
            }
            case BACK -> {
                poseStack.translate(0.0, 0.25, 0.15);
                poseStack.mulPose(rotation(0, 0, -90));
            }
            case BACK_2 -> {
                poseStack.translate(0.0, 0.25, 0.15);
                poseStack.mulPose(rotation(0, 180, -90));
            }
        }
    }

    private ItemDisplayContext displayContext(SheathPosition position) {
        return switch (position) {
            case BACK, BACK_2 -> ItemDisplayContext.FIXED;
            default -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        };
    }

    private Quaternionf rotation(float x, float y, float z) {
        return new Quaternionf()
                .rotationXYZ((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
    }
}
