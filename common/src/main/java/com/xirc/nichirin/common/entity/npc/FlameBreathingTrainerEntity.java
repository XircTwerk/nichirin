package com.xirc.nichirin.common.entity.npc;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Kyojuro Rengoku — Flame Breathing trainer. */
public class FlameBreathingTrainerEntity extends BaseBreathingTrainerEntity {

    public FlameBreathingTrainerEntity(EntityType<? extends FlameBreathingTrainerEntity> type, Level level) {
        super(type, level, TrainerType.FLAME);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseBreathingTrainerEntity.createAttributes();
    }

    @Override
    protected void equipArmor() {
        setItemSlot(EquipmentSlot.CHEST,    new ItemStack(Items.IRON_CHESTPLATE));
        setItemSlot(EquipmentSlot.LEGS,     new ItemStack(Items.IRON_LEGGINGS));
        setItemSlot(EquipmentSlot.FEET,     new ItemStack(Items.IRON_BOOTS));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        setDropChance(EquipmentSlot.CHEST,    0.0f);
        setDropChance(EquipmentSlot.LEGS,     0.0f);
        setDropChance(EquipmentSlot.FEET,     0.0f);
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }
}