package com.xirc.nichirin.common.network;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
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
        AbstractMoveset moveset = BreathingStyleHelper.getMoveset(player);
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
                moveset.performMove(player, moveIndex);

                // Visual feedback
                player.displayClientMessage(
                        Component.literal("Executing: " + config.getDisplayName())
                                .withStyle(style -> style.withColor(0x55FFFF)),
                        true
                );
            }
        }
    }
}