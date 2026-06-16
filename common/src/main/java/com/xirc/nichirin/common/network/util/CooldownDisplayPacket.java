package com.xirc.nichirin.common.network.util;

import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.util.NetworkBufferUtils;
import com.xirc.nichirin.common.util.enums.BreathingStyle;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class CooldownDisplayPacket {

    private static final ResourceLocation PACKET_ID = ResourceLocation.fromNamespaceAndPath("nichirin", "cooldown_display");
    private static final ResourceLocation DEFAULT_ICON = ResourceLocation.fromNamespaceAndPath("nichirin", "textures/icons/default_move.png");
    private static final int DEFAULT_COLOR = 0xFFDDDDDD;

    public static void registerClient() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PACKET_ID, (buf, context) -> {
            String moveName = buf.readUtf();
            int cooldownTicks = buf.readInt();
            boolean hasVisuals = buf.readableBytes() > 0;
            ResourceLocation icon = hasVisuals ? buf.readResourceLocation() : null;
            int elementColorARGB = hasVisuals && buf.readableBytes() >= Integer.BYTES ? buf.readInt() : DEFAULT_COLOR;

            context.queue(() -> {
                if (hasVisuals) {
                    CooldownHUD.setCooldown(moveName, cooldownTicks, icon, elementColorARGB);
                } else {
                    CooldownHUD.setCooldown(moveName, cooldownTicks);
                }
            });
        });
    }

    public static void sendToClient(ServerPlayer player, String moveName, int cooldownTicks) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(moveName);
        buf.writeInt(cooldownTicks);

        NetworkManager.sendToPlayer(player, PACKET_ID, NetworkBufferUtils.server(buf, player));
    }

    public static void sendToClient(ServerPlayer player, String moveName, int cooldownTicks,
                                    ResourceLocation icon, int elementColorARGB) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(moveName);
        buf.writeInt(cooldownTicks);
        buf.writeResourceLocation(icon != null ? icon : DEFAULT_ICON);
        buf.writeInt(elementColorARGB);

        NetworkManager.sendToPlayer(player, PACKET_ID, NetworkBufferUtils.server(buf, player));
    }

    public static void sendToClient(ServerPlayer player, String movesetId, AbstractMoveset.MoveConfiguration config) {
        if (config == null) {
            return;
        }

        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(
                "nichirin",
                "textures/icons/" + movesetId + "/" + config.getMoveId() + ".png");
        sendToClient(player, config.getDisplayName(), config.getCooldownOrDefault(0), icon, getElementColor(movesetId));
    }

    private static int getElementColor(String movesetId) {
        BreathingStyle style = BreathingStyle.fromMovesetId(movesetId);
        return style != null ? 0xFF000000 | style.getColor() : DEFAULT_COLOR;
    }
}
