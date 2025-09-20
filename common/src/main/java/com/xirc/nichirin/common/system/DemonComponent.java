package com.xirc.nichirin.common.system;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Component for handling demon blood data and syncing
 */
public class DemonComponent {

    public static final ResourceLocation DEMON_SYNC_PACKET = new ResourceLocation("nichirin", "demon_sync");

    // Client-side blood points for display
    private static int clientBloodPoints = 10;

    /**
     * Gets blood points for display (client-side)
     */
    public static int getClientBloodPoints() {
        return clientBloodPoints;
    }

    /**
     * Sets blood points from server sync (client-side)
     */
    public static void setClientBloodPoints(int bloodPoints) {
        clientBloodPoints = Math.max(0, Math.min(bloodPoints, 10));
    }

    /**
     * Syncs demon data to a player
     */
    public static void sync(ServerPlayer player) {
        if (DemonManager.isDemon(player)) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            writeSyncPacket(buf, player);
            NetworkManager.sendToPlayer(player, DEMON_SYNC_PACKET, buf);
        }
    }

    /**
     * Writes sync packet data
     */
    private static void writeSyncPacket(FriendlyByteBuf buf, ServerPlayer player) {
        buf.writeInt(DemonManager.getBloodPoints(player));
        buf.writeBoolean(DemonManager.isDemon(player));
    }

    /**
     * Applies sync packet data (client-side)
     */
    public static void applySyncPacket(FriendlyByteBuf buf) {
        int bloodPoints = buf.readInt();
        boolean isDemon = buf.readBoolean();

        if (isDemon) {
            setClientBloodPoints(bloodPoints);
        }
    }

    /**
     * Saves demon data to NBT
     */
    public static CompoundTag save(Player player) {
        CompoundTag tag = new CompoundTag();
        if (DemonManager.isDemon(player)) {
            tag.putInt("BloodPoints", DemonManager.getBloodPoints(player));
            tag.putBoolean("IsDemon", true);
        }
        return tag;
    }

    /**
     * Loads demon data from NBT
     */
    public static void load(Player player, CompoundTag tag) {
        if (tag.getBoolean("IsDemon")) {
            int bloodPoints = tag.getInt("BloodPoints");
            DemonManager.setBloodPoints(player, bloodPoints);
        }
    }

    /**
     * Handles packet on client side
     */
    public static void handleSyncPacket(FriendlyByteBuf buf) {
        applySyncPacket(buf);
    }
}