package com.xirc.nichirin.common.network;

import com.xirc.nichirin.client.gui.BreathingBarHUD;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Syncs breathing power level to clients
 */
public class BreathingSyncPacket {
    private final int playerId;
    private final float currentBreath;
    private final float maxBreath;

    public BreathingSyncPacket(int playerId, float currentBreath, float maxBreath) {
        this.playerId = playerId;
        this.currentBreath = currentBreath;
        this.maxBreath = maxBreath;
    }

    public BreathingSyncPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readInt();
        this.currentBreath = buf.readFloat();
        this.maxBreath = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(playerId);
        buf.writeFloat(currentBreath);
        buf.writeFloat(maxBreath);
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(playerId);
            if (entity instanceof Player player && player == mc.player) {
                // Update the breathing HUD for the local player
                BreathingBarHUD.updateBreath(currentBreath, maxBreath);
            }
        }
    }
}