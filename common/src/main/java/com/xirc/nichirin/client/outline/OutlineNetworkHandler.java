package com.xirc.nichirin.client.outline;

import com.xirc.nichirin.common.outline.OutlineInstance;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.UUID;

import static com.xirc.nichirin.registry.NichirinPacketRegistry.OUTLINE_ADD_ID;
import static com.xirc.nichirin.registry.NichirinPacketRegistry.OUTLINE_CLEAR_ID;
import static com.xirc.nichirin.registry.NichirinPacketRegistry.OUTLINE_REMOVE_ID;

@Environment(EnvType.CLIENT)
public final class OutlineNetworkHandler {

    private OutlineNetworkHandler() {}

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OUTLINE_ADD_ID, (buf, context) -> {
            UUID host = buf.readUUID();
            OutlineInstance instance = OutlineInstance.read(buf);
            context.queue(() -> OutlineTracker.addOutline(host, instance));
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OUTLINE_REMOVE_ID, (buf, context) -> {
            UUID host = buf.readUUID();
            UUID id = buf.readUUID();
            context.queue(() -> OutlineTracker.removeOutline(host, id));
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OUTLINE_CLEAR_ID, (buf, context) -> {
            UUID host = buf.readUUID();
            context.queue(() -> OutlineTracker.clearOutlines(host));
        });
    }
}
