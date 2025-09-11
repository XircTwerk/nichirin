package com.xirc.nichirin.common.network.s2c;

import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C packet to sync combo counter data to client for display
 */
@Getter
public class ComboCounterPacket {

    private final int comboCount;
    private final int stunDurationTicks;
    private final float damage;

    public ComboCounterPacket(int comboCount, int stunDurationTicks, float damage) {
        this.comboCount = comboCount;
        this.stunDurationTicks = stunDurationTicks;
        this.damage = damage;
    }

    public ComboCounterPacket(FriendlyByteBuf buf) {
        this.comboCount = buf.readInt();
        this.stunDurationTicks = buf.readInt();
        this.damage = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(comboCount);
        buf.writeInt(stunDurationTicks);
        buf.writeFloat(damage);
    }

    public void handleClient() {
        // Update client-side combo display
        com.xirc.nichirin.client.gui.ComboHUD.updateCombo(comboCount, stunDurationTicks);
        if (damage > 0) {
            com.xirc.nichirin.client.gui.ComboHUD.addDamage(damage);
        }
    }

}