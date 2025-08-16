package com.xirc.nichirin.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;
import com.xirc.nichirin.registry.NichirinEffectRegistry;

public class DisorientedStatusEffect extends MobEffect {

    public DisorientedStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B4513); // Brown color for disorientation
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Server-side effects can go here if needed
        super.applyEffectTick(entity, amplifier);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true; // Apply every tick
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(entity, attributeMap, amplifier);
    }

    // Method to create the disoriented effect instance
    public static void applyDisorientedEffect(Player player, int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(
                NichirinEffectRegistry.DISORIENTED.get(),
                duration,
                amplifier,
                false,
                true,
                true
        ));
    }
}