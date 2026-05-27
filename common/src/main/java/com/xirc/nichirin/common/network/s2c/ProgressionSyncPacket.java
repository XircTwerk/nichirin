package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.data.ClientProgressionCache;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.registry.NichirinMovesetRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

/**
 * Packet for syncing moveset unlock status to client (breathing techniques and demon arts)
 */
public class ProgressionSyncPacket {

    public static final ResourceLocation SYNC_PROGRESSION = new ResourceLocation(BreathOfNichirin.MOD_ID, "sync_progression");

    public static void register() {
        // Client receives unlock status from server
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_PROGRESSION, (buf, context) -> {
            int count = buf.readInt();
            Set<String> unlockedMovesets = new HashSet<>();

            for (int i = 0; i < count; i++) {
                unlockedMovesets.add(buf.readUtf());
            }

            context.queue(() -> {
                ClientProgressionCache.setUnlockedMovesets(unlockedMovesets);
            });
        });
    }

    /**
     * Sends unlock status to player
     */
    public static void sendToPlayer(ServerPlayer player) {
        var progression = PlayerDataProvider.getData(player).getProgression();
        Set<String> unlockedMovesets = new HashSet<>();

        // Get all unlocked movesets
        for (String movesetId : NichirinMovesetRegistry.getAllMovesetIds()) {
            if (progression.isMovesetUnlocked(movesetId)) {
                unlockedMovesets.add(movesetId);
            }
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(unlockedMovesets.size());
        for (String moveset : unlockedMovesets) {
            buf.writeUtf(moveset);
        }

        NetworkManager.sendToPlayer(player, SYNC_PROGRESSION, buf);
    }
}
