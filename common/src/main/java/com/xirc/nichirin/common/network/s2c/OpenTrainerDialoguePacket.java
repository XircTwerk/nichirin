package com.xirc.nichirin.common.network.s2c;

import com.xirc.nichirin.client.gui.trainer.TrainerDialogueScreen;
import com.xirc.nichirin.common.entity.npc.TrainerType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;
import net.minecraft.client.Minecraft;

/**
 * S2C â€” tells the client to open the trainer dialogue screen.
 */
public class OpenTrainerDialoguePacket {

    public enum DialogueState {
        /** Player hasn't brought the prerequisite items yet. */
        STRANGER,
        /** Player has the prerequisite items â€” offer to start the duel. */
        PREREQ_MET,
        /** Player already has the breathing style â€” offer a practice spar. */
        STUDENT,
        /** Trainer recently dueled â€” resting. */
        DUEL_COOLDOWN
    }

    public final UUID          trainerUUID;
    public final TrainerType   trainerType;
    public final DialogueState state;
    public final boolean       hasBeatenTrainer;

    public OpenTrainerDialoguePacket(UUID trainerUUID, TrainerType trainerType, DialogueState state) {
        this(trainerUUID, trainerType, state, false);
    }

    public OpenTrainerDialoguePacket(UUID trainerUUID, TrainerType trainerType, DialogueState state, boolean hasBeatenTrainer) {
        this.trainerUUID      = trainerUUID;
        this.trainerType      = trainerType;
        this.state            = state;
        this.hasBeatenTrainer = hasBeatenTrainer;
    }

    public OpenTrainerDialoguePacket(FriendlyByteBuf buf) {
        this.trainerUUID      = buf.readUUID();
        this.trainerType      = buf.readEnum(TrainerType.class);
        this.state            = buf.readEnum(DialogueState.class);
        this.hasBeatenTrainer = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(trainerUUID);
        buf.writeEnum(trainerType);
        buf.writeEnum(state);
        buf.writeBoolean(hasBeatenTrainer);
    }

    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.setScreen(
                new TrainerDialogueScreen(
                        trainerUUID, trainerType, state, hasBeatenTrainer)));
    }
}
