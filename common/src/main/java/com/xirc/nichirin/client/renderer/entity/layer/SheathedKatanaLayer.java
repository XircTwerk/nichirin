package com.xirc.nichirin.client.renderer.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.common.item.katana.Katana;
import com.xirc.nichirin.common.util.ItemStackData;
import com.xirc.nichirin.common.system.sheathing.PlayerSheathData;
import com.xirc.nichirin.common.system.sheathing.SheathPosition;
import com.xirc.nichirin.common.system.sheathing.SheathSlotData;
import com.xirc.nichirin.common.system.sheathing.SheathState;
import com.xirc.nichirin.common.system.sheathing.SheathingManager;
import com.xirc.nichirin.registry.NichirinItemRegistry;
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
            if (!(stack.getItem() instanceof Katana)) continue;
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
        // Twin substitution only applies to hip slots — back slots use FIXED display context
        // which has identical transforms for LEFT/RIGHT models, so swapping there does nothing
        // useful and changes nothing visually. Keeping the original stack for back means back
        // rendering stays bit-for-bit identical to its pre-fix behavior (which the user said
        // was correct), while hips still get the LEFT→RIGHT twin swap that fixed the flipped
        // orientation there.
        ItemStack renderStack = isHipSlot(slot.getPosition()) ? renderTwin(stack).copy() : stack.copy();
        ItemStackData.update(renderStack, tag -> tag.putBoolean("nichirin_sheathed_render", true));
        Minecraft.getInstance().getItemRenderer().renderStatic(player, renderStack, displayContext(slot),
                false, poseStack, buffer, player.level(), packedLight, OverlayTexture.NO_OVERLAY,
                player.getId() + slot.getPosition().ordinal());
        poseStack.popPose();
    }

    private boolean isHipSlot(SheathPosition position) {
        return position == SheathPosition.LEFT_HIP || position == SheathPosition.RIGHT_HIP;
    }

    /**
     * If the stack is the left half of a dual-katana pair, return a fresh stack of the right
     * half so sheathed rendering reuses the canonical orientation. NBT (custom names, damage,
     * etc.) is not copied because the sheathed render only needs the model — gameplay state
     * lives on the actual stored {@link ItemStack}, which is untouched.
     */
    private ItemStack renderTwin(ItemStack stack) {
        if (stack.is(NichirinItemRegistry.LEFT_BEAST_KATANA.get())) {
            return new ItemStack(NichirinItemRegistry.RIGHT_BEAST_KATANA.get());
        }
        if (stack.is(NichirinItemRegistry.LEFT_SOUND_KATANA.get())) {
            return new ItemStack(NichirinItemRegistry.RIGHT_SOUND_KATANA.get());
        }
        return stack;
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
            // BACK / BACK_2 are the original pre-fix transforms. The single-katana case was
            // already working — leave it alone.
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

    private ItemDisplayContext displayContext(SheathSlotData slot) {
        return switch (slot.getPosition()) {
            case BACK, BACK_2 -> ItemDisplayContext.FIXED;
            default -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        };
    }

    private Quaternionf rotation(float x, float y, float z) {
        return new Quaternionf()
                .rotationXYZ((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
    }
}