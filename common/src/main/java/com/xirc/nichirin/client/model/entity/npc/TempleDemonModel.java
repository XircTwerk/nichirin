package com.xirc.nichirin.client.model.entity.npc;

import com.xirc.nichirin.client.renderer.entity.npc.NPCAnimationManager;
import com.xirc.nichirin.common.entity.npc.TempleDemonEntity;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import mod.azure.azurelib.cache.object.BakedGeoModel;
import mod.azure.azurelib.cache.object.GeoBone;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.model.GeoModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class TempleDemonModel extends GeoModel<TempleDemonEntity> {

    private int lastLoggedTick = -100; // Track last log to prevent spam

    @Override
    public ResourceLocation getModelResource(TempleDemonEntity animatable) {
        return new ResourceLocation("nichirin", "geo/temple_demon.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TempleDemonEntity animatable) {
        return new ResourceLocation("nichirin", "textures/entity/npc/temple_demon.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TempleDemonEntity animatable) {
        return null;
    }

    @Override
    public void setCustomAnimations(TempleDemonEntity animatable, long instanceId, AnimationState<TempleDemonEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        BakedGeoModel model = this.getBakedModel(this.getModelResource(animatable));
        if (model != null) {
            applyAnimations(animatable, model, animationState);
        }
    }

    private void applyAnimations(TempleDemonEntity entity, BakedGeoModel model, AnimationState<TempleDemonEntity> animationState) {
        float limbSwing = animationState.getLimbSwing();
        float limbSwingAmount = animationState.getLimbSwingAmount();
        float headYaw = animationState.getData(mod.azure.azurelib.constant.DataTickets.ENTITY_MODEL_DATA).netHeadYaw();
        float headPitch = animationState.getData(mod.azure.azurelib.constant.DataTickets.ENTITY_MODEL_DATA).headPitch();

        GeoBone head = model.getBone("Head").orElse(null);
        GeoBone torso = model.getBone("Torso").orElse(null);
        GeoBone rightArm = model.getBone("rightArm").orElse(null);
        GeoBone leftArm = model.getBone("leftArm").orElse(null);
        GeoBone rightLeg = model.getBone("rightLeg").orElse(null);
        GeoBone leftLeg = model.getBone("leftLeg").orElse(null);

        resetBoneTransforms(head, torso, rightArm, leftArm, rightLeg, leftLeg);

        if (head != null) {
            head.setRotX(headPitch * ((float) Math.PI / 180F));
            head.setRotY(headYaw * ((float) Math.PI / 180F));
        }

        // Try to get animation from NPCAnimationManager
        ModifierLayer<IAnimation> animationLayer = NPCAnimationManager.getAnimationLayer(entity.getId());

        if (animationLayer != null) {
            IAnimation currentAnim = animationLayer.getAnimation();

            if (currentAnim != null && currentAnim.isActive()) {
                if (currentAnim instanceof KeyframeAnimationPlayer player) {
                    KeyframeAnimation animation = player.getData();

                    if (animation != null) {
                        float animTime = (entity.tickCount - entity.getAnimationStartTick()) / 20.0f;

                        // Log once per second
                        if (entity.tickCount - lastLoggedTick >= 20) {
                            System.out.println("Model: Playing animation: " + entity.getCurrentPlayerAnimation() + " at time: " + animTime);
                            lastLoggedTick = entity.tickCount;
                        }

                        applyPlayerAnimatorAnimation(animation, animTime, head, torso, rightArm, leftArm, rightLeg, leftLeg);
                        return;
                    }
                }
            }
        }

        // Default animations
        applyDefaultMovement(entity, limbSwing, limbSwingAmount, head, torso, rightArm, leftArm, rightLeg, leftLeg);
    }

    private void resetBoneTransforms(GeoBone... bones) {
        for (GeoBone bone : bones) {
            if (bone != null) {
                bone.setRotX(0);
                bone.setRotY(0);
                bone.setRotZ(0);
                bone.setPosX(0);
                bone.setPosY(0);
                bone.setPosZ(0);
            }
        }
    }

    private void applyPlayerAnimatorAnimation(KeyframeAnimation animation, float animTime,
                                              GeoBone head, GeoBone torso, GeoBone rightArm, GeoBone leftArm, GeoBone rightLeg, GeoBone leftLeg) {
        int tick = (int)(animTime * 20);

        applyBoneData(head, animation, "head", tick);
        applyBoneData(head, animation, "Head", tick);
        applyBoneData(torso, animation, "torso", tick);
        applyBoneData(torso, animation, "Torso", tick);
        applyBoneData(torso, animation, "body", tick);
        applyBoneData(rightArm, animation, "rightArm", tick);
        applyBoneData(leftArm, animation, "leftArm", tick);
        applyBoneData(rightLeg, animation, "rightLeg", tick);
        applyBoneData(leftLeg, animation, "leftLeg", tick);
    }

    private void applyBoneData(GeoBone bone, KeyframeAnimation animation, String boneName, int tick) {
        if (bone == null) return;

        try {
            KeyframeAnimation.StateCollection states = animation.getPart(boneName);
            if (states == null) return;

            float pitch = sampleStateValue(states.pitch, tick);
            float yaw = sampleStateValue(states.yaw, tick);
            float roll = sampleStateValue(states.roll, tick);
            float x = sampleStateValue(states.x, tick);
            float y = sampleStateValue(states.y, tick);
            float z = sampleStateValue(states.z, tick);

            bone.setRotX(pitch * ((float) Math.PI / 180F));
            bone.setRotY(yaw * ((float) Math.PI / 180F));
            bone.setRotZ(roll * ((float) Math.PI / 180F));
            bone.setPosX(x);
            bone.setPosY(y);
            bone.setPosZ(z);

        } catch (Exception e) {
            // Silent fail
        }
    }

    private float sampleStateValue(KeyframeAnimation.StateCollection.State state, int tick) {
        if (state == null || !state.isEnabled()) {
            return 0;
        }

        var keyframes = state.getKeyFrames();
        if (keyframes.isEmpty()) {
            return state.defaultValue;
        }

        KeyframeAnimation.KeyFrame before = null;
        KeyframeAnimation.KeyFrame after = null;

        for (KeyframeAnimation.KeyFrame kf : keyframes) {
            if (kf.tick <= tick) {
                before = kf;
            }
            if (kf.tick >= tick && after == null) {
                after = kf;
            }
        }

        if (before == null && after == null) {
            return state.defaultValue;
        }
        if (before == null) {
            return after.value;
        }
        if (after == null) {
            return before.value;
        }
        if (before.tick == after.tick) {
            return before.value;
        }

        float t = (float)(tick - before.tick) / (after.tick - before.tick);
        t = Mth.clamp(t, 0, 1);
        float easedT = before.ease.invoke(t);

        return Mth.lerp(easedT, before.value, after.value);
    }

    private void applyDefaultMovement(TempleDemonEntity entity, float limbSwing, float limbSwingAmount,
                                      GeoBone head, GeoBone torso, GeoBone rightArm, GeoBone leftArm, GeoBone rightLeg, GeoBone leftLeg) {

        if (limbSwingAmount > 0.1f) {
            if (rightArm != null) rightArm.setRotX(Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount);
            if (leftArm != null) leftArm.setRotX(Mth.cos(limbSwing * 0.6662F) * limbSwingAmount);
            if (rightLeg != null) rightLeg.setRotX(Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount);
            if (leftLeg != null) leftLeg.setRotX(Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount);
        }

        if (limbSwingAmount < 0.1f && torso != null) {
            float breathing = Mth.sin(entity.tickCount * 0.067f) * 0.02f;
            torso.setPosY(breathing);
        }
    }
}