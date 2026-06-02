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
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Server-side effects can go here if needed
        return super.applyEffectTick(entity, amplifier);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true; // Apply every tick
    }

    // Helper methods for applying different tiers

    // Tier 1: Movement disorientation only (amplifier 0)
    public static void applyTier1Disorientation(Player player, int duration) {
        player.addEffect(new MobEffectInstance(
                NichirinEffectRegistry.disoriented(),
                duration,
                0, // amplifier 0 = tier 1
                false,
                true,
                true
        ));
    }

    // Tier 2: Mouse disorientation only (amplifier 1)
    public static void applyTier2Disorientation(Player player, int duration) {
        player.addEffect(new MobEffectInstance(
                NichirinEffectRegistry.disoriented(),
                duration,
                1, // amplifier 1 = tier 2
                false,
                true,
                true
        ));
    }

    // Tier 3: Both movement and mouse disorientation (amplifier 2)
    public static void applyTier3Disorientation(Player player, int duration) {
        player.addEffect(new MobEffectInstance(
                NichirinEffectRegistry.disoriented(),
                duration,
                2, // amplifier 2 = tier 3
                false,
                true,
                true
        ));
    }

    // General method to create the disoriented effect instance with custom amplifier
    public static void applyDisorientedEffect(Player player, int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(
                NichirinEffectRegistry.disoriented(),
                duration,
                amplifier,
                false,
                true,
                true
        ));
    }
}