package com.xirc.nichirin.common.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public final class NetworkBufferUtils {
    private NetworkBufferUtils() {
    }

    public static RegistryFriendlyByteBuf client(FriendlyByteBuf buf) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            throw new IllegalStateException("Cannot send a play packet without a client level");
        }
        return new RegistryFriendlyByteBuf(buf, minecraft.level.registryAccess());
    }

    public static RegistryFriendlyByteBuf server(FriendlyByteBuf buf, ServerPlayer player) {
        return new RegistryFriendlyByteBuf(buf, player.registryAccess());
    }

    public static RegistryFriendlyByteBuf serverCopy(FriendlyByteBuf buf, ServerPlayer player) {
        return new RegistryFriendlyByteBuf(buf.copy(), player.registryAccess());
    }
}