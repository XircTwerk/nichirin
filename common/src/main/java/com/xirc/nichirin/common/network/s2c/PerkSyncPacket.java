package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.client.data.ClientPerkCache;
import com.xirc.nichirin.common.system.perks.PerkData;
import com.xirc.nichirin.common.system.perks.PerkPreset;
import com.xirc.nichirin.common.system.perks.PerkTier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C — syncs a player's full {@link PerkData} to their client.
 *
 * <p>Sent after any server-side perk state change (equip, unequip, upgrade,
 * toggle, preset apply) and on player login/respawn.</p>
 */
public class PerkSyncPacket {

    private final CompoundTag nbt;

    public PerkSyncPacket(PerkData data) {
        this.nbt = data.save();
    }

    public PerkSyncPacket(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(nbt);
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        PerkData data = new PerkData();
        data.load(nbt);
        ClientPerkCache.update(data);
    }
}
