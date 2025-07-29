package com.xirc.nichirin.common.data;

import com.xirc.nichirin.BreathOfNichirin;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Packet for syncing breathing style data between client and server
 * Now includes unlock validation
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
                if (player != null) {
                    BreathingStyleData data = PlayerDataProvider.getBreathingStyleData(player);
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
                    if (movesetId != null && !MovesetRegistry.isRegistered(movesetId)) {
                        // Invalid moveset ID
                        serverPlayer.sendSystemMessage(Component.literal(
                                "§cInvalid breathing style: " + movesetId
                        ));
                        return;
                    }

                    // Check if the player has unlocked this breathing style
                    if (movesetId != null && !ProgressionHelper.isStyleUnlocked(serverPlayer, movesetId)) {
                        // Player hasn't unlocked this style
                        String requirement = ProgressionHelper.getUnlockRequirement(movesetId);
                        serverPlayer.sendSystemMessage(Component.literal(
                                "§cYou haven't unlocked this breathing style! §fRequirement: §e" + requirement
                        ));
                        return;
                    }

                    // All checks passed - update the moveset
                    PlayerDataProvider.updateAndSync(serverPlayer, movesetId);

                    // Send confirmation message
                    if (movesetId != null) {
                        String styleName = formatStyleName(movesetId);
                        serverPlayer.sendSystemMessage(Component.literal(
                                "§aSwitched to " + styleName + "."
                        ));
                    } else {
                        serverPlayer.sendSystemMessage(Component.literal(
                                "§7Cleared breathing style."
                        ));
                    }
                }
            });
        });
    }

    /**
     * Formats a breathing style ID for display
     */
    private static String formatStyleName(String styleId) {
        String[] parts = styleId.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return formatted.toString();
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