package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.BreathOfNichirin;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.armor.AzArmorRenderer;
import mod.azure.azurelib.render.armor.AzArmorRendererConfig;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Base armor renderer for Nichirin armors using AzureLib 3.x API
 */
public class NichirinArmorRenderer extends AzArmorRenderer {

    protected Entity currentEntity;
    protected EquipmentSlot currentSlot;
    protected HumanoidModel<?> currentBaseModel;
    protected AzBakedModel currentModel;

    protected NichirinArmorRenderer(String modelName, String textureName) {
        super(createConfig(modelName, textureName));
    }

    protected NichirinArmorRenderer(String armorName) {
        this(armorName, armorName);
    }

    private static AzArmorRendererConfig createConfig(String modelName, String textureName) {
        ResourceLocation geoModel = BreathOfNichirin.id("geo/" + modelName + ".geo.json");
        ResourceLocation texture = BreathOfNichirin.id("textures/armor/" + textureName + ".png");

        return AzArmorRendererConfig.builder(geoModel, texture)
                .build();
    }

    @Override
    public void prepForRender(@Nullable Entity entity, ItemStack stack, @Nullable EquipmentSlot slot, @Nullable HumanoidModel<?> baseModel) {
        super.prepForRender(entity, stack, slot, baseModel);

        this.currentEntity = entity;
        this.currentSlot = slot;
        this.currentBaseModel = baseModel;

        // Get the current baked model from the renderer after super call
        this.currentModel = getCurrentBakedModel();

        // Apply transformations after super call
        if (entity != null && slot != null && baseModel != null && this.currentModel != null) {
            applyBoneTransformations();
            applyBoneVisibilityBySlot(slot);
        }
    }

    /**
     * Override this method to get the baked model from the renderer
     * This should be implemented based on how AzArmorRenderer exposes the model
     */
    protected AzBakedModel getCurrentBakedModel() {
        // Try to access through reflection or protected field
        try {
            java.lang.reflect.Field modelField = AzArmorRenderer.class.getDeclaredField("model");
            modelField.setAccessible(true);
            return (AzBakedModel) modelField.get(this);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Apply custom bone transformations - override in subclasses
     */
    protected void applyBoneTransformations() {
        // Base implementation - override in subclasses
    }

    /**
     * Apply bone visibility based on equipment slot - override in subclasses
     */
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        // Base implementation - override in subclasses
        setAllVisible(false);
    }

    /**
     * Apply body transformation helper
     */
    protected void applyBodyTransform(AzBone bodyBone) {
        if (bodyBone != null && currentBaseModel != null && currentBaseModel.body != null) {
            matchRotation(currentBaseModel.body, bodyBone);
        }
    }

    /**
     * Apply arm transformation helper
     */
    protected void applyArmTransform(AzBone armBone, ModelPart armPart, boolean isLeft) {
        if (armBone != null && armPart != null) {
            matchRotation(armPart, armBone);
        }
    }

    /**
     * Utility method to check if a player has a slim (Alex) skin model
     */
    protected boolean isSlimPlayer(AbstractClientPlayer player) {
        try {
            // Method 1: Direct GameProfile access
            com.mojang.authlib.GameProfile profile = player.getGameProfile();
            if (profile != null) {
                com.mojang.authlib.properties.PropertyMap properties = profile.getProperties();
                if (properties.containsKey("textures")) {
                    com.mojang.authlib.properties.Property textureProperty = properties.get("textures").iterator().next();
                    String texturesJson = new String(java.util.Base64.getDecoder().decode(textureProperty.getValue()));
                    if (texturesJson.contains("\"slim\"")) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}

        try {
            // Method 2: Check the player's model name
            if (player.getSkinTextureLocation() != null) {
                return "slim".equals(player.getModelName());
            }
        } catch (Exception ignored) {}

        // Method 3: UUID fallback (Minecraft's default algorithm)
        return (player.getUUID().hashCode() & 1) == 1;
    }

    /**
     * Utility method to set bone visibility
     */
    protected void setBoneVisible(@Nullable AzBone bone, boolean visible) {
        if (bone != null) {
            bone.setHidden(!visible);
        }
    }

    /**
     * Utility method to set all bones invisible/visible
     */
    protected void setAllVisible(boolean visible) {
        if (this.currentModel != null) {
            for (AzBone bone : this.currentModel.getTopLevelBones()) {
                setAllBonesRecursive(bone, visible);
            }
        }
    }

    /**
     * Recursively set visibility for bone and all children
     */
    private void setAllBonesRecursive(AzBone bone, boolean visible) {
        bone.setHidden(!visible);
        for (AzBone child : bone.getChildBones()) {
            setAllBonesRecursive(child, visible);
        }
    }

    /**
     * Helper to get a bone by name
     */
    protected @Nullable AzBone getBone(String boneName) {
        return this.currentModel != null ? this.currentModel.getBone(boneName).orElse(null) : null;
    }

    /**
     * Helper to match rotation from base model part to bone
     */
    protected void matchRotation(ModelPart modelPart, AzBone bone) {
        if (modelPart != null && bone != null) {
            bone.setRotX(modelPart.xRot);
            bone.setRotY(modelPart.yRot);
            bone.setRotZ(modelPart.zRot);
        }
    }
}