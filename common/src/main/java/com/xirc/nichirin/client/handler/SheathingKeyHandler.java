package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.common.network.c2s.SheathInputPacket;
import com.xirc.nichirin.common.system.sheathing.SheathInputAction;
import com.xirc.nichirin.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;

public class SheathingKeyHandler {
    private static boolean wasDown = false;

    public static void register() {
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (client.player == null || client.screen != null) {
                wasDown = false;
                return;
            }

            boolean down = NichirinKeybindRegistry.SHEATHE_KEY.isDown();
            boolean shiftDown = client.player.isShiftKeyDown();
            if (down && !wasDown) {
                NichirinPacketRegistry.sendToServer(new SheathInputPacket(SheathInputAction.PRESS, shiftDown));
            } else if (!down && wasDown) {
                NichirinPacketRegistry.sendToServer(new SheathInputPacket(SheathInputAction.RELEASE, shiftDown));
            }
            wasDown = down;
        });
    }
}
