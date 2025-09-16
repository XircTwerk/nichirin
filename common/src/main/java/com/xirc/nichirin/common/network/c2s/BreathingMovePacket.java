package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.util.KatanaInputHandler;
import com.xirc.nichirin.common.util.MultiplayerInputHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sent when player uses a breathing technique from the attack wheel
 */
public class BreathingMovePacket {
    private final int moveIndex;
    private final boolean pressed;

    public BreathingMovePacket(int moveIndex, boolean pressed) {
        this.moveIndex = moveIndex;
        this.pressed = pressed;
    }

    public BreathingMovePacket(FriendlyByteBuf buf) {
        this.moveIndex = buf.readInt();
        this.pressed = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(moveIndex);
        buf.writeBoolean(pressed);
    }

    public void handle(ServerPlayer player) {
        // Get player's moveset
        AbstractMoveset moveset = MovesetHelper.getMoveset(player);
        if (moveset == null) {
            player.displayClientMessage(
                    Component.literal("You need a breathing style to use moves!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            return;
        }

        // Check if move index is valid
        if (moveIndex < 0 || moveIndex >= moveset.getMoveCount()) {
            player.displayClientMessage(
                    Component.literal("Invalid move selection!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            return;
        }

        // Execute the move if pressed
        if (pressed) {
            AbstractMoveset.MoveConfiguration config = moveset.getMove(moveIndex);
            if (config != null) {
                // FIRST: Block inputs in MultiplayerInputHandler to prevent race conditions
                MultiplayerInputHandler.blockInputsAfterBreathingMove(player);

                // THEN: Execute the breathing move
                moveset.performMove(player, moveIndex);

                // Also block katana inputs (redundant but safe)
                KatanaInputHandler.blockAfterBreathingMove(player);
            }
        }
    }
}