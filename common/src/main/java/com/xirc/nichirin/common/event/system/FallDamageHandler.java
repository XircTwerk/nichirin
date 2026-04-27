package com.xirc.nichirin.common.event.system;

import com.xirc.nichirin.common.system.abilities.PlayerDoubleJump;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class FallDamageHandler {

    public static void register() {
        EntityEvent.LIVING_HURT.register(FallDamageHandler::onEntityHurt);
    }

    private static EventResult onEntityHurt(LivingEntity entity, DamageSource damageSource, float damage) {
        if (!(entity instanceof Player player)) return EventResult.pass();

        if (damageSource == player.damageSources().fall()) {
            if (PlayerDoubleJump.hasDoubleJumped(player)) {
                float reducedDamage = Math.max(0, damage - 6.0f);
                PlayerDoubleJump.resetDoubleJump(player);
                return EventResult.interruptDefault();
            }
        }

        return EventResult.pass();
    }
}