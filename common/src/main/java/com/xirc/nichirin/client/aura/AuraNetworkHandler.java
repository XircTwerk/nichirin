package com.xirc.nichirin.client.aura;

import com.xirc.nichirin.common.aura.AuraInstance;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.UUID;

import static com.xirc.nichirin.registry.NichirinPacketRegistry.AURA_ADD_ID;
import static com.xirc.nichirin.registry.NichirinPacketRegistry.AURA_CLEAR_ID;
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
    }
}
