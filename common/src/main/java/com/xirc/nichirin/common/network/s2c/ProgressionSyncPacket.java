package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.data.ClientProgressionCache;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.registry.MovesetRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

/**
 * Packet for syncing breathing style unlock status to client
 */
public class ProgressionSyncPacket {

    public static final ResourceLocation SYNC_PROGRESSION = BreathOfNichirin.id("sync_progression");

    public static void register() {
        // Client receives unlock status from server
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_PROGRESSION, (buf, context) -> {
            int count = buf.readInt();
            Set<String> unlockedStyles = new HashSet<>();

            for (int i = 0; i < count; i++) {
                unlockedStyles.add(buf.readUtf());
            }

            context.queue(() -> {
                // Store this on the client somehow - maybe in a static map keyed by player UUID
                // or extend your client-side data storage
                ClientProgressionCache.setUnlockedStyles(unlockedStyles);
            });
        });
    }

    /**
     * Sends unlock status to player
     */
    public static void sendToPlayer(ServerPlayer player) {
        var progression = PlayerDataProvider.getData(player).getProgression();
        Set<String> unlockedStyles = new HashSet<>();

        // Get all unlocked styles
        for (String styleId : MovesetRegistry.getAllMovesetIds()) {
            if (progression.isStyleUnlocked(styleId)) {
                unlockedStyles.add(styleId);
            }
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(unlockedStyles.size());
        for (String style : unlockedStyles) {
            buf.writeUtf(style);
        }

        NetworkManager.sendToPlayer(player, SYNC_PROGRESSION, buf);
    }
}
