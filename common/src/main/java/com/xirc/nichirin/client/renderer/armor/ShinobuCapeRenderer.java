package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.client.model.NichirinArmorModel;
import com.xirc.nichirin.common.item.armor.NichirinArmorItem;
import mod.azure.azurelib.cache.object.GeoBone;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

public class ShinobuCapeRenderer extends NichirinArmorRenderer<NichirinArmorItem> {
    public ShinobuCapeRenderer() {
        super(new NichirinArmorModel<>("shinobu_cape"));
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

    @Override
    protected void applyBaseTransformations(HumanoidModel<?> baseModel) {
        super.applyBaseTransformations(baseModel);

        // Stretch cape arms out to the sides
        GeoBone capeLeft = this.model.getBone("capeLeft").orElse(null);
        GeoBone capeRight = this.model.getBone("capeRight").orElse(null);

        if (capeLeft != null) capeLeft.setScaleX(1.2f);  // Stretch 20% wider
        if (capeRight != null) capeRight.setScaleX(1.2f); // Stretch 20% wider
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        setAllVisible(false);

        if (currentSlot == EquipmentSlot.CHEST) {
            setBoneVisible(this.model.getBone("Cape").orElse(null), true);
            setBoneVisible(this.model.getBone("capeLeft").orElse(null), true);
            setBoneVisible(this.model.getBone("capeRight").orElse(null), true);
            setBoneVisible(this.model.getBone("Lower parts").orElse(null), true);
        }
    }
}