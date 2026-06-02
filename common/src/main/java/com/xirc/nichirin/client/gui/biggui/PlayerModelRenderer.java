package com.xirc.nichirin.client.gui.biggui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Renders 3D player models with mouse-following rotation like the inventory screen
 */
public class PlayerModelRenderer {

    /**
     * Renders a player model that follows the mouse cursor (like inventory screen)
     */
    public static void renderPlayerFollowingMouse(GuiGraphics graphics, int x, int y, int size,
                                                  float mouseX, float mouseY, Player player) {
        float f = (float)Math.atan((x - mouseX) / 40.0F);
        float g = (float)Math.atan((y - 50 - mouseY) / 40.0F);

        Quaternionf quaternionf = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf quaternionf2 = new Quaternionf().rotateX(g * 20.0F * (float) (Math.PI / 180.0));
        quaternionf.mul(quaternionf2);

        // Store original rotations
        float h = player.yBodyRot;
        float i = player.getYRot();
        float j = player.getXRot();
        float k = player.yHeadRotO;
        float l = player.yHeadRot;

        // Apply mouse-based rotations
        player.yBodyRot = 180.0F + f * 20.0F;
        player.setYRot(180.0F + f * 40.0F);
        player.setXRot(-g * 20.0F);
        player.yHeadRot = player.getYRot();
        player.yHeadRotO = player.getYRot();

        // Render the entity
        renderEntityInInventory(graphics, x, y, size, quaternionf, quaternionf2, player);

        // Restore original rotations
        player.yBodyRot = h;
        player.setYRot(i);
        player.setXRot(j);
        player.yHeadRotO = k;
        player.yHeadRot = l;
    }

    /**
     * Renders a player model with automatic rotation (current behavior)
     */
    public static void renderPlayerWithRotation(GuiGraphics graphics, int x, int y, int size, Player player) {
        // Calculate rotation based on time
        float rotation = (System.currentTimeMillis() / 50L % 360L) * 0.017453292F;

        graphics.pose().pushPose();
        graphics.pose().translate(x, y + 55, 50.0F); // Moved down 5 pixels from +50 to +55
        graphics.pose().scale((float)size, (float)size, (float)size);

        Quaternionf quaternion = Axis.ZP.rotationDegrees(180.0F);
        Quaternionf quaternion2 = Axis.XP.rotationDegrees(-20.0F);
        Quaternionf quaternion3 = Axis.YP.rotation(rotation);

        quaternion.mul(quaternion2);
        quaternion.mul(quaternion3);

        graphics.pose().mulPose(quaternion);

        Lighting.setupForEntityInInventory();

        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        entityrenderdispatcher.setRenderShadow(false);

        MultiBufferSource.BufferSource bufferSource = graphics.bufferSource();
        RenderSystem.runAsFancy(() -> {
            entityrenderdispatcher.render(player, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F,
                    graphics.pose(), bufferSource, 15728880);
        });

        bufferSource.endBatch();
        entityrenderdispatcher.setRenderShadow(true);

        graphics.pose().popPose();
        Lighting.setupFor3DItems();
    }

    /**
     * Core entity rendering method (copied from InventoryScreen)
     */
    private static void renderEntityInInventory(GuiGraphics guiGraphics, int x, int y, int scale,
                                                Quaternionf pose, Quaternionf cameraOrientation, LivingEntity entity) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((double)x, (double)y + 55, 50.0); // Moved down 5 pixels from +50 to +55
        guiGraphics.pose().mulPose(new Matrix4f().scaling(scale, scale, -scale));
        guiGraphics.pose().mulPose(pose);

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        if (cameraOrientation != null) {
            cameraOrientation.conjugate();
            entityRenderDispatcher.overrideCameraOrientation(cameraOrientation);
        }

        entityRenderDispatcher.setRenderShadow(false);
        RenderSystem.runAsFancy(() -> entityRenderDispatcher.render(entity, 0.0, 0.0, 0.0, 0.0F, 1.0F,
                guiGraphics.pose(), guiGraphics.bufferSource(), 15728880));
        guiGraphics.flush();
        entityRenderDispatcher.setRenderShadow(true);
        guiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
    }
}