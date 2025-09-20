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

                        // Your existing blocks - unchanged
                        entries.accept(NichirinBlockRegistry.SCARLET_ORE_ITEM.get());
                        entries.accept(NichirinBlockRegistry.SCARLET_CRIMSON_IRON_SAND_ITEM.get());
                        entries.accept(NichirinBlockRegistry.KATANA_HOLDER_BLOCK.get());

                        // NEW: Wysteria wood blocks
                        entries.accept(NichirinBlockRegistry.WYSTERIA_LOG.get());
                        entries.accept(NichirinBlockRegistry.STRIPPED_WYSTERIA_LOG.get());
                        entries.accept(NichirinBlockRegistry.WYSTERIA_WOOD.get());
                        entries.accept(NichirinBlockRegistry.STRIPPED_WYSTERIA_WOOD.get());
                        entries.accept(NichirinBlockRegistry.WYSTERIA_PLANKS.get());
                        entries.accept(NichirinBlockRegistry.WYSTERIA_LEAVES.get());
                        entries.accept(NichirinBlockRegistry.WYSTERIA_STAIRS.get());
                        entries.accept(NichirinBlockRegistry.WYSTERIA_SLAB.get());
                        entries.accept(NichirinBlockRegistry.WYSTERIA_FENCE.get());
                        entries.accept(NichirinBlockRegistry.WYSTERIA_FENCE_GATE.get());
                        entries.accept(NichirinBlockRegistry.WYSTERIA_DOOR.get());
                        entries.accept(NichirinBlockRegistry.WYSTERIA_TRAPDOOR.get());
                        entries.accept(NichirinBlockRegistry.WYSTERIA_PRESSURE_PLATE.get());
                        entries.accept(NichirinBlockRegistry.WYSTERIA_BUTTON.get());
                        entries.accept(NichirinBlockRegistry.WYSTERIA_SAPLING_ITEM.get());

                        // Your existing items - unchanged
                        entries.accept(NichirinItemRegistry.SCARLET_CRIMSON_IRON_GEM.get());
                        entries.accept(NichirinItemRegistry.SCARLET_GEM.get());
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
                        // Your existing katanas - unchanged
                        entries.accept(NichirinItemRegistry.KATANA.get());
                        entries.accept(NichirinItemRegistry.THUNDER_KATANA.get());
                        entries.accept(NichirinItemRegistry.FLAME_KATANA.get());
                        entries.accept(NichirinItemRegistry.INSECT_KATANA.get());
                        entries.accept(NichirinItemRegistry.SOUND_KATANAS.get());
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
                        // Your existing armor - unchanged
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
                        entries.accept(NichirinItemRegistry.TENGEN_LEGGINGS.get());
                        entries.accept(NichirinItemRegistry.TENGEN_BOOTS.get());

                    })
                    .build();
        });

        LOGGER.info("CreativeTabRegistry initialization complete");
    }
}