package com.xirc.nichirin.common.entity.npc;

import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Sakonji Urokodaki — Water Breathing trainer.
 */
public class WaterBreathingTrainerEntity extends BaseBreathingTrainerEntity {

    public WaterBreathingTrainerEntity(EntityType<? extends WaterBreathingTrainerEntity> type, Level level) {
        super(type, level, TrainerType.WATER);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseBreathingTrainerEntity.createAttributes();
    }

    @Override
    protected void equipArmor() {
        setItemSlot(EquipmentSlot.HEAD,     new ItemStack(NichirinItemRegistry.UROKODAKI_HEADPIECE.get()));
        setItemSlot(EquipmentSlot.CHEST,    new ItemStack(NichirinItemRegistry.UROKODAKI_CAPE.get()));
        setItemSlot(EquipmentSlot.LEGS,     new ItemStack(NichirinItemRegistry.UROKODAKI_LEGGINGS.get()));
        setItemSlot(EquipmentSlot.FEET,     new ItemStack(NichirinItemRegistry.UROKODAKI_BOOTS.get()));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(NichirinItemRegistry.KATANA.get()));

        setDropChance(EquipmentSlot.HEAD,     0.0f);
        setDropChance(EquipmentSlot.CHEST,    0.0f);
        setDropChance(EquipmentSlot.LEGS,     0.0f);
        setDropChance(EquipmentSlot.FEET,     0.0f);
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }
}
