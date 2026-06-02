package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.client.handler.BloodMoonClientState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

public class BloodMoonSyncPacket {

    private final boolean active;

    public BloodMoonSyncPacket(boolean active) {
        this.active = active;
    }

    public BloodMoonSyncPacket(FriendlyByteBuf buf) {
        this.active = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        BloodMoonClientState.setActive(active);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if (active) {
                mc.player.sendSystemMessage(
                        Component.literal("\u00a74\u263d A Blood Moon is rising... \u00a7r")
                );
            } else {
                mc.player.sendSystemMessage(
                        Component.literal("\u00a77\u263c The blood moon has subsided... for now. \u00a7r")
                );
            }
        }
    }
}