package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.common.system.sheathing.SheathPosition;
import com.xirc.nichirin.common.system.sheathing.SheathingManager;
import com.xirc.nichirin.common.system.sheathing.UnsheatheAttackType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class SheathConfigPacket {
    private final SheathPosition position;
    private final boolean enabled;
    private final int hotbarSlot;
    private final int priority;
    private final UnsheatheAttackType tapAttack;
    private final UnsheatheAttackType holdAttack;
    private final boolean visible;

    public SheathConfigPacket(SheathPosition position, boolean enabled, int hotbarSlot, int priority,
                              UnsheatheAttackType tapAttack, UnsheatheAttackType holdAttack, boolean visible) {
        this.position = position;
        this.enabled = enabled;
        this.hotbarSlot = hotbarSlot;
        this.priority = priority;
        this.tapAttack = tapAttack;
        this.holdAttack = holdAttack;
        this.visible = visible;
    }

    public SheathConfigPacket(FriendlyByteBuf buf) {
        this.position = buf.readEnum(SheathPosition.class);
        this.enabled = buf.readBoolean();
        this.hotbarSlot = buf.readInt();
        this.priority = buf.readInt();
        this.tapAttack = buf.readEnum(UnsheatheAttackType.class);
        this.holdAttack = buf.readEnum(UnsheatheAttackType.class);
        this.visible = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(position);
        buf.writeBoolean(enabled);
        buf.writeInt(hotbarSlot);
        buf.writeInt(priority);
        buf.writeEnum(tapAttack);
        buf.writeEnum(holdAttack);
        buf.writeBoolean(visible);
    }

    public void handle(ServerPlayer player) {
        SheathingManager.updateSlot(player, position, enabled, hotbarSlot, priority, tapAttack, holdAttack, visible);
    }
}
