package com.xirc.nichirin.common.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Slammed Status Effect - applied by slam attacks.
 *
 * <p>Behaves like {@link StunnedStatusEffect} (kills mob AI, blocks flight) but instead of the
 * near-total movement lock it slows movement by 80%, and forces the vanilla swimming/crawl pose so
 * the victim looks like they have been driven into the ground. A wobble post-shader is layered on
 * top client-side.</p>
 *
 * <p>The movement modifier is a manually-added transient attribute, so it is torn down in
 * {@code LivingEntityMixin#onEffectRemoved} via {@link #removeMovementModifier(LivingEntity)} when
 * the effect expires, exactly like the stun effect.</p>
 */
public class SlammedStatusEffect extends MobEffect {

    private static final ResourceLocation MOVEMENT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("nichirin", "slammed_movement_reduction");

    public SlammedStatusEffect() {
        super(MobEffectCategory.NEUTRAL, 0x5C73C4); // muted impact blue
    }

    private static void updateMovementModifier(LivingEntity entity) {
        var attribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) return;

        if (!attribute.hasModifier(MOVEMENT_MODIFIER_ID)) {
            AttributeModifier modifier = new AttributeModifier(
                    MOVEMENT_MODIFIER_ID,
                    -0.80, // 80% reduction
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            attribute.addTransientModifier(modifier);
        }
    }

    public static void removeMovementModifier(LivingEntity entity) {
        var attribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(MOVEMENT_MODIFIER_ID);
        }
        // Drop the forced swim pose and restore flight for creative/spectator.
        entity.setSwimming(false);
        if (entity instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                player.getAbilities().mayfly = true;
            }
            player.onUpdateAbilities();
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true; // Apply every tick
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        updateMovementModifier(entity);

        // Force the slammed-into-the-ground look. Vanilla updateSwimming() runs later in aiStep and
        // would clear this when out of water, so LivingEntityMixin re-asserts it at that method's
        // tail; setting it here covers the rest of the tick.
        entity.setSwimming(true);

        // Player restrictions (mirror stun): no flight while slammed.
        if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
        }

        // Mob restrictions (mirror stun): drop aggro and stop pathing.
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.setAggressive(false);
            mob.getNavigation().stop();
        }

        return true;
    }
}
