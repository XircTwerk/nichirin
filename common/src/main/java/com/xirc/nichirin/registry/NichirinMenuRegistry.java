package com.xirc.nichirin.registry;

import com.xirc.nichirin.common.item.tool.BentoBoxItem;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.world.inventory.MenuType;

public interface NichirinMenuRegistry {

    /**
     * Register the Bento Box menu type for custom 3x2 food storage
     */
    MenuType<BentoBoxItem.BentoBoxMenu> BENTO_BOX_MENU_TYPE =
            MenuRegistry.ofExtended(BentoBoxItem.BentoBoxMenu::fromNetwork);

    /**
     * Initialize menu types - call this from your mod's common initialization
     */
    static void init() {
        // Set the menu type in BentoBoxItem after registration
        BentoBoxItem.BENTO_BOX_MENU_TYPE = BENTO_BOX_MENU_TYPE;

        // Log successful registration
        System.out.println("Nichirin Menu Types registered successfully!");
        System.out.println("Bento Box Menu Type: " + BENTO_BOX_MENU_TYPE);
    }
}