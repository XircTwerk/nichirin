package com.xirc.nichirin.datagen.providers.data;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.advancement.BeastBreathingTrigger;
import com.xirc.nichirin.common.advancement.FirstBreathTrigger;
import com.xirc.nichirin.common.advancement.FlameBreathingTrigger;
import com.xirc.nichirin.common.advancement.InsectBreathingTrigger;
import com.xirc.nichirin.common.advancement.MistBreathingTrigger;
import com.xirc.nichirin.common.advancement.NichirinCriteriaTriggers;
import com.xirc.nichirin.common.advancement.SoundBreathingTrigger;
import com.xirc.nichirin.common.advancement.ThunderBreathingTrigger;
import com.xirc.nichirin.common.advancement.WaterBreathingTrigger;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class NichirinAdvancementProvider extends FabricAdvancementProvider {

    public NichirinAdvancementProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void generateAdvancement(HolderLookup.Provider registries, Consumer<AdvancementHolder> consumer) {
        // Root advancement with mossy cobblestone background
        final AdvancementHolder root = Advancement.Builder.advancement()
                .display(NichirinItemRegistry.KATANA.get(),
                        Component.literal("Breath of Nichirin"),
                        Component.literal("The path of the Demon Slayer"),
                        ResourceLocation.withDefaultNamespace("textures/block/mossy_cobblestone.png"),
                        AdvancementType.TASK,
                        false,
                        false,
                        false)
                .addCriterion("crafting_table", InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().build()))
                .build(BreathOfNichirin.id("root"));

        // Obtain Katana advancement
        final AdvancementHolder obtainKatana = Advancement.Builder.advancement()
                .parent(root)
                .display(NichirinItemRegistry.KATANA.get(),
                        Component.literal("Slice to Meet You"),
                        Component.literal("Obtain a Katana"),
                        null,
                        AdvancementType.TASK,
                        true,
                        true,
                        false)
                .addCriterion("has_katana", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.KATANA.get()))
                .build(BreathOfNichirin.id("obtain_katana"));

        // First Breath advancement - triggered when obtaining any breathing style
        final AdvancementHolder firstBreath = Advancement.Builder.advancement()
                .parent(root)
                .display(new ItemStack(Items.GLASS_BOTTLE), // Breath-related icon
                        Component.literal("First Breath"),
                        Component.literal("Unlock your first breathing style"),
                        null,
                        AdvancementType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("has_first_breathing_style",
                        NichirinCriteriaTriggers.FIRST_BREATH_TRIGGER.get().createCriterion(FirstBreathTrigger.TriggerInstance.firstBreathUnlock()))
                .build(BreathOfNichirin.id("first_breath"));

        // Thunder Breathing advancement - triggered when obtaining the breathing style
        final AdvancementHolder thunderBreathing = Advancement.Builder.advancement()
                .parent(firstBreath)
                .display(new ItemStack(Items.LIGHTNING_ROD), // Thunder particle texture
                        Component.literal("Baptized by the Storm"),
                        Component.literal("Obtain Thunder Breathing"),
                        ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/particle/thunder.png"), // Thunder particle icon
                        AdvancementType.CHALLENGE,
                        true,
                        true,
                        false) // Not hidden
                .addCriterion("has_thunder_breathing",
                        NichirinCriteriaTriggers.THUNDER_BREATHING_TRIGGER.get().createCriterion(ThunderBreathingTrigger.TriggerInstance.thunderBreathingUnlock()))
                .build(BreathOfNichirin.id("thunder_breathing"));

        // Flame Breathing advancement - triggered when obtaining the breathing style
        final AdvancementHolder flameBreathing = Advancement.Builder.advancement()
                .parent(firstBreath)
                .display(new ItemStack(Items.FIRE_CHARGE), // Simple fire icon
                        Component.literal("Hearts Ablaze"),
                        Component.literal("Survive being on fire for 15 seconds to obtain Flame Breathing"),
                        null,
                        AdvancementType.CHALLENGE,
                        true,
                        true,
                        false) // Not hidden
                .addCriterion("has_flame_breathing",
                        NichirinCriteriaTriggers.FLAME_BREATHING_TRIGGER.get().createCriterion(FlameBreathingTrigger.TriggerInstance.flameBreathingUnlock()))
                .build(BreathOfNichirin.id("flame_breathing"));

        final AdvancementHolder insectBreathing = Advancement.Builder.advancement()
                .parent(firstBreath)
                .display(new ItemStack(Items.SPIDER_EYE), // Insect/poison related icon
                        Component.literal("Toxic Elegance"),
                        Component.literal("Throw a poison potion to obtain Insect Breathing"),
                        null,
                        AdvancementType.CHALLENGE,
                        true,
                        true,
                        false) // Not hidden
                .addCriterion("has_insect_breathing",
                        NichirinCriteriaTriggers.INSECT_BREATHING_TRIGGER.get().createCriterion(InsectBreathingTrigger.TriggerInstance.insectBreathingUnlock()))
                .build(BreathOfNichirin.id("insect_breathing"));

        final AdvancementHolder soundBreathing = Advancement.Builder.advancement()
                .parent(firstBreath)
                .display(new ItemStack(Items.MUSIC_DISC_CAT), // Music disc icon
                        Component.literal("Harmonic Resonance"),
                        Component.literal("Play a music disc in a jukebox to obtain Sound Breathing"),
                        null,
                        AdvancementType.CHALLENGE,
                        true,
                        true,
                        false) // Not hidden
                .addCriterion("has_sound_breathing",
                        NichirinCriteriaTriggers.SOUND_BREATHING_TRIGGER.get().createCriterion(SoundBreathingTrigger.TriggerInstance.soundBreathingUnlock()))
                .build(BreathOfNichirin.id("sound_breathing"));

        final AdvancementHolder waterBreathing = Advancement.Builder.advancement()
                .parent(firstBreath)
                .display(new ItemStack(Items.HEART_OF_THE_SEA), // Water-themed icon
                        Component.literal("Current of Life"),
                        Component.literal("Slay a Drowned with your bare hands to obtain Water Breathing"),
                        null,
                        AdvancementType.CHALLENGE,
                        true,
                        true,
                        false)
                .addCriterion("has_water_breathing",
                        NichirinCriteriaTriggers.WATER_BREATHING_TRIGGER.get().createCriterion(WaterBreathingTrigger.TriggerInstance.waterBreathingUnlock()))
                .build(BreathOfNichirin.id("water_breathing"));

        final AdvancementHolder beastBreathing = Advancement.Builder.advancement()
                .parent(firstBreath)
                .display(new ItemStack(Items.PORKCHOP), // Boar-themed icon (placeholder until BOAR_HEAD item)
                        Component.literal("One with the Beast"),
                        Component.literal("Slay a boar and equip its head to obtain Beast Breathing"),
                        null,
                        AdvancementType.CHALLENGE,
                        true,
                        true,
                        false)
                .addCriterion("has_beast_breathing",
                        NichirinCriteriaTriggers.BEAST_BREATHING_TRIGGER.get().createCriterion(BeastBreathingTrigger.TriggerInstance.beastBreathingUnlock()))
                .build(BreathOfNichirin.id("beast_breathing"));

        final AdvancementHolder mistBreathing = Advancement.Builder.advancement()
                .parent(firstBreath)
                .display(new ItemStack(Items.WHITE_WOOL), // Cloudy/misty icon
                        Component.literal("The Mountain Mist"),
                        Component.literal("Slay a mob in a mountain biome while it rains to obtain Mist Breathing"),
                        null,
                        AdvancementType.CHALLENGE,
                        true,
                        true,
                        false)
                .addCriterion("has_mist_breathing",
                        NichirinCriteriaTriggers.MIST_BREATHING_TRIGGER.get().createCriterion(MistBreathingTrigger.TriggerInstance.mistBreathingUnlock()))
                .build(BreathOfNichirin.id("mist_breathing"));


        consumer.accept(root);
        consumer.accept(obtainKatana);

        //breathing triggers
        consumer.accept(firstBreath);
        consumer.accept(thunderBreathing);
        consumer.accept(flameBreathing);
        consumer.accept(insectBreathing);
        consumer.accept(soundBreathing);
        consumer.accept(waterBreathing);
        consumer.accept(beastBreathing);
        consumer.accept(mistBreathing);
    }
}