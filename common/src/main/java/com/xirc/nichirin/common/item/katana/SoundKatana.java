package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.world.item.Item;

/**
 * Holder item that transforms into dual-wielded sound katanas.
 */
public class SoundKatana extends AbstractDualKatana {

    public SoundKatana(Properties properties) {
        super(properties);
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
