package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.mixin.WoodTypeInvoker;
import net.minecraft.world.level.block.state.properties.WoodType;

public interface NichirinWoodTypes {
    // Registered (not just constructed) so it appears in WoodType.values(); vanilla drives sign model
    // layers, renderer models and sign materials off that stream. See WoodTypeInvoker / SheetsMixin.
    WoodType WISTERIA = WoodTypeInvoker.nichirin$register(
            new WoodType(BreathOfNichirin.MOD_ID + ":wisteria", NichirinBlockSetTypes.WISTERIA));
}