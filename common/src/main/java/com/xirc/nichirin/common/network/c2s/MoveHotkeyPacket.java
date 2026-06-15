package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.item.gun.GenyaDB;
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

    private static void handleMoveHotkey(ServerPlayer player, int moveIndex) {
        ItemStack mainHand = player.getMainHandItem();

        // Gun (Genya DB) — its two wheel moves (0 = Gun Bash, 1 = Grab) route here.
        if (mainHand.getItem() instanceof GenyaDB gun) {
            if (player.hasEffect(NichirinEffectRegistry.stunned())) return;
            gun.performWheelMove(player, moveIndex);
            return;
        }

        if (!(mainHand.getItem() instanceof SimpleKatana)) return;

        if (player.hasEffect(NichirinEffectRegistry.blocking())) {
            return;
        }

        if (player.hasEffect(NichirinEffectRegistry.stunned())) {
            return;
        }

        // No breathing style — delegate to SimpleKatana wheel moves
        String currentBreathingStyle = MovesetHelper.getBreathingMovesetId(player);
        if (currentBreathingStyle == null || currentBreathingStyle.isEmpty()) {
            SimpleKatana katana = (SimpleKatana) mainHand.getItem();
            katana.performWheelMove(player, moveIndex);
            return;
        }

        AbstractMoveset moveset = MovesetHelper.getBreathingMoveset(player);
        if (moveset == null) {
            return;
        }

        if (moveIndex < 0 || moveIndex >= moveset.getMoveCount()) {
            return;
        }

        moveset.performMove(player, moveIndex);
    }
}