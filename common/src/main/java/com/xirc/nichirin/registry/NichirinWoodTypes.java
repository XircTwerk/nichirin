package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.world.level.block.state.properties.WoodType;

public interface NichirinWoodTypes {
    WoodType WYSTERIA = new WoodType(BreathOfNichirin.MOD_ID + ":wysteria", NichirinBlockSetTypes.WYSTERIA);
}