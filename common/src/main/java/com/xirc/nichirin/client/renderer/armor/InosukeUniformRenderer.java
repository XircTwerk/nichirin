package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.registry.NichirinItemRegistry;
import mod.azure.azurelib.common.model.AzBone;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

public class InosukeUniformRenderer extends NichirinArmorRenderer {

    public InosukeUniformRenderer() {
        super("inosuke_uniform", "inosuke_uniform");
    }

    private boolean isHoldingBeastKatanas() {
        if (!(currentEntity instanceof LivingEntity living)) return false;
        Item mainhand = living.getMainHandItem().getItem();
        Item offhand = living.getOffhandItem().getItem();
        Item beastKatanas = NichirinItemRegistry.BEAST_KATANAS.get();
        Item rightKatana = NichirinItemRegistry.RIGHT_BEAST_KATANA.get();
        Item leftKatana = NichirinItemRegistry.LEFT_BEAST_KATANA.get();
        // Dual-wield split katanas or the combined item
        return mainhand == beastKatanas
                || (mainhand == rightKatana && offhand == leftKatana)
                || (mainhand == leftKatana && offhand == rightKatana);
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        setAllVisible(false);
        switch (slot) {
            case HEAD -> {
                AzBone head = getBone("Head");
                setBoneVisible(head, true);
                setBoneVisible(getBone("Back"), true);
                setBoneVisible(getBone("left2"), true);
                setBoneVisible(getBone("right2"), true);
                setBoneVisible(getBone("Ears"), true);
                setBoneVisible(getBone("Tusks"), true);
                setBoneVisible(getBone("Snout"), true);
                setBoneVisible(getBone("eyeLeft"), true);
                setBoneVisible(getBone("eyeRight"), true);
                // Make the boar helmet 1.1× larger. Scale ONLY the Head bone — its children
                // (Back, left2, right2, Ears, Tusks, Snout, eyeLeft, eyeRight) are all parented to
                // Head and inherit the scale. Scaling them too compounds (1.1 × 1.1) and warps it.
                scaleBone(head, 1.1f);
            }
            case CHEST -> {
            }
            case LEGS -> {
                // Hide back katanas when holding beast katanas
                if (!isHoldingBeastKatanas()) {
                    setBoneVisible(getBone("bb_main"), true);
                }
                AzBone fur = getBone("Fur");
                if (fur != null) setAllBonesRecursive(fur, true);

                setBoneVisible(getBone("chestplate"), true);
                setBoneVisible(getBone("leftArm"), true);
                setBoneVisible(getBone("rightArm"), true);
                setBoneVisible(getBone("leftLeg"), true);
                setBoneVisible(getBone("rightLeg"), true);
            }
            case FEET -> {
                setBoneVisible(getBone("leftBoot"), true);
                setBoneVisible(getBone("rightBoot"), true);
            }
        }
    }
}