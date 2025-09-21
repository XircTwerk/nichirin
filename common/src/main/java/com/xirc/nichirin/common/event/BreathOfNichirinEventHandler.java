package com.xirc.nichirin.common.event;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.component.AbstractDemonAttack;
import com.xirc.nichirin.common.system.DemonManager;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.client.gui.DemonBloodGui;
import dev.architectury.event.EventResult;
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

        // Register demon-specific events
        registerDemonEvents();
    }

    private static void registerDemonEvents() {
        // Handle player respawn - restore full blood and cleanup
        PlayerEvent.PLAYER_RESPAWN.register((oldPlayer, keepEverything) -> {
            System.out.println("RESPAWN DEBUG: Player respawn event triggered for " + oldPlayer.getName().getString());
            if (MovesetHelper.hasDemonMoveset(oldPlayer)) {
                System.out.println("RESPAWN DEBUG: Player " + oldPlayer.getName().getString() + " is a demon, handling respawn");
                handlePlayerRespawn(oldPlayer);
            } else {
                System.out.println("RESPAWN DEBUG: Player " + oldPlayer.getName().getString() + " is not a demon, skipping");
            }
        });

        // Handle player disconnect - cleanup
        PlayerEvent.PLAYER_QUIT.register((player) -> {
            System.out.println("DISCONNECT DEBUG: Player " + player.getName().getString() + " disconnecting");
            handlePlayerDisconnect(player);
        });
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

            // CRITICAL: Tick all demon attacks
            AbstractDemonAttack.tickAllActiveAttacks(server);

            // Check for demon players with 0 blood and kill them
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (MovesetHelper.hasDemonMoveset(player)) {
                    int bloodPoints = DemonManager.getBloodPoints(player);

                    if (bloodPoints <= 0 && player.isAlive()) {
                        System.out.println("DEATH DEBUG: Player " + player.getName().getString() + " has 0 blood, triggering death");

                        // Force clear all attacks immediately
                        System.out.println("DEATH DEBUG: Clearing demon attacks for " + player.getName().getString());
                        AbstractDemonAttack.clearSelfTickingAttacks(player);

                        // Kill the player
                        System.out.println("DEATH DEBUG: Applying fatal damage to " + player.getName().getString());
                        player.hurt(player.damageSources().magic(), Float.MAX_VALUE);
                        System.out.println("DEATH DEBUG: Fatal damage applied to " + player.getName().getString());
                    }
                }
            }
        }
    }

    /**
     * Called when a player respawns - handle both death cleanup and respawn restoration
     */
    private static void handlePlayerRespawn(Player player) {
        System.out.println("RESPAWN DEBUG: Starting handlePlayerRespawn for " + player.getName().getString());

        try {
            // First, clean up any leftover demon attacks from death
            System.out.println("RESPAWN DEBUG: Clearing demon attacks for " + player.getName().getString());
            AbstractDemonAttack.clearSelfTickingAttacks(player);
            System.out.println("RESPAWN DEBUG: Demon attacks cleared for " + player.getName().getString());

            // Clean up demon data
            System.out.println("RESPAWN DEBUG: Cleaning up demon manager data for " + player.getName().getString());
            DemonManager.cleanupPlayer(player);
            System.out.println("RESPAWN DEBUG: Demon manager data cleaned for " + player.getName().getString());

            System.out.println("RESPAWN DEBUG: Cleaning up demon food handler data for " + player.getName().getString());
            DemonFoodHandler.cleanupPlayer(player);
            System.out.println("RESPAWN DEBUG: Demon food handler data cleaned for " + player.getName().getString());

            // Then restore full blood on respawn
            System.out.println("RESPAWN DEBUG: Setting blood points to 10 for " + player.getName().getString());
            DemonManager.setBloodPoints(player, 10);
            System.out.println("RESPAWN DEBUG: Blood points set to " + DemonManager.getBloodPoints(player) + " for " + player.getName().getString());

            // Notify client-side GUI to reset display
            if (player.level().isClientSide) {
                System.out.println("RESPAWN DEBUG: Notifying client GUI for " + player.getName().getString());
                DemonBloodGui.onPlayerRespawn();
                System.out.println("RESPAWN DEBUG: Client GUI notified for " + player.getName().getString());
            }

            System.out.println("RESPAWN DEBUG: handlePlayerRespawn completed successfully for " + player.getName().getString());

        } catch (Exception e) {
            System.out.println("RESPAWN DEBUG: ERROR in handlePlayerRespawn for " + player.getName().getString() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Called when a player disconnects
     */
    private static void handlePlayerDisconnect(Player player) {
        System.out.println("DISCONNECT DEBUG: Starting handlePlayerDisconnect for " + player.getName().getString());

        try {
            // Clear all active attacks to prevent memory leaks
            System.out.println("DISCONNECT DEBUG: Clearing demon attacks for " + player.getName().getString());
            AbstractDemonAttack.clearSelfTickingAttacks(player);

            // Clean up demon data
            System.out.println("DISCONNECT DEBUG: Cleaning up demon data for " + player.getName().getString());
            DemonManager.cleanupPlayer(player);
            DemonFoodHandler.cleanupPlayer(player);

            System.out.println("DISCONNECT DEBUG: handlePlayerDisconnect completed for " + player.getName().getString());

        } catch (Exception e) {
            System.out.println("DISCONNECT DEBUG: ERROR in handlePlayerDisconnect for " + player.getName().getString() + ": " + e.getMessage());
            e.printStackTrace();
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