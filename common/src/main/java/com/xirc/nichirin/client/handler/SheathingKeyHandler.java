package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.common.network.c2s.SheathInputPacket;
import com.xirc.nichirin.common.system.sheathing.SheathInputAction;
import com.xirc.nichirin.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;

public class SheathingKeyHandler {
    private static boolean wasDown = false;
    private static int heldTicks = 0;

    public static void register() {
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (client.player == null || client.screen != null) {
                wasDown = false;
                heldTicks = 0;
                return;
            }

            boolean down = NichirinKeybindRegistry.SHEATHE_KEY.isDown();
            boolean shiftDown = client.player.isShiftKeyDown();
            if (down && !wasDown) {
                heldTicks = 0;
                NichirinPacketRegistry.sendToServer(new SheathInputPacket(SheathInputAction.PRESS, shiftDown, heldTicks));
            } else if (!down && wasDown) {
                NichirinPacketRegistry.sendToServer(new SheathInputPacket(SheathInputAction.RELEASE, shiftDown, heldTicks));
                heldTicks = 0;
            } else if (down) {
                heldTicks++;
            }
            wasDown = down;
        });
    }
}
