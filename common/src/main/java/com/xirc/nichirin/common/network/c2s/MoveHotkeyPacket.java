package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.registry.NichirinMoveRegistry;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Packet sent from client to server when a move hotkey is pressed
 * Executes the corresponding move from the player's current moveset
 * Always has 12 hotkeys available regardless of breathing style
 */
public class MoveHotkeyPacket {

    private final int moveIndex;

    public MoveHotkeyPacket(int moveIndex) {
        this.moveIndex = moveIndex;
    }

    public MoveHotkeyPacket(FriendlyByteBuf buf) {
        this.moveIndex = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(moveIndex);
    }

    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                handleMoveHotkey(serverPlayer, moveIndex);
            }
        });
    }

    /**
     * Handle move hotkey execution on server side
     * If no breathing style equipped or invalid index, does nothing
     */
    private static void handleMoveHotkey(ServerPlayer player, int moveIndex) {
        // Get current breathing style - if none, do nothing
        String currentBreathingStyle = BreathingStyleHelper.getMovesetId(player);
        if (currentBreathingStyle == null || currentBreathingStyle.isEmpty()) {
            return; // No breathing style equipped - hotkey does nothing
        }

        // Get the moveset - if none found, do nothing
        AbstractMoveset moveset = NichirinMoveRegistry.getMoveset(currentBreathingStyle);
        if (moveset == null) {
            return; // Invalid breathing style - hotkey does nothing
        }

        // If move index is out of bounds for this moveset, do nothing
        if (moveIndex < 0 || moveIndex >= moveset.getMoveCount()) {
            return; // Invalid move index for this breathing style - hotkey does nothing
        }

        // If player can't perform moves (stunned, etc.), do nothing
        if (!moveset.canPerformMoves(player)) {
            return; // Can't perform moves - hotkey does nothing
        }

        // Execute the move
        moveset.performMove(player, moveIndex);
    }
}