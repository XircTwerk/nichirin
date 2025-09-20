package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * FIXED: Packet for executing demon art moves from the attack wheel
 * Now uses the specific demon moveset methods instead of primary moveset
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

        // FIXED: Use specific demon moveset methods instead of primary moveset
        if (!MovesetHelper.hasDemonMoveset(player)) {
            player.displayClientMessage(
                    Component.literal("No demon art equipped!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            System.out.println("DEBUG: DemonMovePacket - Player " + player.getName().getString() + " has no demon moveset");
            return;
        }

        var moveset = MovesetHelper.getDemonMoveset(player);
        if (moveset == null) {
            player.displayClientMessage(
                    Component.literal("Failed to load demon art!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            System.out.println("DEBUG: DemonMovePacket - Failed to get demon moveset for " + player.getName().getString());
            return;
        }

        System.out.println("DEBUG: DemonMovePacket - Using demon moveset: " + moveset.getMovesetId() + " for move " + moveIndex);

        // Validate move index
        if (moveIndex < 0 || moveIndex >= moveset.getMoveCount()) {
            player.displayClientMessage(
                    Component.literal("Invalid demon art move! (Index: " + moveIndex + ", Max: " + (moveset.getMoveCount() - 1) + ")")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            System.out.println("DEBUG: DemonMovePacket - Invalid move index " + moveIndex + " for moveset with " + moveset.getMoveCount() + " moves");
            return;
        }

        // Get the move configuration
        var moveConfig = moveset.getMove(moveIndex);
        if (moveConfig == null) {
            player.displayClientMessage(
                    Component.literal("Move configuration not found!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            System.out.println("DEBUG: DemonMovePacket - Move config null for index " + moveIndex);
            return;
        }

        System.out.println("DEBUG: DemonMovePacket - Executing demon move: " + moveConfig.getDisplayName());

        try {
            // Execute the demon art move
            moveset.performMove(player, moveIndex);

            // Block inputs after execution (same as breathing moves)
            com.xirc.nichirin.common.util.MultiplayerInputHandler.blockInputsAfterMoveExecution(player);

            System.out.println("DEBUG: DemonMovePacket - Successfully executed " + moveConfig.getDisplayName());

        } catch (Exception e) {
            player.displayClientMessage(
                    Component.literal("Failed to execute demon art: " + e.getMessage())
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            System.err.println("ERROR: DemonMovePacket execution failed:");
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