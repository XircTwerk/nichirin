package com.xirc.nichirin.client.util;

import com.xirc.nichirin.common.util.NetworkBufferUtils;
import com.xirc.nichirin.client.gui.CompassNeedleHUD;
import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import com.xirc.nichirin.registry.NichirinKeybindRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/** Watches the wheel-selection mouse button and releases a charged Compass when LMB is released. */
public final class CompassNeedleInputHandler {
    private static boolean registered;
    private static boolean charging;
    private static boolean mouseSource;
    private static KeyMapping hotkeySource;
    private static Vec3 chargeAnchor;

    private CompassNeedleInputHandler() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            if (!charging) return;
            boolean activationHeld = mouseSource
                    ? minecraft.options.keyAttack.isDown()
                    : hotkeySource != null && hotkeySource.isDown();
            boolean movementAttempt = minecraft.options.keyUp.isDown()
                    || minecraft.options.keyDown.isDown()
                    || minecraft.options.keyLeft.isDown()
                    || minecraft.options.keyRight.isDown()
                    || minecraft.options.keyJump.isDown()
                    || minecraft.options.keyShift.isDown();
            if (minecraft.player != null && chargeAnchor != null) {
                minecraft.player.setPos(chargeAnchor.x, chargeAnchor.y, chargeAnchor.z);
                minecraft.player.setDeltaMovement(Vec3.ZERO);
            }
            if (minecraft.player == null || minecraft.screen != null
                    || !activationHeld || movementAttempt) release();
        });
    }

    public static void beginCharge(int moveIndex) {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (CompassNeedleHUD.isActive()) {
            return;
        }
        if (CooldownHUD.isOnCooldown("Compass Needle")) return;
        charging = true;
        mouseSource = minecraft.options.keyAttack.isDown();
        hotkeySource = mouseSource ? null : NichirinKeybindRegistry.getMoveHotkey(moveIndex);
        chargeAnchor = minecraft.player != null ? minecraft.player.position() : null;
    }

    private static void release() {
        charging = false;
        mouseSource = false;
        hotkeySource = null;
        chargeAnchor = null;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        NetworkManager.sendToServer(NichirinPacketRegistry.COMPASS_NEEDLE_RELEASE_ID,
                NetworkBufferUtils.client(buf));
    }
}
