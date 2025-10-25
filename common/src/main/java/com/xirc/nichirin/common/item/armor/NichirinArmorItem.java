package com.xirc.nichirin.common.item.armor;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

public class NichirinArmorItem extends ArmorItem {

    private final NichirinArmorDispatcher dispatcher;

    public NichirinArmorItem(ArmorMaterial material, Type armorType, Properties properties) {
        super(material, armorType, properties);
        this.dispatcher = new NichirinArmorDispatcher();
    }

    public NichirinArmorDispatcher getDispatcher() {
        return dispatcher;
    }
}