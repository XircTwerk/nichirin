package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.util.MultiplayerInputHandler;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Packet for executing demon art moves from the attack wheel.
public class DemonMovePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemonMovePacket.class);

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
        if (player.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return;
        }

        if (!MovesetHelper.hasDemonMoveset(player)) {
            player.displayClientMessage(
                    Component.literal("No demon art equipped!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            LOGGER.debug("DemonMovePacket - Player {} has no demon moveset", player.getName().getString());
            return;
        }

        var moveset = MovesetHelper.getDemonMoveset(player);
        if (moveset == null) {
            player.displayClientMessage(
                    Component.literal("Failed to load demon art!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            LOGGER.debug("DemonMovePacket - Failed to get demon moveset for {}", player.getName().getString());
            return;
        }

        LOGGER.debug("DemonMovePacket - Using demon moveset: {} for move {}", moveset.getMovesetId(), moveIndex);

        if (moveIndex < 0 || moveIndex >= moveset.getMoveCount()) {
            player.displayClientMessage(
                    Component.literal("Invalid demon art move! (Index: " + moveIndex + ", Max: " + (moveset.getMoveCount() - 1) + ")")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            LOGGER.debug("DemonMovePacket - Invalid move index {} for moveset with {} moves", moveIndex, moveset.getMoveCount());
            return;
        }

        var moveConfig = moveset.getMove(moveIndex);
        if (moveConfig == null) {
            player.displayClientMessage(
                    Component.literal("Move configuration not found!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            LOGGER.debug("DemonMovePacket - Move config null for index {}", moveIndex);
            return;
        }

        LOGGER.debug("DemonMovePacket - Executing demon move: {}", moveConfig.getDisplayName());

        try {
            moveset.performMove(player, moveIndex);

            LOGGER.debug("DemonMovePacket - Successfully executed {}", moveConfig.getDisplayName());

        } catch (Exception e) {
            player.displayClientMessage(
                    Component.literal("Failed to execute demon art: " + e.getMessage())
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            LOGGER.error("DemonMovePacket execution failed", e);
        }
    }

    public int getMoveIndex() {
        return moveIndex;
    }

    public boolean isFromWheel() {
        return fromWheel;
    }
}
