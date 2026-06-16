package com.xirc.nichirin.common.effect;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.entity.npc.DemonNPCEntity;
import com.xirc.nichirin.common.util.NichirinDamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class WisteriasGraceStatusEffect extends MobEffect {

    private static final ResourceLocation MOVEMENT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("nichirin", "wisterias_grace_movement_reduction");

    public WisteriasGraceStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0xB86CFF);
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_MODIFIER_ID,
                -0.35,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    public static boolean affects(LivingEntity entity) {
        return entity instanceof DemonNPCEntity
                || entity instanceof Player player && MovesetHelper.hasDemonMoveset(player);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!affects(entity)) return true;
        if (entity instanceof Player player && player.isCreative()) return true;

        if (NichirinModConfig.get().demon.wisteriaDamagesDemons) {
            entity.hurt(NichirinDamageSources.wisteria(entity), 1.0f + amplifier * 0.5f);
        }

        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(
                    ParticleTypes.WITCH,
                    entity.getX(),
                    entity.getY() + entity.getBbHeight() * 0.55,
                    entity.getZ(),
                    6,
                    entity.getBbWidth() * 0.45,
                    entity.getBbHeight() * 0.35,
                    entity.getBbWidth() * 0.45,
                    0.02
            );
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 30 == 0;
    }
}
