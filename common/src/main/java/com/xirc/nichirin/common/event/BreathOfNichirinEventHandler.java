package com.xirc.nichirin.common.event;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.sound.TempoBreakerAttack;
import dev.architectury.event.events.common.*;
import net.minecraft.server.MinecraftServer;

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
    }

    private static void onServerStarting(MinecraftServer server) {
        currentServer = server;
    }

    private static void onServerStopping(MinecraftServer server) {
        currentServer = null;
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
}