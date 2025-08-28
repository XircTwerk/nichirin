package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.client.model.NichirinArmorModel;
import com.xirc.nichirin.common.item.armor.NichirinArmorItem;
import mod.azure.azurelib.cache.object.GeoBone;
import mod.azure.azurelib.util.RenderUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class RengokuCapeRenderer extends NichirinArmorRenderer<NichirinArmorItem> {
    public RengokuCapeRenderer() {
        super(new NichirinArmorModel<>("rengoku_cape"));
    }

    @Override
    public GeoBone getHeadBone() {
        return this.model.getBone("Head").orElse(super.getHeadBone());
    }

    @Nullable
    @Override
    public GeoBone getBodyBone() {
        return this.model.getBone("Cape").orElse(super.getBodyBone());
    }

    @Nullable
    @Override
    public GeoBone getLeftArmBone() {
        return this.model.getBone("capeLeft").orElse(super.getLeftArmBone());
    }

    @Nullable
    @Override
    public GeoBone getRightArmBone() {
        return this.model.getBone("capeRight").orElse(super.getRightArmBone());
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
    public void prepForRender(@Nullable Entity entity, ItemStack stack, @Nullable EquipmentSlot slot, @Nullable HumanoidModel<?> baseModel) {
        super.prepForRender(entity, stack, slot, baseModel);
    }

    @Override
    protected void applyBaseTransformations(HumanoidModel<?> baseModel) {
        // FIRST: Reset all bone scales to defaults to prevent persistence from cached renderer
        GeoBone cape = this.model.getBone("Cape").orElse(null);
        GeoBone capeLeft = this.model.getBone("capeLeft").orElse(null);
        GeoBone capeRight = this.model.getBone("capeRight").orElse(null);

        // Reset to default scale values (1.0f for all)
        if (cape != null) {
            cape.setScaleX(1.0f);
            cape.setScaleY(1.0f);
            cape.setScaleZ(1.0f);
        }
        if (capeLeft != null) {
            capeLeft.setScaleX(1.0f);
            capeLeft.setScaleY(1.0f);
            capeLeft.setScaleZ(1.0f);
        }
        if (capeRight != null) {
            capeRight.setScaleX(1.0f);
            capeRight.setScaleY(1.0f);
            capeRight.setScaleZ(1.0f);
        }

        // SECOND: Apply base transformations
        super.applyBaseTransformations(baseModel);

        // THIRD: Apply scaling and positioning based on entity type
        if (this.currentEntity instanceof ArmorStand) {
            if (cape != null) {
                cape.setScaleX(1.4f);
                cape.setScaleY(1.4f);
                cape.setScaleZ(1.6f);
            }
            if (capeLeft != null) {
                capeLeft.setScaleX(1.7f);
                capeLeft.setScaleY(1.1f);
                capeLeft.setScaleZ(1.2f);
            }
            if (capeRight != null) {
                capeRight.setScaleX(1.7f);
                capeRight.setScaleY(1.1f);
                capeRight.setScaleZ(1.2f);
            }

            // Apply positioning for armor stands
            if (cape != null) {
                ModelPart bodyPart = baseModel.body;
                RenderUtils.matchModelPartRot(bodyPart, cape);
                cape.updatePosition(bodyPart.x, -bodyPart.y + 1.5f, bodyPart.z - 1f);
            }

            if (capeLeft != null) {
                ModelPart leftArmPart = baseModel.leftArm;
                RenderUtils.matchModelPartRot(leftArmPart, capeLeft);
                capeLeft.updatePosition(leftArmPart.x - 5.7f, 2.32f - leftArmPart.y, leftArmPart.z);
            }

            if (capeRight != null) {
                ModelPart rightArmPart = baseModel.rightArm;
                RenderUtils.matchModelPartRot(rightArmPart, capeRight);
                capeRight.updatePosition(rightArmPart.x + 5.7f, 2.32f - rightArmPart.y, rightArmPart.z);
            }
        } else if (this.currentEntity instanceof AbstractClientPlayer player) {
            boolean isSlim = isSlimPlayer(player);

            // Apply scaling based on player model type
            if (isSlim) {
                if (cape != null) {
                    cape.setScaleX(1.4f);
                    cape.setScaleY(1.425f);
                    cape.setScaleZ(1.8f);
                }
                if (capeLeft != null) {
                    capeLeft.setScaleX(1.65f);
                    capeLeft.setScaleY(1.35f);
                    capeLeft.setScaleZ(1.6f);
                }
                if (capeRight != null) {
                    capeRight.setScaleX(1.65f);
                    capeRight.setScaleY(1.35f);
                    capeRight.setScaleZ(1.6f);
                }
            } else {
                if (cape != null) {
                    cape.setScaleX(1.5f);
                    cape.setScaleY(1.425f);
                    cape.setScaleZ(1.8f);
                }
                if (capeLeft != null) {
                    capeLeft.setScaleX(2.1f);
                    capeLeft.setScaleY(1.5f);
                    capeLeft.setScaleZ(1.6f);
                }
                if (capeRight != null) {
                    capeRight.setScaleX(2.1f);
                    capeRight.setScaleY(1.5f);
                    capeRight.setScaleZ(1.6f);
                }
            }

            // Apply positioning for players
            if (cape != null) {
                ModelPart bodyPart = baseModel.body;
                RenderUtils.matchModelPartRot(bodyPart, cape);
                cape.updatePosition(bodyPart.x, -bodyPart.y + 1.65f, bodyPart.z - 1f);
            }

            if (capeLeft != null) {
                ModelPart leftArmPart = baseModel.leftArm;
                RenderUtils.matchModelPartRot(leftArmPart, capeLeft);
                capeLeft.updatePosition(leftArmPart.x - 5f, 2f - leftArmPart.y, leftArmPart.z);
            }

            if (capeRight != null) {
                ModelPart rightArmPart = baseModel.rightArm;
                RenderUtils.matchModelPartRot(rightArmPart, capeRight);
                capeRight.updatePosition(rightArmPart.x + 5f, 2f - rightArmPart.y, rightArmPart.z);
            }
        }
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        setAllVisible(false);

        if (currentSlot == EquipmentSlot.CHEST) {
            setBoneVisible(this.model.getBone("Cape").orElse(null), true);
            setBoneVisible(this.model.getBone("CapeMiddle").orElse(null), true);
            setBoneVisible(this.model.getBone("CapeLower").orElse(null), true);
            setBoneVisible(this.model.getBone("capeLeft").orElse(null), true);
            setBoneVisible(this.model.getBone("capeRight").orElse(null), true);
        }
    }
}