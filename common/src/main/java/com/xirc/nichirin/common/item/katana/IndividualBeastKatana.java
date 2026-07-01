package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.world.item.Item;

/**
 * Individual beast katana (left or right) that converts back to beast katanas when the pair breaks.
 */
public class IndividualBeastKatana extends AbstractIndividualKatana {

    public IndividualBeastKatana(Properties properties, boolean isRightKatana) {
        super(properties, isRightKatana);
    }

    @Override
    public Item holderItem() {
        return NichirinItemRegistry.BEAST_KATANAS.get();
    }

    @Override
    public Item rightItem() {
        return NichirinItemRegistry.RIGHT_BEAST_KATANA.get();
    }

    @Override
    public Item leftItem() {
        return NichirinItemRegistry.LEFT_BEAST_KATANA.get();
    }
}
