package com.xirc.nichirin.client.renderer.armor;

import mod.azure.azurelib.animatable.GeoItem;
import mod.azure.azurelib.cache.object.GeoBone;
import mod.azure.azurelib.model.GeoModel;
import mod.azure.azurelib.renderer.GeoArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public class NichirinArmorRenderer<T extends Item & GeoItem> extends GeoArmorRenderer<T> {

    public NichirinArmorRenderer(final GeoModel<T> model) {
        super(model);
    }

    @Override
    public GeoBone getHeadBone() {
        return this.model.getBone("Head").orElse(super.getHeadBone());
    }

    @Nullable
    @Override
    public GeoBone getBodyBone() {
        return this.model.getBone("chestplate").orElse(super.getBodyBone());
    }

    @Nullable
    @Override
    public GeoBone getRightArmBone() {
        return this.model.getBone("rightArm").orElse(super.getRightArmBone());
    }

    @Nullable
    @Override
    public GeoBone getLeftArmBone() {
        return this.model.getBone("leftArm").orElse(super.getLeftArmBone());
    }

    @Nullable
    @Override
    public GeoBone getRightLegBone() {
        return this.model.getBone("rightLeg").orElse(super.getRightLegBone());
    }

    @Nullable
    @Override
    public GeoBone getLeftLegBone() {
        return this.model.getBone("leftLeg").orElse(super.getLeftLegBone());
    }

    @Nullable
    @Override
    public GeoBone getRightBootBone() {
        return this.model.getBone("rightBoot").orElse(super.getRightBootBone());
    }

    @Nullable
    @Override
    public GeoBone getLeftBootBone() {
        return this.model.getBone("leftBoot").orElse(super.getLeftBootBone());
    }

    /**
     * Checks if a player has a slim (Alex) skin model - Aggressive multiplayer approach
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
                    // Check if the decoded JSON contains "slim" model specification
                    if (texturesJson.contains("\"slim\"")) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}

        try {
            // Method 2: Check the player's model name after ensuring skin is loaded
            if (player.getSkinTextureLocation() != null) {
                return "slim".equals(player.getModelName());
            }
        } catch (Exception ignored) {}

        // Method 3: UUID fallback (Minecraft's default algorithm)
        return (player.getUUID().hashCode() & 1) == 1;
    }

    @Override
    protected void applyBaseTransformations(HumanoidModel<?> baseModel) {
        // FIRST: Apply base transformations
        super.applyBaseTransformations(baseModel);

        // THEN: Apply slim scaling ONLY for regular armor pieces, NOT cape pieces
        if (this.currentEntity instanceof AbstractClientPlayer player) {
            boolean isSlim = isSlimPlayer(player);

            if (isSlim) {
                GeoBone leftArm = getLeftArmBone();
                GeoBone rightArm = getRightArmBone();

                // Only scale regular arm bones, not cape bones
                if (leftArm != null && !isCapeArm(leftArm)) leftArm.setScaleX(0.75f);
                if (rightArm != null && !isCapeArm(rightArm)) rightArm.setScaleX(0.75f);
            }
        }
    }

    /**
     * Check if a bone is a cape arm bone (to avoid scaling cape arms)
     */
    private boolean isCapeArm(GeoBone bone) {
        // Check if this is a cape-specific bone by looking for cape-related bone names
        return bone == this.model.getBone("capeLeft").orElse(null) ||
                bone == this.model.getBone("capeRight").orElse(null) ||
                bone == this.model.getBone("CapeLeft").orElse(null) ||
                bone == this.model.getBone("CapeRight").orElse(null);
    }
}