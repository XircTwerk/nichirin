package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.item.armor.NichirinArmorItem;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.item.throwable.SmokeBombItem;
import com.xirc.nichirin.common.item.tool.BentoBoxItem;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public interface NichirinItemRegistry {
    Logger LOGGER = LoggerFactory.getLogger("ModItemRegistry");

    DeferredRegister<Item> ITEM_REGISTRY = DeferredRegister.create(BreathOfNichirin.MOD_ID, Registries.ITEM);
    Map<RegistrySupplier<? extends Item>, ResourceLocation> ITEMS = new LinkedHashMap<>();

    // Simple katana
    RegistrySupplier<Item> KATANA = register("katana",
            () -> new SimpleKatana(settings().rarity(Rarity.RARE).stacksTo(1)));
    //thunder katana
    RegistrySupplier<Item> THUNDER_KATANA = register("thunder_katana",
            () -> new SimpleKatana(settings().rarity(Rarity.RARE).stacksTo(1)));

    //Ores and Ingots
    RegistrySupplier<Item> SCARLET_CRIMSON_IRON_GEM = register("scarlet_crimson_iron_gem", () -> new Item(settings()));
    RegistrySupplier<Item> SCARLET_GEM = register("scarlet_gem", () -> new Item(settings()));

    //functional items
    RegistrySupplier<Item> SMOKE_BOMB = register("smoke_bomb",
            () -> new SmokeBombItem(settings().stacksTo(16)));

    RegistrySupplier<Item> BENTO_BOX = register("bento_box",
            () -> new BentoBoxItem(settings().stacksTo(1)));


    // Shinobu Armor Set
    RegistrySupplier<Item> SHINOBU_HEADPIECE = register("shinobu_headpiece",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.HELMET, settings().stacksTo(1)));

    RegistrySupplier<Item> SHINOBU_CAPE = register("shinobu_cape",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.CHESTPLATE, settings().stacksTo(1)));

    RegistrySupplier<Item> SHINOBU_LEGGINGS = register("shinobu_leggings",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.LEGGINGS, settings().stacksTo(1)));

    RegistrySupplier<Item> SHINOBU_BOOTS = register("shinobu_boots",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.BOOTS, settings().stacksTo(1)));


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