package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Packet for executing demon art moves from the attack wheel
 * Similar to BreathingMovePacket but for demon arts
 */
public class DemonMovePacket {

    private final int moveIndex;
    private final boolean fromWheel;

    public DemonMovePacket(int moveIndex, boolean fromWheel) {
        this.moveIndex = moveIndex;
        this.fromWheel = fromWheel;
    }

    public DemonMovePacket(FriendlyByteBuf buf) {
        this.moveIndex = buf.readInt();
        this.fromWheel = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(moveIndex);
        buf.writeBoolean(fromWheel);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(moveIndex);
        buf.writeBoolean(fromWheel);
    }

    public void handle(ServerPlayer player) {
        // Check if player is stunned
        if (player.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return;
        }

        // Get the player's current moveset
        var moveset = MovesetHelper.getMoveset(player);
        if (moveset == null || !moveset.isDemonMoveset()) {
            player.displayClientMessage(
                    Component.literal("No demon art equipped!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            return;
        }

        // Validate move index
        if (moveIndex < 0 || moveIndex >= moveset.getMoveCount()) {
            player.displayClientMessage(
                    Component.literal("Invalid demon art move!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            return;
        }

        // Get the move configuration
        var moveConfig = moveset.getMove(moveIndex);
        if (moveConfig == null) {
            return;
        }

        try {
            // Execute the demon art move
            moveset.performMove(player, moveIndex);

            // Block inputs after execution (same as breathing moves)
            com.xirc.nichirin.common.util.MultiplayerInputHandler.blockInputsAfterMoveExecution(player);

        } catch (Exception e) {
            player.displayClientMessage(
                    Component.literal("Failed to execute demon art!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            e.printStackTrace();
        }
    }

    // Getters for packet registry
    public int getMoveIndex() {
        return moveIndex;
    }

    public boolean isFromWheel() {
        return fromWheel;
    }
}