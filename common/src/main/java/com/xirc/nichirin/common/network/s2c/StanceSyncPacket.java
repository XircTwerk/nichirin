package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.client.gui.StanceBarHUD;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Syncs stance level to clients for the orange stance bar
 */
public class StanceSyncPacket {
    private final int playerId;
    private final float currentStance;
    private final float maxStance;

    public StanceSyncPacket(int playerId, float currentStance, float maxStance) {
        this.playerId = playerId;
        this.currentStance = currentStance;
        this.maxStance = maxStance;
    }

    public StanceSyncPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readInt();
        this.currentStance = buf.readFloat();
        this.maxStance = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(playerId);
        buf.writeFloat(currentStance);
        buf.writeFloat(maxStance);
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(playerId);
            if (entity instanceof Player player && player == mc.player) {
                // Update the stance HUD for the local player
                StanceBarHUD.updateStance(currentStance, maxStance);
            }
        }
    }
}