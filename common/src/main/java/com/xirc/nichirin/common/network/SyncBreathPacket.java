package com.xirc.nichirin.common.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Syncs breath level to clients
 */
public class SyncBreathPacket {
    private final int playerId;
    private final float breathLevel;

    public SyncBreathPacket(int playerId, float breathLevel) {
        this.playerId = playerId;
        this.breathLevel = breathLevel;
    }

    public SyncBreathPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readInt();
        this.breathLevel = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(playerId);
        buf.writeFloat(breathLevel);
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(playerId);
            if (entity instanceof Player player) {
                // TODO: Set breath level on client
                // player.getCapability(BreathCapability.INSTANCE).ifPresent(cap -> cap.setBreath(breathLevel));
            }
        }
    }
}