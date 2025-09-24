package com.xirc.nichirin.common.event;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.component.AbstractDemonAttack;
import com.xirc.nichirin.common.system.DemonManager;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.data.MovesetHelper;
import dev.architectury.event.events.common.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Event handler for Breath of Nichirin mod using Architectury API
 * Updated to work with mixin-based demon blood rendering
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

        PlayerDataProvider.register();
        registerDemonEvents();
    }

    private static void registerDemonEvents() {
        PlayerEvent.PLAYER_RESPAWN.register((oldPlayer, keepEverything) -> {
            if (MovesetHelper.hasDemonMoveset(oldPlayer)) {
                handlePlayerRespawn(oldPlayer);
            }
        });

        PlayerEvent.PLAYER_QUIT.register((player) -> {
            handlePlayerDisconnect(player);
        });
    }

    private static void onServerStarting(MinecraftServer server) {
        currentServer = server;
    }

    private static void onServerStopping(MinecraftServer server) {
        currentServer = null;
        DemonManager.clearAll();
    }

    /**
     * Process delayed explosions - CRITICAL for TempoBreakerAttack
     */
    private static void onServerTick(MinecraftServer server) {
        if (server != null) {
            // CRITICAL: Tick all breathing attacks
            MoveExecutor.tickAllAttacks(server);

            // CRITICAL: Tick all demon attacks
            AbstractDemonAttack.tickAllActiveAttacks(server);

            // Check for demon players with 0 blood and kill them
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (MovesetHelper.hasDemonMoveset(player)) {
                    int bloodPoints = DemonManager.getBloodPoints(player);

                    if (bloodPoints <= 0 && player.isAlive()) {
                        AbstractDemonAttack.clearSelfTickingAttacks(player);
                        player.hurt(player.damageSources().magic(), Float.MAX_VALUE);
                    }
                }
            }
        }
    }

    /**
     * Called when a player respawns - handle both death cleanup and respawn restoration
     */
    private static void handlePlayerRespawn(Player player) {
        try {
            AbstractDemonAttack.clearSelfTickingAttacks(player);
            DemonManager.cleanupPlayer(player);
            DemonFoodHandler.cleanupPlayer(player);
            DemonManager.setBloodPoints(player, 10);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when a player disconnects
     */
    private static void handlePlayerDisconnect(Player player) {
        try {
            AbstractDemonAttack.clearSelfTickingAttacks(player);
            DemonManager.cleanupPlayer(player);
            DemonFoodHandler.cleanupPlayer(player);
            com.xirc.nichirin.registry.NichirinPacketRegistry.cleanupPlayer(player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Call this when a mob is killed by a player (hook into your damage/death events)
     */
    public static void onMobKilled(ServerPlayer player, LivingEntity killedEntity) {
        DemonManager.onMobKilled((Player) player, killedEntity);
    }

    /**
     * Call this when bite attack hits (hook into your bite attack execution)
     */
    public static void onBiteHit(ServerPlayer player, LivingEntity target) {
        DemonManager.onBiteHit((Player) player, target);
    }
}