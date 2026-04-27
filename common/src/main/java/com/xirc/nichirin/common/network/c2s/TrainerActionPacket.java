package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.common.entity.npc.BaseBreathingTrainerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * C2S — player picks a dialogue option on the trainer dialogue screen.
 */
public class TrainerActionPacket {

    public enum Action {
        REQUEST_DUEL
    }

    public final UUID   trainerUUID;
    public final Action action;

    public TrainerActionPacket(UUID trainerUUID, Action action) {
        this.trainerUUID = trainerUUID;
        this.action      = action;
    }

    public TrainerActionPacket(FriendlyByteBuf buf) {
        this.trainerUUID = buf.readUUID();
        this.action      = buf.readEnum(Action.class);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(trainerUUID);
        buf.writeEnum(action);
    }

    public void handle(ServerPlayer player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        Entity entity = serverLevel.getEntity(trainerUUID);
        if (entity instanceof BaseBreathingTrainerEntity trainer && action == Action.REQUEST_DUEL) {
            trainer.startDuel(player);
        }
    }
}
