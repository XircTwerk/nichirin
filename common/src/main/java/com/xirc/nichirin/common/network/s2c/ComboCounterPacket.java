package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.client.gui.ComboHUD;
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
    private final int styleScore;
    private final String styleRank;

    public ComboCounterPacket(int comboCount, int stunDurationTicks, float damage) {
        this(comboCount, stunDurationTicks, damage, 0, "");
    }

    public ComboCounterPacket(int comboCount, int stunDurationTicks, float damage, int styleScore, String styleRank) {
        this.comboCount = comboCount;
        this.stunDurationTicks = stunDurationTicks;
        this.damage = damage;
        this.styleScore = styleScore;
        this.styleRank = styleRank;
    }

    public ComboCounterPacket(FriendlyByteBuf buf) {
        this.comboCount = buf.readInt();
        this.stunDurationTicks = buf.readInt();
        this.damage = buf.readFloat();
        this.styleScore = buf.readInt();
        this.styleRank = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(comboCount);
        buf.writeInt(stunDurationTicks);
        buf.writeFloat(damage);
        buf.writeInt(styleScore);
        buf.writeUtf(styleRank);
    }

    public void handleClient() {
        ComboHUD.updateCombo(comboCount, stunDurationTicks, styleScore, styleRank);
        if (damage > 0) {
            ComboHUD.addDamage(damage);
        }
    }

}
