package com.xirc.nichirin.client.model.entity.npc;

import com.xirc.nichirin.common.entity.npc.DemonNPCEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Model for Demon NPCs that extends PlayerModel to ensure compatibility
 * with PlayerAnimator animations. This allows NPCs to use player-based
 * animations seamlessly.
 */
public class DemonNPCModel extends PlayerModel<DemonNPCEntity> {

    public DemonNPCModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(DemonNPCEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        // First call the parent setup for basic animations
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Apply custom NPC-specific animations or modifications
        setupNPCSpecificAnimations(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // The PlayerAnimator system will automatically apply its transformations
        // after this method is called, so we don't need to handle that here
    }

    /**
     * Setup NPC-specific animation modifications
     */
    private void setupNPCSpecificAnimations(DemonNPCEntity entity, float limbSwing,
                                            float limbSwingAmount, float ageInTicks,
                                            float netHeadYaw, float headPitch) {

        // Example: Make demon NPCs have slightly different idle poses
        if (limbSwingAmount < 0.1f) { // If not walking
            // Slightly hunched posture for demons
            this.body.xRot += 0.1f;

            // Slightly bent arms for menacing look
            this.rightArm.xRot += 0.2f;
            this.leftArm.xRot += 0.2f;

            // Custom breathing animation
            float breathingOffset = (float) Math.sin(ageInTicks * 0.067f) * 0.05f;
            this.body.y += breathingOffset;
            this.head.y += breathingOffset;
        }

        // More aggressive stance if entity is aggressive
        if (entity.isAggressive()) {
            // More aggressive stance
            this.rightArm.xRot -= 0.3f;
            this.leftArm.xRot -= 0.3f;
        }

        // Apply demon type specific modifications
        String demonType = entity.getDemonType();
        switch (demonType) {
            case "horned":
                // You could add custom model modifications for horned demons
                break;
            case "winged":
                // You could add wing-related modifications here
                break;
            case "large":
                // Already handled by render scale, but could add pose changes
                this.body.xRot += 0.05f; // Slightly more hunched for large demons
                break;
            default:
                // Standard demon appearance
                break;
        }
    }

    @Override
    public void prepareMobModel(DemonNPCEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);

        // Prepare model for any custom rendering features
        prepareNPCModel(entity, partialTick);
    }

    /**
     * Prepare NPC-specific model features
     */
    private void prepareNPCModel(DemonNPCEntity entity, float partialTick) {
        // Reset any transformations that might interfere with PlayerAnimator
        // PlayerAnimator expects a clean slate to apply its transformations

        // You can set up model visibility here
        this.hat.visible = false; // Demons don't wear hats
        this.jacket.visible = false; // No jacket layer for demons

        // Show/hide model parts based on entity data
        String demonType = entity.getDemonType();
        switch (demonType) {
            case "horned":
                // You could add custom model parts for horns here
                // this.hornLeft.visible = true;
                // this.hornRight.visible = true;
                break;
            case "winged":
                // You could add wing parts here
                // this.wingLeft.visible = true;
                // this.wingRight.visible = true;
                break;
            default:
                // Standard demon appearance - hide any extra parts
                break;
        }
    }

    /**
     * Override to provide custom rendering for specific body parts
     */
    @Override
    public ModelPart getHead() {
        return this.head;
    }

    /**
     * Get the hat part (usually hidden for demons)
     */
    public ModelPart getHat() {
        return this.hat;
    }
    /**
     * Allow external access to model parts for animation system
     */
    public ModelPart getBody() {
        return this.body;
    }

    public ModelPart getRightArm() {
        return this.rightArm;
    }

    public ModelPart getLeftArm() {
        return this.leftArm;
    }

    public ModelPart getRightLeg() {
        return this.rightLeg;
    }

    public ModelPart getLeftLeg() {
        return this.leftLeg;
    }
}