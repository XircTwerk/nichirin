package com.xirc.nichirin.client.aura;

import com.xirc.nichirin.common.aura.AuraInstance;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.UUID;

import static com.xirc.nichirin.registry.NichirinPacketRegistry.AURA_ADD_ID;
import static com.xirc.nichirin.registry.NichirinPacketRegistry.AURA_CLEAR_ID;
import static com.xirc.nichirin.registry.NichirinPacketRegistry.AURA_CLIENT_CTRL_ID;
import static com.xirc.nichirin.registry.NichirinPacketRegistry.AURA_REMOVE_ID;

/**
 * Registers the S2C receivers that drive {@link EntityAuraTracker}.
 * Call once from BreathOfNichirinClient.init().
 */
@Environment(EnvType.CLIENT)
public final class AuraNetworkHandler {

    private AuraNetworkHandler() {}

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, AURA_ADD_ID, (buf, context) -> {
            UUID host = buf.readUUID();
            AuraInstance instance = AuraInstance.read(buf);
            context.queue(() -> EntityAuraTracker.addAura(host, instance));
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, AURA_REMOVE_ID, (buf, context) -> {
            UUID host = buf.readUUID();
            UUID id = buf.readUUID();
            context.queue(() -> EntityAuraTracker.removeAura(host, id));
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, AURA_CLEAR_ID, (buf, context) -> {
            UUID host = buf.readUUID();
            context.queue(() -> EntityAuraTracker.clearAuras(host));
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, AURA_CLIENT_CTRL_ID, (buf, context) -> {
            String action = buf.readUtf();
            String arg = buf.readUtf();
            context.queue(() -> applyCtrl(action, arg));
        });
    }

    private static void applyCtrl(String action, String arg) {
        switch (action) {
            case "debug_toggle" -> AuraDebugState.overlayEnabled = !AuraDebugState.overlayEnabled;
            case "color_reset"  -> AuraDebugState.resetColor();
            case "color" -> {
                String[] p = arg.split(" ");
                if (p.length == 4) {
                    try {
                        AuraDebugState.debugR = Float.parseFloat(p[0]);
                        AuraDebugState.debugG = Float.parseFloat(p[1]);
                        AuraDebugState.debugB = Float.parseFloat(p[2]);
                        AuraDebugState.debugA = Float.parseFloat(p[3]);
                    } catch (NumberFormatException ignored) {}
                }
            }
            case "pixelize" -> AuraConfig.pixelizationEnabled = Boolean.parseBoolean(arg);
            case "set" -> {
                int sp = arg.indexOf(' ');
                if (sp > 0) AuraConfig.setByName(arg.substring(0, sp), arg.substring(sp + 1));
            }
        }
    }
}
