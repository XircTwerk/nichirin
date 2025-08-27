package com.xirc.nichirin.common.handler;

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

        // Only modify fall damage
        if (damageSource == player.damageSources().fall()) {
            // Check if player used double jump
            if (PlayerDoubleJump.hasDoubleJumped(player)) {
                float reducedDamage = Math.max(0, damage - 6.0f);

                // Reset the double jump state after using the benefit
                PlayerDoubleJump.resetDoubleJump(player);

                // Note: This event might not allow damage modification
                // You may need to use a different approach or keep the mixin for this specific case
                return EventResult.interruptDefault();
            }
        }

        return EventResult.pass();
    }
}