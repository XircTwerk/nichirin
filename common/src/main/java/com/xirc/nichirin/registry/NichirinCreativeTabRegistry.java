package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import dev.architectury.registry.CreativeTabRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface NichirinCreativeTabRegistry {
    Logger LOGGER = LoggerFactory.getLogger("CreativeTabRegistry");

    static void init() {
        BreathOfNichirin.CREATIVE_TAB_REGISTRY.register("main", () -> {
            return CreativeModeTab.builder(CreativeModeTab.Row.TOP , 1 )
                    .title(Component.translatable("itemgroup.nichirin.main"))
                    .icon(() -> {
                        return new ItemStack(NichirinItemRegistry.SMOKE_BOMB.get());
                    })
                    .displayItems((displayContext, entries) -> {

                        entries.accept(NichirinBlockRegistry.SCARLET_ORE_ITEM.get());
                        entries.accept(NichirinBlockRegistry.SCARLET_CRIMSON_IRON_SAND_ITEM.get());
                        entries.accept(NichirinBlockRegistry.KATANA_HOLDER_BLOCK.get());

                        entries.accept(NichirinBlockRegistry.WISTERIA_LOG.get());
                        entries.accept(NichirinBlockRegistry.STRIPPED_WISTERIA_LOG.get());
                        entries.accept(NichirinBlockRegistry.WISTERIA_WOOD.get());
                        entries.accept(NichirinBlockRegistry.STRIPPED_WISTERIA_WOOD.get());
                        entries.accept(NichirinBlockRegistry.WISTERIA_PLANKS.get());
                        entries.accept(NichirinBlockRegistry.WISTERIA_LEAVES.get());
                        entries.accept(NichirinBlockRegistry.WISTERIA_GLOW_LICHEN.get());
                        entries.accept(NichirinBlockRegistry.WISTERIA_GLOW_BERRIES.get());
                        entries.accept(NichirinBlockRegistry.WISTERIA_LANTERN_ITEM.get());
                        entries.accept(NichirinBlockRegistry.WISTERIA_STAIRS.get());
                        entries.accept(NichirinBlockRegistry.WISTERIA_SLAB.get());
                        entries.accept(NichirinBlockRegistry.WISTERIA_FENCE.get());
                        entries.accept(NichirinBlockRegistry.WISTERIA_FENCE_GATE.get());
                        entries.accept(NichirinBlockRegistry.WISTERIA_DOOR.get());
                        entries.accept(NichirinBlockRegistry.WISTERIA_TRAPDOOR.get());
                        entries.accept(NichirinBlockRegistry.WISTERIA_PRESSURE_PLATE.get());
                        entries.accept(NichirinBlockRegistry.WISTERIA_BUTTON.get());
                        entries.accept(NichirinBlockRegistry.WISTERIA_SAPLING_ITEM.get());

                        entries.accept(NichirinBlockRegistry.TATAMI_BLOCK_ITEM.get());
                        entries.accept(NichirinBlockRegistry.INFINITY_GLASS1_ITEM.get());
                        entries.accept(NichirinBlockRegistry.INFINITY_GLASS2_ITEM.get());

                        entries.accept(NichirinItemRegistry.SCARLET_CRIMSON_IRON_GEM.get());
                        entries.accept(NichirinItemRegistry.SCARLET_GEM.get());
                        entries.accept(NichirinItemRegistry.SMOKE_BOMB.get());
                        entries.accept(NichirinItemRegistry.FLASH_BOMB.get());
                        entries.accept(NichirinItemRegistry.DEMON_BLOOD_VIAL.get());
                        entries.accept(NichirinItemRegistry.WISTERIA_FLOWER.get());
                        entries.accept(NichirinItemRegistry.WISTERIA_EXTRACT.get());
                        entries.accept(NichirinItemRegistry.WISTERIA_ARROW.get());
                    })
                    .build();
        });

        BreathOfNichirin.CREATIVE_TAB_REGISTRY.register("katanas", () -> {
            return CreativeModeTab.builder(CreativeModeTab.Row.TOP , 1 )
                    .title(Component.translatable("itemgroup.nichirin.katanas"))
                    .icon(() -> {
                        return new ItemStack(NichirinItemRegistry.KATANA.get());
                    })
                    .displayItems((displayContext, entries) -> {
                        entries.accept(NichirinItemRegistry.KATANA.get());
                        entries.accept(NichirinItemRegistry.THUNDER_KATANA.get());
                        entries.accept(NichirinItemRegistry.FLAME_KATANA.get());
                        entries.accept(NichirinItemRegistry.INSECT_KATANA.get());
                        entries.accept(NichirinItemRegistry.SOUND_KATANAS.get());
                        entries.accept(NichirinItemRegistry.BEAST_KATANAS.get());
                        entries.accept(NichirinItemRegistry.SABITO_KATANA.get());
                        entries.accept(NichirinItemRegistry.UROKODAKI_KATANA.get());
                        entries.accept(NichirinItemRegistry.MIST_KATANA.get());
                        entries.accept(NichirinItemRegistry.GIYU_KATANA.get());
                    })
                    .build();
        });

        BreathOfNichirin.CREATIVE_TAB_REGISTRY.register("equipment", () -> {
            return CreativeModeTab.builder(CreativeModeTab.Row.TOP , 1 )
                    .title(Component.translatable("itemgroup.nichirin.equipment"))
                    .icon(() -> {
                        return new ItemStack(NichirinItemRegistry.SHINOBU_CAPE.get());
                    })
                    .displayItems((displayContext, entries) -> {
                        entries.accept(NichirinItemRegistry.SHINOBU_HEADPIECE.get());
                        entries.accept(NichirinItemRegistry.SHINOBU_CAPE.get());
                        entries.accept(NichirinItemRegistry.SHINOBU_LEGGINGS.get());
                        entries.accept(NichirinItemRegistry.SHINOBU_BOOTS.get());

                        entries.accept(NichirinItemRegistry.ZENITSU_HEADPIECE.get());
                        entries.accept(NichirinItemRegistry.ZENITSU_CAPE.get());
                        entries.accept(NichirinItemRegistry.ZENITSU_LEGGINGS.get());
                        entries.accept(NichirinItemRegistry.ZENITSU_BOOTS.get());

                        entries.accept(NichirinItemRegistry.JIGORO_HEADPIECE.get());
                        entries.accept(NichirinItemRegistry.JIGORO_CAPE.get());
                        entries.accept(NichirinItemRegistry.JIGORO_LEGGINGS.get());
                        entries.accept(NichirinItemRegistry.JIGORO_BOOTS.get());

                        entries.accept(NichirinItemRegistry.RENGOKU_HEADPIECE.get());
                        entries.accept(NichirinItemRegistry.RENGOKU_CAPE.get());
                        entries.accept(NichirinItemRegistry.RENGOKU_LEGGINGS.get());
                        entries.accept(NichirinItemRegistry.RENGOKU_BOOTS.get());

                        entries.accept(NichirinItemRegistry.TENGEN_HEADPIECE.get());
                        entries.accept(NichirinItemRegistry.TENGEN_ACCESSORIES.get());
                        entries.accept(NichirinItemRegistry.TENGEN_LEGGINGS.get());
                        entries.accept(NichirinItemRegistry.TENGEN_BOOTS.get());

                        entries.accept(NichirinItemRegistry.SABITO_HEADPIECE.get());
                        entries.accept(NichirinItemRegistry.SABITO_CAPE.get());
                        entries.accept(NichirinItemRegistry.SABITO_LEGGINGS.get());
                        entries.accept(NichirinItemRegistry.SABITO_BOOTS.get());

                        entries.accept(NichirinItemRegistry.GIYU_HEADPIECE.get());
                        entries.accept(NichirinItemRegistry.GIYU_CAPE.get());
                        entries.accept(NichirinItemRegistry.GIYU_LEGGINGS.get());
                        entries.accept(NichirinItemRegistry.GIYU_BOOTS.get());

                        entries.accept(NichirinItemRegistry.MUICHIRO_HEADPIECE.get());
                        entries.accept(NichirinItemRegistry.MUICHIRO_CHESTPLATE.get());
                        entries.accept(NichirinItemRegistry.MUICHIRO_BOOTS.get());

                        entries.accept(NichirinItemRegistry.UROKODAKI_HEADPIECE.get());
                        entries.accept(NichirinItemRegistry.UROKODAKI_CAPE.get());
                        entries.accept(NichirinItemRegistry.UROKODAKI_LEGGINGS.get());
                        entries.accept(NichirinItemRegistry.UROKODAKI_BOOTS.get());

                        entries.accept(NichirinItemRegistry.BOAR_HEAD.get());
                        entries.accept(NichirinItemRegistry.INOSUKE_LEGGINGS.get());
                        entries.accept(NichirinItemRegistry.INOSUKE_BOOTS.get());

                    })
                    .build();
        });

        BreathOfNichirin.CREATIVE_TAB_REGISTRY.register("food", () -> {
            return CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
                    .title(Component.translatable("itemgroup.nichirin.food"))
                    .icon(() -> new ItemStack(NichirinItemRegistry.ONIGIRI.get()))
                    .displayItems((displayContext, entries) -> {
                        entries.accept(NichirinItemRegistry.RICE.get());
                        entries.accept(NichirinItemRegistry.BENTO_BOX.get());
                        entries.accept(NichirinItemRegistry.ONIGIRI.get());
                        entries.accept(NichirinItemRegistry.SAKURAMOCHI.get());
                        entries.accept(NichirinItemRegistry.WISTERIA_TEA.get());
                        entries.accept(NichirinItemRegistry.DRINKING_GOURD.get());
                        entries.accept(NichirinItemRegistry.MOCHI.get());
                        entries.accept(NichirinItemRegistry.RED_MOCHI.get());
                        entries.accept(NichirinItemRegistry.BLUE_MOCHI.get());
                        entries.accept(NichirinItemRegistry.YELLOW_MOCHI.get());
                        entries.accept(NichirinItemRegistry.GREEN_MOCHI.get());
                        entries.accept(NichirinItemRegistry.ORANGE_MOCHI.get());
                        entries.accept(NichirinItemRegistry.LIME_MOCHI.get());
                        entries.accept(NichirinItemRegistry.LIGHT_BLUE_MOCHI.get());
                        entries.accept(NichirinItemRegistry.CYAN_MOCHI.get());
                        entries.accept(NichirinItemRegistry.BROWN_MOCHI.get());
                        entries.accept(NichirinItemRegistry.PINK_MOCHI.get());
                        entries.accept(NichirinItemRegistry.PURPLE_MOCHI.get());
                        entries.accept(NichirinItemRegistry.MAGENTA_MOCHI.get());
                        entries.accept(NichirinItemRegistry.BLACK_MOCHI.get());
                        entries.accept(NichirinItemRegistry.WHITE_MOCHI.get());
                        entries.accept(NichirinItemRegistry.GRAY_MOCHI.get());
                        entries.accept(NichirinItemRegistry.LIGHT_GRAY_MOCHI.get());
                    })
                    .build();
        });

        CreativeTabRegistry.append(CreativeModeTabs.SPAWN_EGGS,
                NichirinItemRegistry.TEMPLE_DEMON_SPAWN_EGG,
                NichirinItemRegistry.BOAR_SPAWN_EGG,
                NichirinItemRegistry.WATER_BREATHING_TRAINER_SPAWN_EGG,
                NichirinItemRegistry.THUNDER_BREATHING_TRAINER_SPAWN_EGG
        );

    }
}
