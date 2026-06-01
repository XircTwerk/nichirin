package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.common.entity.npc.BaseBreathingTrainerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

/**
 * C2S — player picks a dialogue option on the trainer dialogue screen.
 */
public class TrainerActionPacket {

    public enum Action {
        REQUEST_DUEL
    }

    public final UUID   trainerUUID;
    public final Action action;
    public final BaseBreathingTrainerEntity.DuelDifficulty difficulty;

    public TrainerActionPacket(UUID trainerUUID, Action action) {
        this(trainerUUID, action, BaseBreathingTrainerEntity.DuelDifficulty.EASY);
    }

    public TrainerActionPacket(UUID trainerUUID, Action action, BaseBreathingTrainerEntity.DuelDifficulty difficulty) {
        this.trainerUUID = trainerUUID;
        this.action      = action;
        this.difficulty  = difficulty != null ? difficulty : BaseBreathingTrainerEntity.DuelDifficulty.EASY;
    }

    public TrainerActionPacket(FriendlyByteBuf buf) {
        this.trainerUUID = buf.readUUID();
        this.action      = buf.readEnum(Action.class);
        if (this.action == Action.REQUEST_DUEL) {
            this.difficulty = buf.readEnum(BaseBreathingTrainerEntity.DuelDifficulty.class);
        } else {
            this.difficulty = BaseBreathingTrainerEntity.DuelDifficulty.EASY;
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(trainerUUID);
        buf.writeEnum(action);
        if (action == Action.REQUEST_DUEL) {
            buf.writeEnum(difficulty);
        }
    }

    public void handle(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Entity entity = serverLevel.getEntity(trainerUUID);
        if (entity instanceof BaseBreathingTrainerEntity trainer && action == Action.REQUEST_DUEL) {
            trainer.startDuel(player, difficulty);
        }
    }
}
