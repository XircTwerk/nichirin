package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.world.level.block.state.properties.WoodType;

public interface NichirinWoodTypes {
    WoodType WISTERIA = new WoodType(BreathOfNichirin.MOD_ID + ":wisteria", NichirinBlockSetTypes.WISTERIA);
}