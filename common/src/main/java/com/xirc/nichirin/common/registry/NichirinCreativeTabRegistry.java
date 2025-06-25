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
            return CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.nichirin.main"))
                    .icon(() -> {
                        try {
                            // Try to use katana as icon, fallback to diamond sword
                            return new ItemStack(ModItemRegistry.KATANA.get());
                        } catch (Exception e) {
                            LOGGER.warn("Failed to use katana as icon, using diamond sword fallback");
                            return new ItemStack(Items.DIAMOND_SWORD);
                        }
                    })
                    .displayItems((displayContext, entries) -> {
                        LOGGER.info("Adding items to creative tab...");
                        try {
                            entries.accept(ModItemRegistry.TEST_ITEM.get());
                            entries.accept(ModItemRegistry.KATANA.get());
                            LOGGER.info("Successfully added items to creative tab");
                        } catch (Exception e) {
                            LOGGER.error("Failed to add items to creative tab: ", e);
                        }
                    })
                    .build();
        });

        LOGGER.info("CreativeTabRegistry initialization complete");
    }
}