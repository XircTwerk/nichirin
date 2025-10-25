package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.animator.NichirinArmorAnimator;
import com.xirc.nichirin.common.item.armor.NichirinArmorItem;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.armor.AzArmorRenderer;
import mod.azure.azurelib.render.armor.AzArmorRendererConfig;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class NichirinArmorRenderer<T extends NichirinArmorItem> extends AzArmorRenderer<T> {

    protected Entity currentEntity;
    protected EquipmentSlot currentSlot;

    public NichirinArmorRenderer(String modelName, String textureName) {
        super(createConfig(modelName, textureName));
    }

    public NichirinArmorRenderer(String armorName) {
        this(armorName, armorName);
    }

    private static AzArmorRendererConfig createConfig(String modelName, String textureName) {
        ResourceLocation geoModel = BreathOfNichirin.id("geo/" + modelName + ".geo.json");
        ResourceLocation texture = BreathOfNichirin.id("textures/armor/" + textureName + ".png");

        return AzArmorRendererConfig.builder(geoModel, texture)
                .setAnimatorProvider(() -> new NichirinArmorAnimator(modelName))
                .build();
    }

    @Override
    public void prepForRender(@Nullable Entity entity, ItemStack stack, @Nullable EquipmentSlot slot, @Nullable HumanoidModel<?> baseModel) {
        this.currentEntity = entity;
        this.currentSlot = slot;
        super.prepForRender(entity, stack, slot, baseModel);
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
     * Utility method to set all bones invisible
     */
    protected void setAllVisible(boolean visible) {
        if (this.model != null) {
            this.model.getBones().forEach(bone -> bone.setHidden(!visible));
        }
    }
}