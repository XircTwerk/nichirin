package com.xirc.nichirin.common.data;

import com.xirc.nichirin.BreathOfNichirin;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Packet for syncing breathing style data between client and server
 */
public class BreathingStyleSyncPacket {

    public static final ResourceLocation SYNC_BREATHING_STYLE = BreathOfNichirin.id("sync_breathing_style");
    public static final ResourceLocation REQUEST_STYLE_CHANGE = BreathOfNichirin.id("request_style_change");

    /**
     * Registers the packet handlers
     */
    public static void register() {
        // Client receives breathing style sync from server
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_BREATHING_STYLE, (buf, context) -> {
            String movesetId = buf.readBoolean() ? buf.readUtf() : null;

            context.queue(() -> {
                Player player = context.getPlayer();
                if (player != null) { // Add null check here
                    BreathingStyleData data = PlayerDataProvider.getData(player);
                    data.setMovesetId(movesetId);
                }
            });
        });

        // Server receives style change request from client
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, REQUEST_STYLE_CHANGE, (buf, context) -> {
            String movesetId = buf.readBoolean() ? buf.readUtf() : null;

            context.queue(() -> {
                Player player = context.getPlayer();
                if (player instanceof ServerPlayer) {
                    ServerPlayer serverPlayer = (ServerPlayer) player;
                    // Validate the moveset exists
                    if (movesetId == null || MovesetRegistry.isRegistered(movesetId)) {
                        PlayerDataProvider.updateAndSync(serverPlayer, movesetId);
                    }
                }
            });
        });
    }

    /**
     * Sends breathing style data to a specific player
     */
    public static void sendToPlayer(ServerPlayer player, String movesetId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBoolean(movesetId != null);
        if (movesetId != null) {
            buf.writeUtf(movesetId);
        }

        NetworkManager.sendToPlayer(player, SYNC_BREATHING_STYLE, buf);
    }

    /**
     * Sends breathing style data to all players in the same level
     */
    public static void sendToTracking(ServerPlayer player, String movesetId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBoolean(movesetId != null);
        if (movesetId != null) {
            buf.writeUtf(movesetId);
        }

        // Send to all players in the same dimension
        player.server.getPlayerList().getPlayers().stream()
                .filter(p -> p.level() == player.level())
                .forEach(p -> NetworkManager.sendToPlayer(p, SYNC_BREATHING_STYLE, buf));
    }

    /**
     * Client requests a breathing style change
     */
    public static void requestStyleChange(String movesetId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBoolean(movesetId != null);
        if (movesetId != null) {
            buf.writeUtf(movesetId);
        }

        NetworkManager.sendToServer(REQUEST_STYLE_CHANGE, buf);
    }
}