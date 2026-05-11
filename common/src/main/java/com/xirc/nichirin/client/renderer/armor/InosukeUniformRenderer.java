package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.client.renderer.armor.core.NichirinArmorBoneProvider;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public class InosukeUniformRenderer extends NichirinArmorRenderer {

    // Inosuke's geo uses lowercase "head" rather than "Head"
    private static final NichirinArmorBoneProvider INOSUKE_BONE_PROVIDER = new NichirinArmorBoneProvider() {
        @Override
        public @Nullable AzBone getHeadBone(AzBakedModel model) {
            return model.getBone("head").orElse(null);
        }
    };

    public InosukeUniformRenderer() {
        super("inosuke_uniform", "inosuke_uniform", INOSUKE_BONE_PROVIDER);
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
                setBoneVisible(getBone("head"), true);
                setBoneVisible(getBone("Back"), true);
                setBoneVisible(getBone("left2"), true);
                setBoneVisible(getBone("right2"), true);
                setBoneVisible(getBone("Ears"), true);
                setBoneVisible(getBone("Tusks"), true);
                setBoneVisible(getBone("Snout"), true);
                setBoneVisible(getBone("eyeLeft"), true);
                setBoneVisible(getBone("eyeRight"), true);
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
