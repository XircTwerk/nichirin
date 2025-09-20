package com.xirc.nichirin.common.event;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.system.DemonManager;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import dev.architectury.event.events.common.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Event handler for Breath of Nichirin mod using Architectury API
 */
public class BreathOfNichirinEventHandler {

    private static MinecraftServer currentServer;

    /**
     * Initialize event listeners
     */
    public static void init() {
        LifecycleEvent.SERVER_STARTING.register(BreathOfNichirinEventHandler::onServerStarting);
        LifecycleEvent.SERVER_STOPPING.register(BreathOfNichirinEventHandler::onServerStopping);
        TickEvent.SERVER_PRE.register(BreathOfNichirinEventHandler::onServerTick);

        // Register player data events
        PlayerDataProvider.register();
    }

    private static void onServerStarting(MinecraftServer server) {
        currentServer = server;
    }

    private static void onServerStopping(MinecraftServer server) {
        currentServer = null;
        // Clean up demon data
        DemonManager.clearAll();
    }

    /**
     * Process delayed explosions - CRITICAL for TempoBreakerAttack
     */
    private static void onServerTick(MinecraftServer server) {
        if (server != null) {
            // CRITICAL: Tick all breathing attacks
            MoveExecutor.tickAllAttacks(server);
        }
    }

    /**
     * Call this when a mob is killed by a player (hook into your damage/death events)
     */
    public static void onMobKilled(ServerPlayer player, LivingEntity killedEntity) {
        // Award blood points for demon players
        DemonManager.onMobKilled((Player) player, killedEntity);
    }

    /**
     * Call this when bite attack hits (hook into your bite attack execution)
     */
    public static void onBiteHit(ServerPlayer player, LivingEntity target) {
        // Award blood points for demon players (more efficient than killing)
        DemonManager.onBiteHit((Player) player, target);
    }
}