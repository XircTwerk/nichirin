package com.xirc.nichirin.common.network.util;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.data.MovesetData;
import com.xirc.nichirin.registry.NichirinMovesetRegistry;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.data.ProgressionHelper;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Packet for syncing moveset data between client and server (breathing techniques and demon arts)
 * Includes unlock validation for both types
 */
public class MovesetSyncPacket {

    public static final ResourceLocation SYNC_MOVESET = BreathOfNichirin.id("sync_moveset");
    public static final ResourceLocation REQUEST_MOVESET_CHANGE = BreathOfNichirin.id("request_moveset_change");

    /**
     * Registers the packet handlers
     */
    public static void register() {
        // Client receives moveset sync from server
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_MOVESET, (buf, context) -> {
            String movesetId = buf.readBoolean() ? buf.readUtf() : null;

            context.queue(() -> {
                Player player = context.getPlayer();
                if (player != null) {
                    MovesetData data = PlayerDataProvider.getMovesetData(player);
                    data.setMovesetId(movesetId);
                }
            });
        });

        // Server receives moveset change request from client
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, REQUEST_MOVESET_CHANGE, (buf, context) -> {
            String movesetId = buf.readBoolean() ? buf.readUtf() : null;

            context.queue(() -> {
                Player player = context.getPlayer();
                if (player instanceof ServerPlayer) {
                    ServerPlayer serverPlayer = (ServerPlayer) player;

                    // Validate the moveset exists
                    if (movesetId != null && !NichirinMovesetRegistry.isRegistered(movesetId)) {
                        // Invalid moveset ID
                        serverPlayer.sendSystemMessage(Component.literal(
                                "§cInvalid moveset: " + movesetId
                        ));
                        return;
                    }

                    // Check if the player has unlocked this moveset
                    if (movesetId != null && !ProgressionHelper.isMovesetUnlocked(serverPlayer, movesetId)) {
                        // Player hasn't unlocked this moveset
                        String requirement = ProgressionHelper.getUnlockRequirement(movesetId);
                        serverPlayer.sendSystemMessage(Component.literal(
                                "§cYou haven't unlocked this moveset! §fRequirement: §e" + requirement
                        ));
                        return;
                    }

                    // All checks passed - update the moveset
                    PlayerDataProvider.updateAndSync(serverPlayer, movesetId);

                    // Send confirmation message
                    if (movesetId != null) {
                        String movesetName = formatMovesetName(movesetId);
                        String movesetType = getMovesetTypeDisplay(movesetId);
                        serverPlayer.sendSystemMessage(Component.literal(
                                "§aSwitched to " + movesetName + " §7(" + movesetType + ")§a."
                        ));
                    } else {
                        serverPlayer.sendSystemMessage(Component.literal(
                                "§7Cleared moveset."
                        ));
                    }
                }
            });
        });
    }

    /**
     * Formats a moveset ID for display
     */
    private static String formatMovesetName(String movesetId) {
        String[] parts = movesetId.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return formatted.toString();
    }

    /**
     * Gets the display type for a moveset (Breathing Technique or Demon Art)
     */
    private static String getMovesetTypeDisplay(String movesetId) {
        if (movesetId.contains("breathing")) {
            return "Breathing Technique";
        } else if (movesetId.contains("demon")) {
            return "Demon Art";
        } else {
            return "Technique";
        }
    }

    /**
     * Sends moveset data to a specific player
     */
    public static void sendToPlayer(ServerPlayer player, String movesetId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBoolean(movesetId != null);
        if (movesetId != null) {
            buf.writeUtf(movesetId);
        }

        NetworkManager.sendToPlayer(player, SYNC_MOVESET, buf);
    }

    /**
     * Sends moveset data to all players in the same level
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
                .forEach(p -> NetworkManager.sendToPlayer(p, SYNC_MOVESET, buf));
    }

    /**
     * Client requests a moveset change
     */
    public static void requestMovesetChange(String movesetId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBoolean(movesetId != null);
        if (movesetId != null) {
            buf.writeUtf(movesetId);
        }

        NetworkManager.sendToServer(REQUEST_MOVESET_CHANGE, buf);
    }

    // Legacy methods for backwards compatibility

    /**
     * @deprecated Use sendToPlayer instead
     */
    @Deprecated
    public static void sendBreathingStyleToPlayer(ServerPlayer player, String movesetId) {
        sendToPlayer(player, movesetId);
    }

    /**
     * @deprecated Use requestMovesetChange instead
     */
    @Deprecated
    public static void requestStyleChange(String movesetId) {
        requestMovesetChange(movesetId);
    }
}