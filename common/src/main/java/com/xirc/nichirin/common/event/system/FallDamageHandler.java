package com.xirc.nichirin.common.event.system;

import com.xirc.nichirin.common.system.abilities.PlayerDoubleJump;
import com.xirc.nichirin.common.system.perks.PerkManager;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FallDamageHandler {

    /** Guards against recursive re-entry when we re-apply modified fall damage. */
    private static final Set<UUID> inFallCalc = new HashSet<>();

    public static void register() {
        EntityEvent.LIVING_HURT.register(FallDamageHandler::onEntityHurt);
    }

    private static EventResult onEntityHurt(LivingEntity entity, DamageSource damageSource, float damage) {
        if (!(entity instanceof Player player)) return EventResult.pass();
        if (player.level().isClientSide) return EventResult.pass();

        if (damageSource.getMsgId().equals("fall")) {
            // Double jump negates fall damage
            if (PlayerDoubleJump.hasDoubleJumped(player)) {
                PlayerDoubleJump.resetDoubleJump(player);
                return EventResult.interruptFalse();
            }

            // Featherweight perk
            if (player instanceof ServerPlayer sp && !inFallCalc.contains(sp.getUUID())) {
                float mult = PerkManager.getFallDamageMultiplier(sp);
                if (mult <= 0f) {
                    // Full immunity
                    return EventResult.interruptFalse();
                }
                if (mult < 1.0f) {
                    // Partial reduction — cancel and re-apply scaled damage
                    inFallCalc.add(sp.getUUID());
                    sp.hurt(damageSource, damage * mult);
                    inFallCalc.remove(sp.getUUID());
                    return EventResult.interruptFalse();
                }
            }
        }

        return EventResult.pass();
    }
}