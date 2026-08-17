package com.xirc.nichirin.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.client.data.ClientDestructiveDeathState;
import com.xirc.nichirin.client.vfx.FistAuraRenderer;
import com.xirc.nichirin.common.data.MovesetHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders the Destructive Death "blue fist" on the local player's first-person arm.
 *
 * <p>The player-model {@link com.xirc.nichirin.client.renderer.entity.layer.ArmTrailLayer} never runs
 * in first person (the body isn't drawn), so the fist was invisible on your own hands. Here we hook the
 * viewmodel bare-arm render — which vanilla only invokes for an EMPTY hand — and feed
 * {@link FistAuraRenderer} the arm cube built exactly like ArmTrailLayer builds its world-space one,
 * only left in the hand pass's camera/view space.</p>
 *
 * <p>{@code require = 0}: this targets a private vanilla method, so if a future mapping shifts it the
 * fist simply doesn't draw in first person rather than crashing the client.</p>
 */
@Mixin(ItemInHandRenderer.class)
public class FirstPersonFistMixin {

    // Must match ArmTrailLayer's arm cube: inflated past the 2/16 arm surface so it sits over sleeves.
    private static final float R = 3.4f / 16.0f;
    private static final float Y0 = 5.0f / 16.0f;
    private static final float Y1 = 11.2f / 16.0f;

    @Inject(method = "renderPlayerArm", at = @At("TAIL"), require = 0)
    private void nichirin$blueFistFirstPerson(PoseStack poseStack, MultiBufferSource buffer, int combinedLight,
                                              float equipProgress, float swingProgress, HumanoidArm arm,
                                              CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null || player.isInvisible()) return;
        if (!"destructive_death".equals(MovesetHelper.getDemonMovesetId(player))) return;

        EntityRenderer<?> renderer = mc.getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer playerRenderer)) return;
        PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();
        ModelPart armPart = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;

        // Overdrive is a local-player flag, and the viewmodel is only ever the local player's.
        boolean red = ClientDestructiveDeathState.overdriveEnabled;

        // At TAIL the pose is at the arm base (view space); mirror the arm ModelPart's own transform,
        // then read the same 8 cube corners ArmTrailLayer uses — now in camera/view space.
        poseStack.pushPose();
        armPart.translateAndRotate(poseStack);
        Matrix4f mat = poseStack.last().pose();
        float x0 = -R, x1 = R, z0 = -R, z1 = R;
        Vec3[] cube = {
                viewPoint(mat, x0, Y0, z0), viewPoint(mat, x1, Y0, z0),
                viewPoint(mat, x1, Y0, z1), viewPoint(mat, x0, Y0, z1),
                viewPoint(mat, x0, Y1, z0), viewPoint(mat, x1, Y1, z0),
                viewPoint(mat, x1, Y1, z1), viewPoint(mat, x0, Y1, z1)
        };
        poseStack.popPose();

        FistAuraRenderer.renderFirstPerson(buffer, cube, red);
    }

    private static Vec3 viewPoint(Matrix4f matrix, float x, float y, float z) {
        Vector4f v = matrix.transform(new Vector4f(x, y, z, 1.0f));
        return new Vec3(v.x(), v.y(), v.z());
    }
}
