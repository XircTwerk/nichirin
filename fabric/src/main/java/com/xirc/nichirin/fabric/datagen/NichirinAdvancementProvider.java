package com.xirc.nichirin.fabric.datagen;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class NichirinAdvancementProvider extends FabricAdvancementProvider {

    public NichirinAdvancementProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateAdvancement(Consumer<Advancement> consumer) {
        // Obtain Katana advancement
        final Advancement obtainKatana = Advancement.Builder.advancement()
                .display(NichirinItemRegistry.KATANA.get(),
                        Component.literal("Slice to Meet You"),
                        Component.literal("Obtain a Katana"),
                        null,  // No background texture
                        FrameType.TASK,
                        true,  // Show toast
                        true,  // Announce to chat
                        false) // Hidden
                .addCriterion("has_katana", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.KATANA.get()))
                .build(BreathOfNichirin.id("obtain_katana"));

        consumer.accept(obtainKatana);
    }
}