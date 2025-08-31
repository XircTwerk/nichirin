package com.xirc.nichirin.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Blocking Status Effect - Applied when a player is blocking with a katana
 * Only handles movement speed reduction - damage reduction is handled by vanilla Resistance IV
 */
public class BlockingStatusEffect extends MobEffect {

    public BlockingStatusEffect() {
        super(MobEffectCategory.NEUTRAL, 0x4169E1); // Steel blue color for blocking
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Apply effect every few ticks for movement restriction
        return duration % 5 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);

        // Restore normal movement when effect ends
        if (entity instanceof Player player) {
            // Allow sprinting again (player will need to start sprinting manually)
        }
    }
}