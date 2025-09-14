package com.xirc.nichirin.common.effect;

import com.xirc.nichirin.registry.NichirinEffectRegistry;
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
 * ENHANCED: Removes stun effects when applied while blocking
 */
public class BlockingStatusEffect extends MobEffect {

    // UUIDs for attribute modifiers
    private static final UUID MOVEMENT_MODIFIER_UUID = UUID.fromString("8207DE5E-8CE8-4030-941E-514C1F160891");

    public BlockingStatusEffect() {
        super(MobEffectCategory.NEUTRAL, 0x4169E1); // Steel blue color for blocking

        // Add movement speed reduction (40% slower)
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_MODIFIER_UUID.toString(),
                -0.40, // 40% reduction
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Apply effect every tick to check for and remove stun effects
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // If entity has blocking effect and gets stunned, remove the stun
        if (entity.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            entity.removeEffect(NichirinEffectRegistry.STUNNED.get());

            if (entity instanceof Player player) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Blocking protects you from stun!")
                                .withStyle(style -> style.withColor(0x55FF55)),
                        true
                );
            }
        }
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