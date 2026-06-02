package com.xirc.nichirin.common.effect;

import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

/**
 * Blocking Status Effect - Applied when a player is blocking with a katana
 * Only handles movement speed reduction - damage reduction is handled by vanilla Resistance IV
 * Removes stun effects when applied while blocking
 */
public class BlockingStatusEffect extends MobEffect {

    private static final ResourceLocation MOVEMENT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("nichirin", "blocking_movement_reduction");

    public BlockingStatusEffect() {
        super(MobEffectCategory.NEUTRAL, 0x4169E1); // Steel blue color for blocking

        // Add movement speed reduction (40% slower)
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_MODIFIER_ID,
                -0.40, // 40% reduction
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Apply effect every tick to check for and remove stun effects
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // If entity has blocking effect and gets stunned, remove the stun
        if (entity.hasEffect(NichirinEffectRegistry.stunned())) {
            entity.removeEffect(NichirinEffectRegistry.stunned());

            if (entity instanceof Player player) {
                player.displayClientMessage(
                        Component.literal("Blocking protects you from stun!")
                                .withStyle(style -> style.withColor(0x55FF55)),
                        true
                );
            }
        }
        return true;
    }
}