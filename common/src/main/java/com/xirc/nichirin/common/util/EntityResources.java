package com.xirc.nichirin.common.util;

import com.xirc.nichirin.common.entity.MovesetCapableNPC;
import com.xirc.nichirin.common.system.NPCResourceManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Unified resource access for any LivingEntity — players or NPCs.
 * Callers never need to branch on entity type for breath/stamina checks.
 */
public final class EntityResources {

    private EntityResources() {}


    public static boolean hasBreath(LivingEntity entity, float amount) {
        if (entity instanceof Player player)       return BreathingManager.hasBreath(player, amount);
        if (entity instanceof MovesetCapableNPC n) return n.getBreathGauge() >= amount;
        return true;
    }

    public static boolean consumeBreath(LivingEntity entity, float amount) {
        if (entity instanceof Player player)       return BreathingManager.consume(player, amount);
        if (entity instanceof MovesetCapableNPC n) return NPCResourceManager.consumeBreath(n, amount);
        return true;
    }


    public static boolean hasStamina(LivingEntity entity, float amount) {
        if (entity instanceof Player player)       return StaminaManager.hasStamina(player, amount);
        if (entity instanceof MovesetCapableNPC n) return n.hasStamina(amount);
        return true;
    }

    public static boolean consumeStamina(LivingEntity entity, float amount) {
        if (entity instanceof Player player)       return StaminaManager.consume(player, amount);
        if (entity instanceof MovesetCapableNPC n) return NPCResourceManager.consumeStamina(n, amount);
        return true;
    }


    public static void sendMessage(LivingEntity entity, Component message, boolean actionBar) {
        if (entity instanceof Player player) player.displayClientMessage(message, actionBar);
    }
}
