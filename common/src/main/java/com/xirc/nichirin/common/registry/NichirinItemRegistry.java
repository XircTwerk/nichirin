package com.xirc.nichirin.common.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.item.sword.SimpleKatana;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public interface NichirinItemRegistry {
    Logger LOGGER = LoggerFactory.getLogger("ModItemRegistry");

    DeferredRegister<Item> ITEM_REGISTRY = DeferredRegister.create(BreathOfNichirin.MOD_ID, Registries.ITEM);
    Map<RegistrySupplier<? extends Item>, ResourceLocation> ITEMS = new LinkedHashMap<>();

    // Test with a simple item first
    RegistrySupplier<Item> TEST_ITEM = register("test_item",
            () -> new Item(new Item.Properties()));

    // Simple katana for testing
    RegistrySupplier<Item> KATANA = register("katana",
            () -> new SimpleKatana(Tiers.DIAMOND, 6, -2.4f, settings().rarity(Rarity.EPIC).stacksTo(1)));

    static <T extends Item> RegistrySupplier<T> register(String id, Supplier<? extends T> supplier) {
        LOGGER.info("Registering item: {}", id);
        RegistrySupplier<T> item = ITEM_REGISTRY.register(id, supplier);
        ITEMS.put(item, BreathOfNichirin.id(id));
        LOGGER.info("Successfully registered item: {} with resource location: {}", id, BreathOfNichirin.id(id));
        return item;
    }

    static Item.Properties settings() {
        return new Item.Properties();
    }

    static void init() {
        LOGGER.info("ModItemRegistry.init() called - Total items to register: {}", ITEMS.size());
    }
}