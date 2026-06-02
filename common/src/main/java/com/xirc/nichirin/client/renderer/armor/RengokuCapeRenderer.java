package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.client.renderer.armor.core.NichirinCapeArmorBoneProvider;
import mod.azure.azurelib.common.model.AzBone;
import net.minecraft.world.entity.EquipmentSlot;

public class RengokuCapeRenderer extends NichirinArmorRenderer {

    public RengokuCapeRenderer() {
        super("rengoku_cape", "rengoku_cape", new NichirinCapeArmorBoneProvider("Cape"), "rengoku_cape");
    }

    @Override
    protected void applyBoneTransformations() {
        if (currentBaseModel == null) return;
        // Cape body is handled by the bone provider.
        // capeLeft/capeRight follow the arms so they swing correctly.
        // Match arm rotation/position then nudge upward slightly.
        float upOffset = 0.3f;
        matchArmBone(currentBaseModel.rightArm, getBone("capeRight"), false);
        matchArmBone(currentBaseModel.leftArm,  getBone("capeLeft"),  true);
        if (currentBaseModel.rightArm != null && getBone("capeRight") != null)
            getBone("capeRight").updatePosition(currentBaseModel.rightArm.x + 5f, 2f - currentBaseModel.rightArm.y + upOffset, currentBaseModel.rightArm.z);
        if (currentBaseModel.leftArm  != null && getBone("capeLeft")  != null)
            getBone("capeLeft") .updatePosition(currentBaseModel.leftArm.x  - 5f, 2f - currentBaseModel.leftArm.y  + upOffset, currentBaseModel.leftArm.z);

        AzBone capeLeft  = getBone("capeLeft");
        AzBone capeRight = getBone("capeRight");
        if (capeLeft  != null) { capeLeft.setScaleX(1.84f);  capeLeft.setScaleY(1.32f);  capeLeft.setScaleZ(1.32f); }
        if (capeRight != null) { capeRight.setScaleX(1.84f); capeRight.setScaleY(1.32f); capeRight.setScaleZ(1.32f); }

        // Pipeline resets Cape scale to body.xScale (1.0) every frame — override it back up.
        // Also shift the Cape body up by the same offset so the whole haori moves as one unit.
        offsetBodyBone("Cape", upOffset, 0f);
        AzBone cape = getBone("Cape");
        if (cape != null) { cape.setScaleX(1.38f); cape.setScaleY(1.38f); cape.setScaleZ(1.38f); }
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        // capeLeft/capeRight are NOT in the boneContext, so setAllVisible won't touch them.
        // The leggings renderer hides all geo bones (shared model), so we must explicitly
        // show/hide them here to avoid them staying invisible when leggings are worn.
        AzBone capeLeft  = getBone("capeLeft");
        AzBone capeRight = getBone("capeRight");
        if (slot == EquipmentSlot.CHEST) {
            setAllVisible(true);
            if (capeLeft  != null) capeLeft.setHidden(false);
            if (capeRight != null) capeRight.setHidden(false);
        } else {
            setAllVisible(false);
            if (capeLeft  != null) capeLeft.setHidden(true);
            if (capeRight != null) capeRight.setHidden(true);
        }
    }
}