package com.xirc.nichirin.common.entity.npc;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Shinobu Kocho — Insect Breathing trainer. */
public class InsectBreathingTrainerEntity extends BaseBreathingTrainerEntity {

    public InsectBreathingTrainerEntity(EntityType<? extends InsectBreathingTrainerEntity> type, Level level) {
        super(type, level, TrainerType.INSECT);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseBreathingTrainerEntity.createAttributes();
    }

    @Override
    protected void equipArmor() {
        setItemSlot(EquipmentSlot.CHEST,    new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        setDropChance(EquipmentSlot.CHEST,    0.0f);
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }
}