package com.xirc.nichirin.datagen.providers.data;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.advancement.FirstBreathTrigger;
import com.xirc.nichirin.common.advancement.FlameBreathingTrigger;
import com.xirc.nichirin.common.advancement.InsectBreathingTrigger;
import com.xirc.nichirin.common.advancement.SoundBreathingTrigger;
import com.xirc.nichirin.common.advancement.ThunderBreathingTrigger;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

public class NichirinAdvancementProvider extends FabricAdvancementProvider {

    public NichirinAdvancementProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateAdvancement(Consumer<Advancement> consumer) {
        // Root advancement with mossy cobblestone background
        final Advancement root = Advancement.Builder.advancement()
                .display(NichirinItemRegistry.KATANA.get(),
                        Component.literal("Breath of Nichirin"),
                        Component.literal("The path of the Demon Slayer"),
                        new ResourceLocation("textures/block/mossy_cobblestone.png"),
                        FrameType.TASK,
                        false,
                        false,
                        false)
                .addCriterion("crafting_table", InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().build()))
                .build(BreathOfNichirin.id("root"));

        // Obtain Katana advancement
        final Advancement obtainKatana = Advancement.Builder.advancement()
                .parent(root)
                .display(NichirinItemRegistry.KATANA.get(),
                        Component.literal("Slice to Meet You"),
                        Component.literal("Obtain a Katana"),
                        null,
                        FrameType.TASK,
                        true,
                        true,
                        false)
                .addCriterion("has_katana", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.KATANA.get()))
                .build(BreathOfNichirin.id("obtain_katana"));

        // First Breath advancement - triggered when obtaining any breathing style
        final Advancement firstBreath = Advancement.Builder.advancement()
                .parent(root)
                .display(new ItemStack(Items.GLASS_BOTTLE), // Breath-related icon
                        Component.literal("First Breath"),
                        Component.literal("Unlock your first breathing style"),
                        null,
                        FrameType.GOAL,
                        true,
                        true,
                        false)
                .addCriterion("has_first_breathing_style",
                        FirstBreathTrigger.TriggerInstance.firstBreathUnlock())
                .build(BreathOfNichirin.id("first_breath"));

        // Thunder Breathing advancement - triggered when obtaining the breathing style
        final Advancement thunderBreathing = Advancement.Builder.advancement()
                .parent(firstBreath)
                .display(new ItemStack(Items.LIGHTNING_ROD), // Thunder particle texture
                        Component.literal("Baptized by the Storm"),
                        Component.literal("Obtain Thunder Breathing"),
                        new ResourceLocation(BreathOfNichirin.MOD_ID, "textures/particle/thunder.png"), // Thunder particle icon
                        FrameType.CHALLENGE,
                        true,
                        true,
                        false) // Not hidden
                .addCriterion("has_thunder_breathing",
                        ThunderBreathingTrigger.TriggerInstance.thunderBreathingUnlock())
                .build(BreathOfNichirin.id("thunder_breathing"));

        // Flame Breathing advancement - triggered when obtaining the breathing style
        final Advancement flameBreathing = Advancement.Builder.advancement()
                .parent(firstBreath)
                .display(new ItemStack(Items.FIRE_CHARGE), // Simple fire icon
                        Component.literal("Hearts Ablaze"),
                        Component.literal("Survive being on fire for 15 seconds to obtain Flame Breathing"),
                        null,
                        FrameType.CHALLENGE,
                        true,
                        true,
                        false) // Not hidden
                .addCriterion("has_flame_breathing",
                        FlameBreathingTrigger.TriggerInstance.flameBreathingUnlock())
                .build(BreathOfNichirin.id("flame_breathing"));

        final Advancement insectBreathing = Advancement.Builder.advancement()
                .parent(firstBreath)
                .display(new ItemStack(Items.SPIDER_EYE), // Insect/poison related icon
                        Component.literal("Toxic Elegance"),
                        Component.literal("Throw a poison potion to obtain Insect Breathing"),
                        null,
                        FrameType.CHALLENGE,
                        true,
                        true,
                        false) // Not hidden
                .addCriterion("has_insect_breathing",
                        InsectBreathingTrigger.TriggerInstance.insectBreathingUnlock())
                .build(BreathOfNichirin.id("insect_breathing"));

        final Advancement soundBreathing = Advancement.Builder.advancement()
                .parent(firstBreath)
                .display(new ItemStack(Items.MUSIC_DISC_CAT), // Music disc icon
                        Component.literal("Harmonic Resonance"),
                        Component.literal("Play a music disc in a jukebox to obtain Sound Breathing"),
                        null,
                        FrameType.CHALLENGE,
                        true,
                        true,
                        false) // Not hidden
                .addCriterion("has_sound_breathing",
                        SoundBreathingTrigger.TriggerInstance.soundBreathingUnlock())
                .build(BreathOfNichirin.id("sound_breathing"));


        consumer.accept(root);
        consumer.accept(obtainKatana);

        //breathing triggers
        consumer.accept(firstBreath);
        consumer.accept(thunderBreathing);
        consumer.accept(flameBreathing);
        consumer.accept(insectBreathing);
        consumer.accept(soundBreathing);
    }
}