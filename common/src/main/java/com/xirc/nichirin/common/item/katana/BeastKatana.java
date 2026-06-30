package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.world.item.Item;

/**
 * Holder item that transforms into dual-wielded beast katanas (Inosuke style).
 */
public class BeastKatana extends AbstractDualKatana {

    public BeastKatana(Properties properties) {
        super(properties);
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
