package com.xirc.nichirin.client.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

public final class ImpactCameraShake {
    private static final int DURATION_TICKS = 12;
    private static int ticksRemaining;
    private static float magnitude = 1.0F;

    private ImpactCameraShake() {
    }

    public static void trigger(float strength) {
        trigger(strength, 1.0F);
    }

    /**
     * @param minFloor minimum allowed magnitude. The default {@link #trigger(float)} floors at 1.0;
     *                 callers that want a gentler shake (e.g. the gun) can pass a lower floor.
     */
    public static void trigger(float strength, float minFloor) {
        magnitude = Math.max(minFloor, Math.min(2.5F, strength));
        ticksRemaining = DURATION_TICKS;
    }

    public static void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }

    public static void apply(PoseStack poseStack, float partialTick) {
        if (ticksRemaining <= 0) {
            return;
        }

        float age = DURATION_TICKS - ticksRemaining + partialTick;
        float progress = Math.max(0.0F, Math.min(1.0F, age / DURATION_TICKS));
        float envelope = (1.0F - progress) * (1.0F - progress);
        float frame = age * 31.0F;

        float x = pseudo(frame, 17.23F) * 0.12F * magnitude * envelope;
        float y = pseudo(frame, 51.91F) * 0.08F * magnitude * envelope;
        float roll = pseudo(frame, 93.47F) * 7.0F * magnitude * envelope;

        poseStack.translate(x, y, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
    }

    private static float pseudo(float value, float salt) {
        return (float) Math.sin(value * 12.9898F + salt) * 0.5F;
    }
}
