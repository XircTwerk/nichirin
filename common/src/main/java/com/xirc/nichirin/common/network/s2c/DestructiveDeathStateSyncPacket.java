package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.client.data.ClientDestructiveDeathState;
import com.xirc.nichirin.common.attack.moves.demon.destructive.DestructiveDeathState;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * S2C — syncs a player's {@link DestructiveDeathState} flags to their client.
 *
 * <p>Sent on every state change so the HUD/UI can reflect which Destructive Death toggles
 * are currently active.</p>
 */
public class DestructiveDeathStateSyncPacket {

    private final boolean shockwaveEnabled;
    private final boolean overdriveEnabled;
    private final boolean compassActive;
    private final boolean compassOverdrive;
    private final long compassExpiryTick;

    public DestructiveDeathStateSyncPacket(DestructiveDeathState.State state) {
        this.shockwaveEnabled = state.shockwaveEnabled;
        this.overdriveEnabled = state.overdriveEnabled;
        this.compassActive = state.compassActive;
        this.compassOverdrive = state.compassOverdrive;
        this.compassExpiryTick = state.compassExpiryTick;
    }

    public DestructiveDeathStateSyncPacket(FriendlyByteBuf buf) {
        this.shockwaveEnabled = buf.readBoolean();
        this.overdriveEnabled = buf.readBoolean();
        this.compassActive = buf.readBoolean();
        this.compassOverdrive = buf.readBoolean();
        this.compassExpiryTick = buf.readLong();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(shockwaveEnabled);
        buf.writeBoolean(overdriveEnabled);
        buf.writeBoolean(compassActive);
        buf.writeBoolean(compassOverdrive);
        buf.writeLong(compassExpiryTick);
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        ClientDestructiveDeathState.update(shockwaveEnabled, overdriveEnabled,
                compassActive, compassOverdrive, compassExpiryTick);
    }

    /** Convenience: build + send to the given player. */
    public static void send(ServerPlayer player) {
        DestructiveDeathStateSyncPacket packet =
                new DestructiveDeathStateSyncPacket(DestructiveDeathState.get(player.getUUID()));
        NichirinPacketRegistry.sendToPlayer(packet, player);
    }
}
