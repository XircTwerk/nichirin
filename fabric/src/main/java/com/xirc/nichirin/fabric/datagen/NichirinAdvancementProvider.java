package com.xirc.nichirin.fabric.datagen;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.LightningStrikeTrigger;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.EntityEquipmentPredicate;
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

        // Thunder Breathing advancement - triggered when obtaining the breathing style
        final Advancement thunderBreathing = Advancement.Builder.advancement()
                .parent(root)
                .display(new ItemStack(Items.LIGHTNING_ROD), // Thunder-related icon
                        Component.literal("Baptized by the Storm"),
                        Component.literal("Obtain Thunder Breathing"),
                        null,
                        FrameType.CHALLENGE,
                        true,
                        true,
                        false) // Not hidden
                .addCriterion("has_thunder_breathing",
                        com.xirc.nichirin.common.advancement.ThunderBreathingTrigger.TriggerInstance.thunderBreathingUnlock())
                .build(BreathOfNichirin.id("thunder_breathing"));

        consumer.accept(root);
        consumer.accept(obtainKatana);
        consumer.accept(thunderBreathing);
    }
}