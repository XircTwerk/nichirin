package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.common.system.movement.MovementContext;
import com.xirc.nichirin.common.system.abilities.PlayerDoubleJump;
import com.xirc.nichirin.common.util.StaminaManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Packet sent from client to server to request a double jump with directional input
 */
public class DoubleJumpPacket {

    // Movement input data
    private boolean forward;
    private boolean backward;
    private boolean left;
    private boolean right;

    public DoubleJumpPacket() {
        this.forward = false;
        this.backward = false;
        this.left = false;
        this.right = false;
    }

    public DoubleJumpPacket(boolean forward, boolean backward, boolean left, boolean right) {
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
    }

    public DoubleJumpPacket(FriendlyByteBuf buf) {
        this.forward = buf.readBoolean();
        this.backward = buf.readBoolean();
        this.left = buf.readBoolean();
        this.right = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(forward);
        buf.writeBoolean(backward);
        buf.writeBoolean(left);
        buf.writeBoolean(right);
    }

    public void handle(ServerPlayer player) {
        float currentStamina = StaminaManager.getStamina(player);

        MovementContext.DashInput input = new MovementContext.DashInput();
        input.forward = forward;
        input.backward = backward;
        input.left = left;
        input.right = right;

        PlayerDoubleJump.tryDoubleJump(player, input);

        float newStamina = StaminaManager.getStamina(player);

        if (currentStamina != newStamina) {
            StaminaManager.forceSyncToClient(player);
        }
    }

    public static DoubleJumpPacket fromMovementInput(boolean forward, boolean backward, boolean left, boolean right) {
        return new DoubleJumpPacket(forward, backward, left, right);
    }

    public static DoubleJumpPacket createEmpty() {
        return new DoubleJumpPacket();
    }
}