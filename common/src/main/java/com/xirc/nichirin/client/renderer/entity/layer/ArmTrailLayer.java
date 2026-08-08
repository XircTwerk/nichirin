package com.xirc.nichirin.client.renderer.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.client.afterimage.AfterimageRenderState;
import com.xirc.nichirin.client.data.ClientDestructiveDeathState;
import com.xirc.nichirin.client.vfx.FistAuraRenderer;
import com.xirc.nichirin.common.data.MovesetHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.UUID;

/**
 * Feeds each arm's hand-cube geometry (in world space) to {@link FistAuraRenderer}, which draws the
 * Destructive Death "blue fist" — a pixelized energy cube + full-width box-tube trail — through the
 * VFX pixel pass. This layer only captures; it renders nothing itself.
 */
@Environment(EnvType.CLIENT)
public class ArmTrailLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    // Local (block) coords. The arm cube runs y = -2/16 .. 10/16 from the shoulder pivot (larger Y =
    // hand end). R is inflated past the 2/16 arm surface so the cube sits OVER armor/kimonos.
    private static final float R = 3.4f / 16.0f;
    private static final float Y0 = 5.0f / 16.0f;
    private static final float Y1 = 11.2f / 16.0f;

    public ArmTrailLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        // Ghost/afterimage passes re-render the whole player many times per frame — skip those.
        if (AfterimageRenderState.isRendering()) return;
        if (!"destructive_death".equals(MovesetHelper.getDemonMovesetId(player))) return;

        // Other players' Overdrive isn't synced to observers, so only the local player reddens.
        boolean red = player == Minecraft.getInstance().player && ClientDestructiveDeathState.overdriveEnabled;
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        PlayerModel<AbstractClientPlayer> model = getParentModel();

        // A fist only forms on an EMPTY hand — each hand is independent. Map each model arm to the
        // interaction hand it holds (depends on the player's handedness).
        HumanoidArm mainArm = player.getMainArm();
        InteractionHand rightHand = mainArm == HumanoidArm.RIGHT ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        InteractionHand leftHand = mainArm == HumanoidArm.RIGHT ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

        if (player.getItemInHand(rightHand).isEmpty()) {
            captureArm(poseStack, camera, model.rightArm, player.getUUID(), 0, red);
        }
        if (player.getItemInHand(leftHand).isEmpty()) {
            captureArm(poseStack, camera, model.leftArm, player.getUUID(), 1, red);
        }
    }

    private void captureArm(PoseStack poseStack, Vec3 camera, ModelPart arm, UUID id, int armIndex, boolean red) {
        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        Matrix4f mat = poseStack.last().pose();
        float x0 = -R, x1 = R, z0 = -R, z1 = R;
        // [0..3] wrist ring (Y0), [4..7] hand ring (Y1) — order consumed by FistAuraRenderer.
        Vec3[] cube = {
                worldPoint(mat, camera, x0, Y0, z0), worldPoint(mat, camera, x1, Y0, z0),
                worldPoint(mat, camera, x1, Y0, z1), worldPoint(mat, camera, x0, Y0, z1),
                worldPoint(mat, camera, x0, Y1, z0), worldPoint(mat, camera, x1, Y1, z0),
                worldPoint(mat, camera, x1, Y1, z1), worldPoint(mat, camera, x0, Y1, z1)
        };
        poseStack.popPose();
        FistAuraRenderer.capture(id, armIndex, cube, red);
    }

    /** Transforms a model-local point through the (camera-relative) pose to absolute world coords. */
    private static Vec3 worldPoint(Matrix4f matrix, Vec3 camera, float x, float y, float z) {
        Vector4f v = matrix.transform(new Vector4f(x, y, z, 1.0f));
        return new Vec3(v.x() + camera.x, v.y() + camera.y, v.z() + camera.z);
    }
}
