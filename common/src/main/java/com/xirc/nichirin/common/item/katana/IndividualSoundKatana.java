package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.world.item.Item;

/**
 * Individual sound katana (left or right) that converts back to sound katanas when the pair breaks.
 */
public class IndividualSoundKatana extends AbstractIndividualKatana {

    public IndividualSoundKatana(Properties properties, boolean isRightKatana) {
        super(properties, isRightKatana);
    }

    @Override
    public Item holderItem() {
        return NichirinItemRegistry.SOUND_KATANAS.get();
    }

    @Override
    public Item rightItem() {
        return NichirinItemRegistry.RIGHT_SOUND_KATANA.get();
    }

    @Override
    public Item leftItem() {
        return NichirinItemRegistry.LEFT_SOUND_KATANA.get();
    }
}
