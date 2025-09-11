package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.common.system.movement.MovementContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Packet for syncing WASD+Jump input states to server
 */
public class MovementInputSyncPacket {

    private final boolean forward;
    private final boolean backward;
    private final boolean left;
    private final boolean right;
    private final boolean jump;

    public MovementInputSyncPacket(boolean forward, boolean backward, boolean left, boolean right, boolean jump) {
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
        this.jump = jump;
    }

    public MovementInputSyncPacket(FriendlyByteBuf buf) {
        this.forward = buf.readBoolean();
        this.backward = buf.readBoolean();
        this.left = buf.readBoolean();
        this.right = buf.readBoolean();
        this.jump = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(forward);
        buf.writeBoolean(backward);
        buf.writeBoolean(left);
        buf.writeBoolean(right);
        buf.writeBoolean(jump);
    }

    public void handle(ServerPlayer player) {
        if (player != null) {
            // Update the movement context with current input state
            MovementContext.updatePlayerInput(player.getUUID(), forward, backward, left, right, jump);
        }
    }
}