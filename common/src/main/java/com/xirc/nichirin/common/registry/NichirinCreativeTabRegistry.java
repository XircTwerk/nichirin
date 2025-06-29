package com.xirc.nichirin.common.registry;

import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface NichirinCreativeTabRegistry {
    Logger LOGGER = LoggerFactory.getLogger("CreativeTabRegistry");

    static void init() {
        LOGGER.info("Initializing CreativeTabRegistry...");

        BreathOfNichirin.CREATIVE_TAB_REGISTRY.register("main", () -> {
            LOGGER.info("Creating main creative tab...");
            return CreativeModeTab.builder(CreativeModeTab.Row.TOP , 1 )
                    .title(Component.translatable("itemgroup.nichirin.main"))
                    .icon(() -> {
                        try {
                            // Try to use katana as icon, fallback to diamond sword
                            return new ItemStack(NichirinItemRegistry.KATANA.get());
                        } catch (Exception e) {
                            LOGGER.warn("Failed to use katana as icon, using diamond sword fallback");
                            return new ItemStack(Items.DIAMOND_SWORD);
                        }
                    })
                    .displayItems((displayContext, entries) -> {
                        try {
                            // Add katana
                            entries.accept(NichirinItemRegistry.KATANA.get());

                            // Add ore blocks
                            entries.accept(OreRegistry.SCARLET_ORE_ITEM.get());
                            entries.accept(OreRegistry.SCARLET_CRIMSON_IRON_SAND_ITEM.get());

                            //gems/ingots
                            entries.accept(NichirinItemRegistry.SCARLET_CRIMSON_IRON_GEM.get());
                            entries.accept(NichirinItemRegistry.SCARLET_GEM.get());
                        } catch (Exception e) {
                            LOGGER.warn("Failed to add items to creative tab: " + e.getMessage());
                        }
                    })
                    .build();
        });

        LOGGER.info("CreativeTabRegistry initialization complete");
    }
}