package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.config.NichirinServerConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;

public class ServerConfigSyncPacket {

    private final String configJson;

    public ServerConfigSyncPacket(String configJson) {
        this.configJson = configJson;
    }

    public ServerConfigSyncPacket(FriendlyByteBuf buf) {
        this.configJson = buf.readUtf(65535);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(configJson, 65535);
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        try {
            NichirinModConfig synced = NichirinServerConfig.fromJson(configJson);
            if (synced != null) {
                NichirinServerConfig.applySyncedConfig(synced);
            }
        } catch (Exception e) {
            BreathOfNichirin.LOGGER.warn("Rejected malformed server config sync.", e);
        }
    }
}