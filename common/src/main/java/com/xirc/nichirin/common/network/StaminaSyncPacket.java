package com.xirc.nichirin.common.network;

import com.xirc.nichirin.client.gui.StaminaBarHUD;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Syncs stamina level to clients
 */
public class StaminaSyncPacket {
    private final int playerId;
    private final float currentStamina;
    private final float maxStamina;

    public StaminaSyncPacket(int playerId, float currentStamina, float maxStamina) {
        this.playerId = playerId;
        this.currentStamina = currentStamina;
        this.maxStamina = maxStamina;
    }

    public StaminaSyncPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readInt();
        this.currentStamina = buf.readFloat();
        this.maxStamina = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(playerId);
        buf.writeFloat(currentStamina);
        buf.writeFloat(maxStamina);
    }

    @Environment(EnvType.CLIENT)
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(playerId);
            if (entity instanceof Player player && player == mc.player) {
                // Update the stamina HUD for the local player
                StaminaBarHUD.updateStamina(currentStamina, maxStamina);
            }
        }
    }
}