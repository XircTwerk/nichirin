package com.xirc.nichirin.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Plays a "medium_hit" flinch on any humanoid mob whenever it gets hurt — the hit_animations.json
 * pose, ported to code. The bones in that animation (head/torso/arms/legs) map 1:1 onto
 * {@link HumanoidModel}, so the same pose works for every humanoid (zombies, skeletons, villagers,
 * etc.). It's driven off the synced vanilla {@code hurtTime}, so it needs no networking and fires
 * on any damage source.
 *
 * <p>Players are skipped here — they get the richer small/medium/large flinch via the PlayerAnim
 * system (see HitAnimationHandler / NichirinAnimations).</p>
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {

    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart body;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightLeg;
    @Shadow @Final public ModelPart leftLeg;

    private static final float DEG = (float) (Math.PI / 180.0);

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void nichirin$hitFlinch(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                    float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player) return;
        if (entity.hurtTime <= 0) return;

        // hurtDuration is 10; this decays 1 -> 0 over ~0.5s after the hit.
        float r = DEG * (entity.hurtTime / 10.0f);

        // medium_hit pose (degrees), added on top of the normal animation and scaled by the decay.
        head.xRot += -10.0f * r;
        body.xRot += 10.0f * r;
        rightArm.xRot += 12.4539f * r;
        rightArm.yRot += -1.0809f * r;
        rightArm.zRot += 12.382f * r;
        leftArm.xRot += 12.4539f * r;
        leftArm.yRot += 1.0809f * r;
        leftArm.zRot += -12.382f * r;
        rightLeg.xRot += -7.5f * r;
        rightLeg.zRot += 7.5f * r;
        leftLeg.xRot += -7.5f * r;
        leftLeg.zRot += -7.5f * r;
    }
}
