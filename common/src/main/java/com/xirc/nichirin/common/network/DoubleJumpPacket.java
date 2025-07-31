package com.xirc.nichirin.common.network;

import com.xirc.nichirin.common.system.movement.MovementContext;
import com.xirc.nichirin.common.system.slayerabilities.PlayerDoubleJump;
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

    /**
     * Empty constructor for packet (backwards compatibility)
     */
    public DoubleJumpPacket() {
        this.forward = false;
        this.backward = false;
        this.left = false;
        this.right = false;
    }

    /**
     * Constructor for creating packet with movement input
     */
    public DoubleJumpPacket(boolean forward, boolean backward, boolean left, boolean right) {
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
    }

    /**
     * Constructor for reading from network buffer
     */
    public DoubleJumpPacket(FriendlyByteBuf buf) {
        this.forward = buf.readBoolean();
        this.backward = buf.readBoolean();
        this.left = buf.readBoolean();
        this.right = buf.readBoolean();
    }

    /**
     * Write packet data to network buffer
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(forward);
        buf.writeBoolean(backward);
        buf.writeBoolean(left);
        buf.writeBoolean(right);
    }

    /**
     * Handle the packet on the server
     */
    public void handle(ServerPlayer player) {
        // Debug stamina state BEFORE attempting double jump
        float currentStamina = StaminaManager.getStamina(player);
        float maxStamina = StaminaManager.getMaxStamina(player);
        float staminaCost = PlayerDoubleJump.getStaminaCost();

        // Create movement input from packet data
        MovementContext.DashInput input = new MovementContext.DashInput();
        input.forward = forward;
        input.backward = backward;
        input.left = left;
        input.right = right;

        // Use the new directional double jump method
        PlayerDoubleJump.tryDoubleJump(player, input);

        // Debug stamina state AFTER attempting double jump
        float newStamina = StaminaManager.getStamina(player);

        // Force sync to ensure client gets the update
        if (currentStamina != newStamina) {
            StaminaManager.forceSyncToClient(player);
        }
    }

    /**
     * Static method to create packet from current movement input
     */
    public static DoubleJumpPacket fromMovementInput(boolean forward, boolean backward, boolean left, boolean right) {
        return new DoubleJumpPacket(forward, backward, left, right);
    }

    /**
     * Static method to create empty packet (no directional input)
     */
    public static DoubleJumpPacket createEmpty() {
        return new DoubleJumpPacket();
    }
}