package com.xirc.nichirin.client.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

/**
 * A woozy, continuous camera wobble applied while the local player has the Slammed effect — a
 * rolling, off-kilter "your head got rung" distortion. Unlike {@link ImpactCameraShake} (a sharp,
 * decaying jitter), this is a slow figure-eight sway with mismatched frequencies on each axis so it
 * reads as disorientation rather than a hit. Driven from {@code GameRenderer.bobHurt} (which is
 * always called, so it works regardless of the view-bobbing setting).
 */
@Environment(EnvType.CLIENT)
public final class SlammedWobble {

    private static float intensity;        // smoothed 0..1
    private static float targetIntensity;

    private SlammedWobble() {
    }

    public static void register() {
        ClientTickEvent.CLIENT_POST.register(SlammedWobble::clientTick);
    }

    private static void clientTick(Minecraft minecraft) {
        boolean slammed = minecraft.player != null
                && minecraft.player.hasEffect(NichirinEffectRegistry.slammed());
        targetIntensity = slammed ? 1.0f : 0.0f;
        // Ease in a little faster than it eases out so the recovery lingers woozily.
        float factor = targetIntensity > intensity ? 0.16f : 0.07f;
        intensity += (targetIntensity - intensity) * factor;
        if (intensity < 0.002f && targetIntensity == 0.0f) {
            intensity = 0.0f;
        }
    }

    public static void apply(PoseStack poseStack, float partialTick) {
        if (intensity <= 0.002f) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        float t = minecraft.level.getGameTime() + partialTick;

        // Mismatched slow frequencies = a drifting figure-eight wobble, scaled by the eased intensity.
        float roll = (float) Math.sin(t * 0.45f) * 6.5f * intensity;
        float pitch = (float) Math.sin(t * 0.31f + 1.3f) * 3.0f * intensity;
        float yaw = (float) Math.cos(t * 0.37f) * 3.0f * intensity;
        float swayX = (float) Math.sin(t * 0.23f) * 0.10f * intensity;
        float swayY = (float) Math.cos(t * 0.29f) * 0.06f * intensity;

        poseStack.translate(swayX, swayY, 0.0f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
    }
}
