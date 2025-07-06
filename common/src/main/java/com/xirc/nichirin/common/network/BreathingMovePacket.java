package com.xirc.nichirin.common.network;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.util.enums.MoveClass;
import com.xirc.nichirin.common.util.enums.MoveInputType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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

// Update the handle method in your existing BreathingMovePacket:

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

        // Check if moveset has this move
        if (!moveset.hasMove(moveClass)) {
            player.displayClientMessage(
                    Component.literal("This breathing style doesn't have that move!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            return;
        }

        // Get the corresponding input type
        MoveInputType inputType = getInputTypeForMoveClass(moveClass);
        if (inputType == null) {
            BreathOfNichirin.LOGGER.warn("No input type mapping for move class: " + moveClass);
            return;
        }

        // Execute the move if pressed
        if (pressed) {
            moveset.performMove(player, inputType, null);

            // Visual feedback
            player.displayClientMessage(
                    Component.literal("Executing: " + formatMoveName(moveClass.name()))
                            .withStyle(style -> style.withColor(0x55FFFF)),
                    true
            );
        }
    }

    private static MoveInputType getInputTypeForMoveClass(MoveClass moveClass) {
        // Direct mapping
        for (MoveInputType inputType : MoveInputType.values()) {
            if (inputType.getMoveClass() == moveClass) {
                return inputType;
            }
        }
        return null;
    }

    private static String formatMoveName(String name) {
        String formatted = name.replace("_", " ").toLowerCase();
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : formatted.toCharArray()) {
            if (capitalizeNext && Character.isLetter(c)) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
                if (c == ' ') {
                    capitalizeNext = true;
                }
            }
        }

        return result.toString().replace("Special", "Form");
    }
}