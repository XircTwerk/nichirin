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
    /**
     * Set after we auto-fire a RELEASE while the player is still physically holding Y, so we
     * don't re-trigger PRESS / RELEASE every tick. Cleared when the player actually releases.
     */
    private static boolean autoFired = false;

    /** Hold longer than this and we auto-fire RELEASE (plain unsheathe, no quickdraw). */
    private static final int AUTO_RELEASE_TICKS = 20;

    public static void register() {
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (client.player == null || client.screen != null) {
                wasDown = false;
                heldTicks = 0;
                autoFired = false;
                return;
            }

            boolean down = NichirinKeybindRegistry.SHEATHE_KEY.isDown();
            boolean shiftDown = client.player.isShiftKeyDown();
            if (down && !wasDown) {
                if (!autoFired) {
                    heldTicks = 0;
                    NichirinPacketRegistry.sendToServer(new SheathInputPacket(SheathInputAction.PRESS, shiftDown, heldTicks));
                }
            } else if (!down && wasDown) {
                if (!autoFired) {
                    NichirinPacketRegistry.sendToServer(new SheathInputPacket(SheathInputAction.RELEASE, shiftDown, heldTicks));
                }
                heldTicks = 0;
                autoFired = false;
            } else if (down && !autoFired) {
                heldTicks++;
                if (heldTicks >= AUTO_RELEASE_TICKS) {
                    // Player held too long — auto-fire RELEASE so the slot doesn't stay
                    // charged forever waiting for them to let go.
                    NichirinPacketRegistry.sendToServer(new SheathInputPacket(SheathInputAction.RELEASE, shiftDown, heldTicks));
                    autoFired = true;
                }
            }
            wasDown = down;
        });
    }
}