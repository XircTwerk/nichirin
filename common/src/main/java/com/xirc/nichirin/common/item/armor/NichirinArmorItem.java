package com.xirc.nichirin.common.item.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

/**
 * Base armor item for Nichirin armors.
 * No longer needs to implement GeoAnimatable in AzureLib 3.x.
 * Animation and rendering are handled through separate registries.
 */
public class NichirinArmorItem extends ArmorItem {

    // Netherite-tier durability multiplier (helmet 407 / chest 592 / legs 555 / boots 481).
    private static final int DURABILITY_MULTIPLIER = 37;

    public NichirinArmorItem(Holder<ArmorMaterial> material, Type armorType, Properties properties) {
        // Registrations never set a durability, which made every piece unbreakable. Apply it here
        // so all nichirin armor wears down; nichirin attacks wear it slower (NichirinDamageHandler
        // scales hurtArmor damage down for mod attack sources).
        super(material, armorType, properties.durability(armorType.getDurability(DURABILITY_MULTIPLIER)));
    }
}