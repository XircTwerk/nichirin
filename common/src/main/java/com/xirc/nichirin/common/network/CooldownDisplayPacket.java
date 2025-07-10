package com.xirc.nichirin.common.network;

import com.xirc.nichirin.client.gui.CooldownHUD;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class CooldownDisplayPacket {

    private static final ResourceLocation PACKET_ID = new ResourceLocation("nichirin", "cooldown_display");

    /**
     * Register the packet handler on client
     */
    public static void registerClient() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PACKET_ID, (buf, context) -> {
            String moveName = buf.readUtf();
            int cooldownTicks = buf.readInt();

            context.queue(() -> {
                // Display the cooldown on client
                CooldownHUD.setCooldown(moveName, cooldownTicks);
            });
        });
    }

    /**
     * Send cooldown display packet to client
     */
    public static void sendToClient(ServerPlayer player, String moveName, int cooldownTicks) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(moveName);
        buf.writeInt(cooldownTicks);

        NetworkManager.sendToPlayer(player, PACKET_ID, buf);
    }
}