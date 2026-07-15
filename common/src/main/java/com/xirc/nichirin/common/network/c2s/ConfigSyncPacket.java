package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.config.NichirinServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client-to-server push of the full mod config, sent when an operator saves the in-game config
 * screen while connected to a remote server. Without this, the cloth-config screen only wrote
 * the CLIENT's own config file, so edits made in multiplayer silently never reached the server
 * (base damage multiplier, wisteriaDamagesDemons, etc.).
 */
public class ConfigSyncPacket {

    private final String configJson;

    public ConfigSyncPacket(String configJson) {
        this.configJson = configJson;
    }

    public ConfigSyncPacket(FriendlyByteBuf buf) {
        this.configJson = buf.readUtf(65535);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(configJson, 65535);
    }

    public void handle(ServerPlayer player) {
        if (!player.hasPermissions(2) && !player.getServer().isSingleplayer()) {
            player.displayClientMessage(Component.literal(
                            "You need operator permissions to change the server config.")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }
        NichirinModConfig parsed;
        try {
            parsed = NichirinServerConfig.fromJson(configJson);
        } catch (Exception e) {
            BreathOfNichirin.LOGGER.warn("Rejected malformed config sync from {}.",
                    player.getGameProfile().getName(), e);
            return;
        }
        if (parsed == null) return;
        NichirinServerConfig.save(parsed);
        BreathOfNichirin.LOGGER.info("Server config updated by {} via config screen.",
                player.getGameProfile().getName());
        player.displayClientMessage(Component.literal("Server config updated.")
                .withStyle(ChatFormatting.GREEN), false);
    }
}
