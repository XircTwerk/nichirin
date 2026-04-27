package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.item.armor.BoarHeadItem;
import com.xirc.nichirin.common.item.armor.NichirinArmorItem;
import com.xirc.nichirin.common.item.katana.BeastKatana;
import com.xirc.nichirin.common.item.katana.IndividualBeastKatana;
import com.xirc.nichirin.common.item.katana.IndividualSoundKatana;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.item.katana.SoundKatana;
import com.xirc.nichirin.common.item.food.MochiItem;
import com.xirc.nichirin.common.item.food.RiceItem;
import com.xirc.nichirin.common.item.throwable.FlashBombItem;
import com.xirc.nichirin.common.item.throwable.SmokeBombItem;
import com.xirc.nichirin.common.item.tool.BentoBoxItem;
import com.xirc.nichirin.common.item.LazySpawnEggItem;
import com.xirc.nichirin.common.item.tool.DrinkingGourdItem;
import com.xirc.nichirin.common.item.scroll.PerkScrollItem;
import com.xirc.nichirin.common.item.scroll.CursedScrollItem;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;


public interface NichirinItemRegistry {
    Logger LOGGER = LoggerFactory.getLogger("ModItemRegistry");

    DeferredRegister<Item> ITEM_REGISTRY = DeferredRegister.create(BreathOfNichirin.MOD_ID, Registries.ITEM);
    Map<RegistrySupplier<? extends Item>, ResourceLocation> ITEMS = new LinkedHashMap<>();

    RegistrySupplier<Item> KATANA = register("katana",
            () -> new SimpleKatana(settings().rarity(Rarity.RARE).stacksTo(1)));

    RegistrySupplier<Item> THUNDER_KATANA = register("thunder_katana",
            () -> new SimpleKatana(settings().rarity(Rarity.RARE).stacksTo(1)));

    RegistrySupplier<Item> FLAME_KATANA = register("flame_katana",
            () -> new SimpleKatana(settings().rarity(Rarity.RARE).stacksTo(1)));

    RegistrySupplier<Item> INSECT_KATANA = register("insect_katana",
            () -> new SimpleKatana(settings().rarity(Rarity.RARE).stacksTo(1)));

    RegistrySupplier<Item> SOUND_KATANAS = register("sound_katanas", () -> new SoundKatana(settings().rarity(Rarity.RARE).stacksTo(1)));

    RegistrySupplier<Item> RIGHT_SOUND_KATANA = register("right_sound_katana", () -> new IndividualSoundKatana(settings().rarity(Rarity.RARE).stacksTo(1), true));

    RegistrySupplier<Item> LEFT_SOUND_KATANA = register("left_sound_katana", () -> new IndividualSoundKatana(settings().rarity(Rarity.RARE).stacksTo(1), false));

    RegistrySupplier<Item> SABITO_KATANA = register("sabito_katana",
            () -> new SimpleKatana(settings().rarity(Rarity.RARE).stacksTo(1)));

    RegistrySupplier<Item> SCARLET_CRIMSON_IRON_GEM = register("scarlet_crimson_iron_gem", () -> new Item(settings()));

    RegistrySupplier<Item> SCARLET_GEM = register("scarlet_gem", () -> new Item(settings()));

    RegistrySupplier<Item> SMOKE_BOMB = register("smoke_bomb",
            () -> new SmokeBombItem(settings().stacksTo(16)));

    RegistrySupplier<Item> FLASH_BOMB = register("flash_bomb",
            () -> new FlashBombItem(settings().stacksTo(16)));

    RegistrySupplier<Item> BENTO_BOX = register("bento_box",
            () -> new BentoBoxItem(NichirinBlockRegistry.BENTO_BOX_BLOCK.get(), settings().stacksTo(1)));

    RegistrySupplier<Item> DRINKING_GOURD = register("drinking_gourd",
            () -> new DrinkingGourdItem(settings().stacksTo(1)));

    RegistrySupplier<Item> ONIGIRI = register("onigiri",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2)                     // restores 1 hunger point
                            .saturationMod(1.25F)
                            .alwaysEat()                      // allow eating when full
                            .effect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0), 1.0F)
                            .build())));

    RegistrySupplier<Item> SAKURAMOCHI = register("sakuramochi",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(6)                     // restores 1 hunger point
                            .saturationMod(1.5F)
                            .build())));
    RegistrySupplier<Item> RICE = register("rice",
            () -> new RiceItem(settings().stacksTo(64)));

    // Mochiiii
    RegistrySupplier<Item> MOCHI = register("mochi",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2)
                            .saturationMod(1.0F)
                            .build())));

    RegistrySupplier<Item> RED_MOCHI = register("red_mochi",
            () -> new MochiItem(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2).saturationMod(1.0F).alwaysEat()
                            .effect(new MobEffectInstance(MobEffects.HEAL, 40, 0), 1.0F)
                            .build()), "Instant Heal"));

    RegistrySupplier<Item> BLUE_MOCHI = register("blue_mochi",
            () -> new MochiItem(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2).saturationMod(1.0F).alwaysEat()
                            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3), 1.0F)
                            .effect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 2), 1.0F)
                            .build()), "Slowness + Resistance III"));

    RegistrySupplier<Item> YELLOW_MOCHI = register("yellow_mochi",
            () -> new MochiItem(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2).saturationMod(1.0F).alwaysEat()
                            .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0), 1.0F)
                            .build()), "Strength"));

    RegistrySupplier<Item> GREEN_MOCHI = register("green_mochi",
            () -> new MochiItem(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2).saturationMod(1.0F).alwaysEat()
                            .effect(new MobEffectInstance(MobEffects.POISON, 40, 0), 1.0F)
                            .build()), "Poison"));

    RegistrySupplier<Item> ORANGE_MOCHI = register("orange_mochi",
            () -> new MochiItem(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2).saturationMod(1.0F).alwaysEat()
                            .effect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0), 1.0F)
                            .build()), "Fire Resistance"));

    RegistrySupplier<Item> LIME_MOCHI = register("lime_mochi",
            () -> new MochiItem(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2).saturationMod(1.0F).alwaysEat()
                            .effect(new MobEffectInstance(MobEffects.NIGHT_VISION, 40, 0), 1.0F)
                            .build()), "Night Vision"));

    RegistrySupplier<Item> LIGHT_BLUE_MOCHI = register("light_blue_mochi",
            () -> new MochiItem(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2).saturationMod(1.0F).alwaysEat()
                            .effect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0), 1.0F)
                            .build()), "Speed"));

    RegistrySupplier<Item> CYAN_MOCHI = register("cyan_mochi",
            () -> new MochiItem(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2).saturationMod(1.0F).alwaysEat()
                            .effect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 40, 0), 1.0F)
                            .build()), "Conduit Power"));

    RegistrySupplier<Item> BROWN_MOCHI = register("brown_mochi",
            () -> new MochiItem(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2).saturationMod(1.0F).alwaysEat()
                            .effect(new MobEffectInstance(MobEffects.HARM, 40, 0), 1.0F)
                            .build()), "Instant Damage"));

    RegistrySupplier<Item> PINK_MOCHI = register("pink_mochi",
            () -> new MochiItem(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2).saturationMod(1.0F).alwaysEat()
                            .effect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0), 1.0F)
                            .build()), "Regeneration"));

    RegistrySupplier<Item> PURPLE_MOCHI = register("purple_mochi",
            () -> new MochiItem(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2).saturationMod(1.0F).alwaysEat()
                            .effect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0), 1.0F)
                            .build()), "Resistance"));

    RegistrySupplier<Item> MAGENTA_MOCHI = register("magenta_mochi",
            () -> new MochiItem(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2).saturationMod(1.0F).alwaysEat()
                            .effect(new MobEffectInstance(MobEffects.ABSORPTION, 40, 0), 1.0F)
                            .build()), "Absorption"));

    RegistrySupplier<Item> BLACK_MOCHI = register("black_mochi",
            () -> new MochiItem(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2).saturationMod(1.0F).alwaysEat()
                            .effect(new MobEffectInstance(MobEffects.WITHER, 40, 0), 1.0F)
                            .build()), "Wither"));

    RegistrySupplier<Item> WHITE_MOCHI = register("white_mochi",
            () -> new MochiItem(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2).saturationMod(1.0F).alwaysEat()
                            .effect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0), 1.0F)
                            .build()), "Slow Falling"));

    RegistrySupplier<Item> GRAY_MOCHI = register("gray_mochi",
            () -> new MochiItem(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2).saturationMod(1.0F).alwaysEat()
                            .effect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0), 1.0F)
                            .build()), "Dolphin's Grace"));

    RegistrySupplier<Item> LIGHT_GRAY_MOCHI = register("light_gray_mochi",
            () -> new MochiItem(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2).saturationMod(1.0F).alwaysEat()
                            .effect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0), 1.0F)
                            .build()), "Levitation"));


    // Armor
    RegistrySupplier<Item> ZENITSU_HEADPIECE = register("zenitsu_headpiece",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.HELMET, settings().stacksTo(1)));
    RegistrySupplier<Item> ZENITSU_CAPE = register("zenitsu_cape",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.CHESTPLATE, settings().stacksTo(1)));
    RegistrySupplier<Item> ZENITSU_LEGGINGS = register("zenitsu_leggings",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.LEGGINGS, settings().stacksTo(1)));
    RegistrySupplier<Item> ZENITSU_BOOTS = register("zenitsu_boots",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.BOOTS, settings().stacksTo(1)));

    RegistrySupplier<Item> RENGOKU_HEADPIECE = register("rengoku_headpiece",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.HELMET, settings().stacksTo(1)));
    RegistrySupplier<Item> RENGOKU_CAPE = register("rengoku_cape",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.CHESTPLATE, settings().stacksTo(1)));
    RegistrySupplier<Item> RENGOKU_LEGGINGS = register("rengoku_leggings",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.LEGGINGS, settings().stacksTo(1)));
    RegistrySupplier<Item> RENGOKU_BOOTS = register("rengoku_boots",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.BOOTS, settings().stacksTo(1)));

    RegistrySupplier<Item> TENGEN_HEADPIECE = register("tengen_headpiece",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.HELMET, settings().stacksTo(1)));
    RegistrySupplier<Item> TENGEN_ACCESSORIES = register("tengen_accessories",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.CHESTPLATE, settings().stacksTo(1)));
    RegistrySupplier<Item> TENGEN_LEGGINGS = register("tengen_leggings",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.LEGGINGS, settings().stacksTo(1)));
    RegistrySupplier<Item> TENGEN_BOOTS = register("tengen_boots",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.BOOTS, settings().stacksTo(1)));

    RegistrySupplier<Item> SHINOBU_HEADPIECE = register("shinobu_headpiece",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.HELMET, settings().stacksTo(1)));
    RegistrySupplier<Item> SHINOBU_CAPE = register("shinobu_cape",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.CHESTPLATE, settings().stacksTo(1)));
    RegistrySupplier<Item> SHINOBU_LEGGINGS = register("shinobu_leggings",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.LEGGINGS, settings().stacksTo(1)));
    RegistrySupplier<Item> SHINOBU_BOOTS = register("shinobu_boots",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.BOOTS, settings().stacksTo(1)));

    RegistrySupplier<Item> SABITO_HEADPIECE = register("sabito_headpiece",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.HELMET, settings().stacksTo(1)));
    RegistrySupplier<Item> SABITO_CAPE = register("sabito_cape",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.CHESTPLATE, settings().stacksTo(1)));
    RegistrySupplier<Item> SABITO_LEGGINGS = register("sabito_leggings",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.LEGGINGS, settings().stacksTo(1)));
    RegistrySupplier<Item> SABITO_BOOTS = register("sabito_boots",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.BOOTS, settings().stacksTo(1)));

    RegistrySupplier<Item> GIYU_HEADPIECE = register("giyu_headpiece",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.HELMET, settings().stacksTo(1)));
    RegistrySupplier<Item> GIYU_CAPE = register("giyu_cape",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.CHESTPLATE, settings().stacksTo(1)));
    RegistrySupplier<Item> GIYU_LEGGINGS = register("giyu_leggings",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.LEGGINGS, settings().stacksTo(1)));
    RegistrySupplier<Item> GIYU_BOOTS = register("giyu_boots",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.BOOTS, settings().stacksTo(1)));

    RegistrySupplier<Item> UROKODAKI_HEADPIECE = register("urokodaki_headpiece",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.HELMET, settings().stacksTo(1)));
    RegistrySupplier<Item> UROKODAKI_CAPE = register("urokodaki_cape",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.CHESTPLATE, settings().stacksTo(1)));
    RegistrySupplier<Item> UROKODAKI_LEGGINGS = register("urokodaki_leggings",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.LEGGINGS, settings().stacksTo(1)));
    RegistrySupplier<Item> UROKODAKI_BOOTS = register("urokodaki_boots",
            () -> new NichirinArmorItem(ArmorMaterials.NETHERITE, NichirinArmorItem.Type.BOOTS, settings().stacksTo(1)));

    // Beast Breathing - Inosuke's dual katanas and boar head
    RegistrySupplier<Item> BEAST_KATANAS = register("beast_katanas",
            () -> new BeastKatana(settings().rarity(Rarity.RARE).stacksTo(1)));
    RegistrySupplier<Item> RIGHT_BEAST_KATANA = register("right_beast_katana",
            () -> new IndividualBeastKatana(settings().rarity(Rarity.RARE).stacksTo(1), true));
    RegistrySupplier<Item> LEFT_BEAST_KATANA = register("left_beast_katana",
            () -> new IndividualBeastKatana(settings().rarity(Rarity.RARE).stacksTo(1), false));
    RegistrySupplier<Item> BOAR_HEAD = register("boar_head",
            () -> new BoarHeadItem(settings().stacksTo(1)));
    RegistrySupplier<Item> INOSUKE_UNIFORM = register("inosuke_uniform",
            () -> new NichirinArmorItem(ArmorMaterials.DIAMOND, NichirinArmorItem.Type.CHESTPLATE, settings().stacksTo(1)));
    RegistrySupplier<Item> INOSUKE_LEGGINGS = register("inosuke_leggings",
            () -> new NichirinArmorItem(ArmorMaterials.DIAMOND, NichirinArmorItem.Type.LEGGINGS, settings().stacksTo(1)));
    RegistrySupplier<Item> INOSUKE_BOOTS = register("inosuke_boots",
            () -> new NichirinArmorItem(ArmorMaterials.DIAMOND, NichirinArmorItem.Type.BOOTS, settings().stacksTo(1)));

    // Spawn egg for Temple Demon (dark body, blood-red spots)
    RegistrySupplier<Item> TEMPLE_DEMON_SPAWN_EGG = register("temple_demon_spawn_egg",
            () -> new LazySpawnEggItem(NichirinEntityRegistry.TEMPLE_DEMON, 0x1a1a2e, 0x8b0000, settings()));

    // Spawn egg for Water Breathing Trainer (deep teal body, white mask spots)
    RegistrySupplier<Item> WATER_BREATHING_TRAINER_SPAWN_EGG = register("water_breathing_trainer_spawn_egg",
            () -> new LazySpawnEggItem(NichirinEntityRegistry.WATER_BREATHING_TRAINER, 0x1a3a4a, 0xffffff, settings()));

    // Perk scrolls
    RegistrySupplier<Item> PERK_SCROLL = register("perk_scroll",
            () -> new PerkScrollItem(settings().rarity(Rarity.UNCOMMON)));

    RegistrySupplier<Item> CURSED_SCROLL = register("cursed_scroll",
            () -> new CursedScrollItem(settings().rarity(Rarity.EPIC)));


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
        LOGGER.info("NichirinItemRegistry.init() called - Total items to register: {}", ITEMS.size());
    }
}