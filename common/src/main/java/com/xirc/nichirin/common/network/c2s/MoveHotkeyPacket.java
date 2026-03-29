package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.registry.NichirinEffectRegistry;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

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
     * Requires a katana to be held in the main hand
     * Performs the same validation checks as the attack wheel
     */
    private static void handleMoveHotkey(ServerPlayer player, int moveIndex) {
        // Check if holding katana in main hand - REQUIRED
        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof SimpleKatana)) {
            return; // No katana in hand - hotkey does nothing
        }

        if (player.hasEffect(NichirinEffectRegistry.BLOCKING.get())) {
            return;
        }

        if (player.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return;
        }

        // Get current breathing style - if none, use base katana wheel moves
        String currentBreathingStyle = MovesetHelper.getMovesetId(player);
        if (currentBreathingStyle == null || currentBreathingStyle.isEmpty()) {
            // No breathing style — delegate to SimpleKatana wheel moves
            SimpleKatana katana = (SimpleKatana) mainHand.getItem();
            katana.performWheelMove(player, moveIndex);
            return;
        }

        // Use MovesetHelper for consistency with how left/right click resolve the moveset
        AbstractMoveset moveset = MovesetHelper.getBreathingMoveset(player);
        if (moveset == null) {
            return;
        }

        // Bounds check
        if (moveIndex < 0 || moveIndex >= moveset.getMoveCount()) {
            return;
        }

        // Let the moveset handle its own validation (cooldowns, stamina, etc.)
        moveset.performMove(player, moveIndex);
    }
}