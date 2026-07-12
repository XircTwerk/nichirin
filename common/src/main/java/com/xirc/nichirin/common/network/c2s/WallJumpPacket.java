package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.common.system.abilities.PlayerWallJump;
import com.xirc.nichirin.common.util.StaminaManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client request to wall jump. Carries no payload — the server re-derives the wall side from the
 * player's collision box, so the bounce direction can't be spoofed.
 */
public class WallJumpPacket {

    public WallJumpPacket() {
    }

    public WallJumpPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public void handle(ServerPlayer player) {
        if (PlayerWallJump.tryWallJump(player)) {
            StaminaManager.forceSyncToClient(player);
        }
    }
}
