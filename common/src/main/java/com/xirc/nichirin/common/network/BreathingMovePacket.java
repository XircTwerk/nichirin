package com.xirc.nichirin.common.network;

import com.xirc.nichirin.common.util.enums.MoveClass;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sent when player uses a breathing technique
 */
public class BreathingMovePacket {
    private final MoveClass moveClass;
    private final boolean pressed;

    public BreathingMovePacket(MoveClass moveClass, boolean pressed) {
        this.moveClass = moveClass;
        this.pressed = pressed;
    }

    public BreathingMovePacket(FriendlyByteBuf buf) {
        this.moveClass = buf.readEnum(MoveClass.class);
        this.pressed = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(moveClass);
        buf.writeBoolean(pressed);
    }

    public void handle(ServerPlayer player) {
        // Handle breathing move input on server
        // TODO: Implement breathing move system integration
        // Example:
        // BreathingSystem.handleMoveInput(player, moveClass, pressed);
    }
}