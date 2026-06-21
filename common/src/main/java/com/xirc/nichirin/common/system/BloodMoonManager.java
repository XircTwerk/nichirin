package com.xirc.nichirin.common.system;

import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.entity.npc.TempleDemonEntity;
import com.xirc.nichirin.common.network.s2c.BloodMoonSyncPacket;
import com.xirc.nichirin.common.util.NetworkBufferUtils;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

public class BloodMoonManager {

    private static final ResourceLocation ATTACK_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("nichirin", "blood_moon_attack");
    private static final ResourceLocation SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("nichirin", "blood_moon_speed");

    // Blood moon state
    private static boolean active = false;

    // Tracks whether we've already rolled for this night (reset each day)
    private static boolean rolledThisNight = false;
    private static boolean wasNight = false;

    // Cooldown: wait at least 10 full day cycles (200000 ticks) after ending before another can trigger
    private static long lastBloodMoonEndTick = -200001L;

    public static boolean isActive() {
        return active;
    }

    /**
     * Called every server tick. Checks for blood moon trigger and end conditions.
     */
    public static void onServerTick(MinecraftServer server) {
        // Find the overworld level
        ServerLevel overworld = server.overworld();
        long dayTime = overworld.getDayTime() % 24000L;
        long gameTime = overworld.getGameTime();

        boolean isNight = dayTime >= 13000L && dayTime <= 23000L;

        if (active) {
            // End blood moon at dawn
            if (!isNight) {
                endBloodMoon(server);
            }
        } else {
            NichirinModConfig.BloodMoonConfig cfg = NichirinModConfig.get().bloodMoon;
            if (!cfg.enabled) {
                wasNight = isNight;
                return;
            }
            if (isNight) {
                // Roll exactly once when night first starts
                if (!wasNight && !rolledThisNight) {
                    rolledThisNight = true;
                    if (gameTime - lastBloodMoonEndTick >= 200000L) {
                        int chance = Math.max(1, cfg.chancePerNight);
                        if (overworld.getRandom().nextInt(chance) == 0) {
                            startBloodMoon(server);
                        }
                    }
                }
            } else {
                // Reset for the next night
                rolledThisNight = false;
            }
        }

        wasNight = isNight;
    }

    /**
     * Starts the blood moon: sets flag, broadcasts chat, boosts demons, syncs to all clients.
     */
    public static void startBloodMoon(MinecraftServer server) {
        active = true;

        // Message is handled client-side via BloodMoonSyncPacket to avoid sending it twice

        // Apply attribute boosts to all TempleDemonEntity in all server levels
        for (ServerLevel level : server.getAllLevels()) {
            for (TempleDemonEntity demon : level.getEntitiesOfClass(TempleDemonEntity.class, new AABB(level.getWorldBorder().getMinX(), level.getMinBuildHeight(), level.getWorldBorder().getMinZ(), level.getWorldBorder().getMaxX(), level.getMaxBuildHeight(), level.getWorldBorder().getMaxZ()))) {
                applyBoosts(demon);
            }
        }

        // Sync to all clients
        syncToAllClients(server, true);
    }

    /**
     * Ends the blood moon: clears flag, removes demon boosts, syncs to all clients.
     */
    public static void endBloodMoon(MinecraftServer server) {
        active = false;
        lastBloodMoonEndTick = server.overworld().getGameTime();

        // Remove attribute boosts from all TempleDemonEntity in all server levels
        for (ServerLevel level : server.getAllLevels()) {
            for (TempleDemonEntity demon : level.getEntitiesOfClass(TempleDemonEntity.class, new AABB(level.getWorldBorder().getMinX(), level.getMinBuildHeight(), level.getWorldBorder().getMinZ(), level.getWorldBorder().getMaxX(), level.getMaxBuildHeight(), level.getWorldBorder().getMaxZ()))) {
                removeBoosts(demon);
            }
        }

        // Sync to all clients
        syncToAllClients(server, false);
    }

    private static void applyBoosts(TempleDemonEntity demon) {
        NichirinModConfig.BloodMoonConfig cfg = NichirinModConfig.get().bloodMoon;
        double attackBoost = cfg.demonAttackBoostPercent / 100.0;
        double speedBoost  = cfg.demonSpeedBoostPercent  / 100.0;

        AttributeInstance attackAttr = demon.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null && attackAttr.getModifier(ATTACK_MODIFIER_ID) == null && attackBoost > 0) {
            attackAttr.addPermanentModifier(new AttributeModifier(
                    ATTACK_MODIFIER_ID,
                    attackBoost,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }

        AttributeInstance speedAttr = demon.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null && speedAttr.getModifier(SPEED_MODIFIER_ID) == null && speedBoost > 0) {
            speedAttr.addPermanentModifier(new AttributeModifier(
                    SPEED_MODIFIER_ID,
                    speedBoost,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }

    private static void removeBoosts(TempleDemonEntity demon) {
        AttributeInstance attackAttr = demon.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.removeModifier(ATTACK_MODIFIER_ID);
        }

        AttributeInstance speedAttr = demon.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER_ID);
        }
    }

    /**
     * Applies or removes boosts to a newly spawned demon during an active blood moon.
     * Call this from TempleDemonEntity's onAddedToWorld if desired.
     */
    public static void applyBoostsIfActive(TempleDemonEntity demon) {
        if (active) {
            applyBoosts(demon);
        }
    }

    private static void syncToAllClients(MinecraftServer server, boolean isActive) {
        try {
            BloodMoonSyncPacket packet = new BloodMoonSyncPacket(isActive);
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            packet.toBytes(buf);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                NetworkManager.sendToPlayer(player, NichirinPacketRegistry.BLOOD_MOON_SYNC_ID, NetworkBufferUtils.serverCopy(buf, player));
            }
            buf.release();
        } catch (Exception e) {
            // Do not crash the server on packet failure
        }
    }
}