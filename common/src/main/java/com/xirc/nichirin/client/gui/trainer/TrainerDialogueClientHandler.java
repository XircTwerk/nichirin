package com.xirc.nichirin.client.gui.trainer;

import com.xirc.nichirin.common.network.s2c.OpenTrainerDialoguePacket;
import net.minecraft.client.Minecraft;

public final class TrainerDialogueClientHandler {

    private TrainerDialogueClientHandler() {
    }

    public static void open(OpenTrainerDialoguePacket packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new TrainerDialogueScreen(
                packet.trainerUUID,
                packet.trainerType,
                packet.state,
                packet.hasBeatenTrainer));
    }
}
