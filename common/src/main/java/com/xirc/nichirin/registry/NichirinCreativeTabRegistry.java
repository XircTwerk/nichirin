package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
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
                        return new ItemStack(NichirinItemRegistry.SMOKE_BOMB.get());
                    })
                    .displayItems((displayContext, entries) -> {

                        // Add ore blocks
                        entries.accept(NichirinBlockRegistry.SCARLET_ORE_ITEM.get());
                        entries.accept(NichirinBlockRegistry.SCARLET_CRIMSON_IRON_SAND_ITEM.get());

                        //gems/ingots
                        entries.accept(NichirinItemRegistry.SCARLET_CRIMSON_IRON_GEM.get());
                        entries.accept(NichirinItemRegistry.SCARLET_GEM.get());

                        //functional items
                        entries.accept(NichirinItemRegistry.SMOKE_BOMB.get());
                        entries.accept(NichirinItemRegistry.FLASH_BOMB.get());
                        entries.accept(NichirinItemRegistry.BENTO_BOX.get());

                    })
                    .build();
        });
        BreathOfNichirin.CREATIVE_TAB_REGISTRY.register("katanas", () -> {
            LOGGER.info("Creating katana creative tab...");
            return CreativeModeTab.builder(CreativeModeTab.Row.TOP , 1 )
                    .title(Component.translatable("itemgroup.nichirin.katanas"))
                    .icon(() -> {
                        return new ItemStack(NichirinItemRegistry.KATANA.get());
                    })
                    .displayItems((displayContext, entries) -> {
                        // Add katana
                        entries.accept(NichirinItemRegistry.KATANA.get());
                        entries.accept(NichirinItemRegistry.THUNDER_KATANA.get());
                        entries.accept(NichirinItemRegistry.FLAME_KATANA.get());
                        entries.accept(NichirinItemRegistry.INSECT_KATANA.get());
                    })
                    .build();
        });

        BreathOfNichirin.CREATIVE_TAB_REGISTRY.register("equipment", () -> {
            LOGGER.info("Creating equipment creative tab...");
            return CreativeModeTab.builder(CreativeModeTab.Row.TOP , 1 )
                    .title(Component.translatable("itemgroup.nichirin.equipment"))
                    .icon(() -> {
                        return new ItemStack(NichirinItemRegistry.SHINOBU_CAPE.get());
                    })
                    .displayItems((displayContext, entries) -> {
                        // Add armor
                        entries.accept(NichirinItemRegistry.SHINOBU_HEADPIECE.get());
                        entries.accept(NichirinItemRegistry.SHINOBU_CAPE.get());
                        entries.accept(NichirinItemRegistry.SHINOBU_LEGGINGS.get());
                        entries.accept(NichirinItemRegistry.SHINOBU_BOOTS.get());

                        entries.accept(NichirinItemRegistry.ZENITSU_HEADPIECE.get());
                        entries.accept(NichirinItemRegistry.ZENITSU_CAPE.get());
                        entries.accept(NichirinItemRegistry.ZENITSU_LEGGINGS.get());
                        entries.accept(NichirinItemRegistry.ZENITSU_BOOTS.get());

                        entries.accept(NichirinItemRegistry.RENGOKU_HEADPIECE.get());
                        entries.accept(NichirinItemRegistry.RENGOKU_CAPE.get());
                        entries.accept(NichirinItemRegistry.RENGOKU_LEGGINGS.get());
                        entries.accept(NichirinItemRegistry.RENGOKU_BOOTS.get());

                        entries.accept(NichirinItemRegistry.TENGEN_HEADPIECE.get());
                        entries.accept(NichirinItemRegistry.TENGEN_ACCESSORIES.get());
                        entries.accept(NichirinItemRegistry.TENGEN_BOOTS.get());
                        entries.accept(NichirinItemRegistry.TENGEN_LEGGINGS.get());
                        


                    })
                    .build();
        });

        LOGGER.info("CreativeTabRegistry initialization complete");
    }
}