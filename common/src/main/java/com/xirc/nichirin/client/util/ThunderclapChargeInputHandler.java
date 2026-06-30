package com.xirc.nichirin.client.util;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.item.katana.Katana;
import com.xirc.nichirin.common.util.NetworkBufferUtils;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

/**
 * Per-tick poll for the hold-to-charge Thunderclap input.
 *
 * <p>The charge is started by the existing block + LMB special path (see {@link ClientInputHandler}).
 * Once a Thunder Breathing user is holding RMB+LMB on a katana, this handler watches for the
 * transition out of that state — releasing either button — and sends the C2S release packet so the
 * server-side attack fires the dash.</p>
 */
public final class ThunderclapChargeInputHandler {

    private static final String THUNDER_BREATHING_ID = "thunder_breathing";
    private static boolean registered;
    private static boolean wasChargingLastTick;

    private ThunderclapChargeInputHandler() {}

    public static void register() {
        if (registered) return;
        if (Platform.getEnvironment() != Env.CLIENT) return;
        registered = true;
        ClientTickEvent.CLIENT_POST.register(minecraft -> tick(minecraft));
    }

    private static void tick(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null) {
            wasChargingLastTick = false;
            return;
        }

        boolean canCharge = isThunderBreathingKatanaHolder(player);
        boolean rmbHeld = minecraft.options.keyUse.isDown();
        boolean lmbHeld = minecraft.options.keyAttack.isDown();
        boolean guiOpen = minecraft.screen != null;

        boolean charging = canCharge && rmbHeld && lmbHeld && !guiOpen;

        if (wasChargingLastTick && !charging) {
            sendRelease();
        }
        wasChargingLastTick = charging;
    }

    private static boolean isThunderBreathingKatanaHolder(Player player) {
        if (!(player.getMainHandItem().getItem() instanceof Katana)) return false;
        if (!MovesetHelper.hasBreathingMoveset(player)) return false;
        String id = MovesetHelper.getBreathingMovesetId(player);
        return THUNDER_BREATHING_ID.equals(id);
    }

    private static void sendRelease() {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            NetworkManager.sendToServer(NichirinPacketRegistry.THUNDERCLAP_RELEASE_ID,
                    NetworkBufferUtils.client(buf));
        } catch (Exception ignored) {
        }
    }
}
