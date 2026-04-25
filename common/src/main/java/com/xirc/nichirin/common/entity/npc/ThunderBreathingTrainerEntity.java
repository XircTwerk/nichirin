package com.xirc.nichirin.common.entity.npc;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Jigoro Kuwajima — Thunder Breathing trainer. */
public class ThunderBreathingTrainerEntity extends BaseBreathingTrainerEntity {

    public ThunderBreathingTrainerEntity(EntityType<? extends ThunderBreathingTrainerEntity> type, Level level) {
        super(type, level, TrainerType.THUNDER);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseBreathingTrainerEntity.createAttributes();
    }

    @Override
    protected void equipArmor() {
        setItemSlot(EquipmentSlot.HEAD,     new ItemStack(Items.GOLDEN_HELMET));
        setItemSlot(EquipmentSlot.CHEST,    new ItemStack(Items.GOLDEN_CHESTPLATE));
        setItemSlot(EquipmentSlot.LEGS,     new ItemStack(Items.GOLDEN_LEGGINGS));
        setItemSlot(EquipmentSlot.FEET,     new ItemStack(Items.GOLDEN_BOOTS));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        setDropChance(EquipmentSlot.HEAD,     0.0f);
        setDropChance(EquipmentSlot.CHEST,    0.0f);
        setDropChance(EquipmentSlot.LEGS,     0.0f);
        setDropChance(EquipmentSlot.FEET,     0.0f);
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }
}
