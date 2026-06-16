package com.xirc.nichirin.common.outline;

import com.xirc.nichirin.common.aura.AuraAudience;
import com.xirc.nichirin.common.util.NetworkBufferUtils;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side registry of outline instances per host entity. Mirror of AuraManager for
 * the outline system. Audience filtering reuses {@link AuraAudience}.
 */
public final class OutlineManager {
    private record Stored(OutlineInstance instance, AuraAudience audience) {}

    private static final Map<UUID, List<Stored>> ENTITY_OUTLINES = new ConcurrentHashMap<>();

    private OutlineManager() {}

    public static void addOutline(Entity host, OutlineInstance instance, AuraAudience audience) {
        if (host == null || host.level().isClientSide) return;
        ENTITY_OUTLINES.computeIfAbsent(host.getUUID(), k -> new ArrayList<>()).add(new Stored(instance, audience));
        sendAdd(host, instance, audience);
    }

    public static void removeOutline(Entity host, UUID instanceId) {
        if (host == null || host.level().isClientSide) return;
        List<Stored> list = ENTITY_OUTLINES.get(host.getUUID());
        if (list == null) return;
        boolean removed = list.removeIf(s -> s.instance.id().equals(instanceId));
        if (list.isEmpty()) ENTITY_OUTLINES.remove(host.getUUID());
        if (removed) sendRemove(host, instanceId);
    }

    public static void clearOutlines(Entity host) {
        if (host == null || host.level().isClientSide) return;
        List<Stored> list = ENTITY_OUTLINES.remove(host.getUUID());
        if (list == null || list.isEmpty()) return;
        sendClear(host);
    }

    public static void onEntityRemoved(UUID entityId) {
        ENTITY_OUTLINES.remove(entityId);
    }

    public static void clearAll() {
        ENTITY_OUTLINES.clear();
    }

    private static void sendAdd(Entity host, OutlineInstance instance, AuraAudience audience) {
        if (!(host.level() instanceof ServerLevel level)) return;
        FriendlyByteBuf buf = encodeAdd(host, instance);
        ResourceLocation id = NichirinPacketRegistry.OUTLINE_ADD_ID;
        audience.filter(level.getServer().getPlayerList().getPlayers().stream(), host)
                .forEach(p -> safeSend(p, id, copyBuf(buf, p)));
        buf.release();
    }

    private static FriendlyByteBuf encodeAdd(Entity host, OutlineInstance instance) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUUID(host.getUUID());
        instance.write(buf);
        return buf;
    }

    private static void sendRemove(Entity host, UUID instanceId) {
        if (!(host.level() instanceof ServerLevel level)) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUUID(host.getUUID());
        buf.writeUUID(instanceId);
        ResourceLocation id = NichirinPacketRegistry.OUTLINE_REMOVE_ID;
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            safeSend(p, id, copyBuf(buf, p));
        }
        buf.release();
    }

    private static void sendClear(Entity host) {
        if (!(host.level() instanceof ServerLevel level)) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUUID(host.getUUID());
        ResourceLocation id = NichirinPacketRegistry.OUTLINE_CLEAR_ID;
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            safeSend(p, id, copyBuf(buf, p));
        }
        buf.release();
    }

    private static RegistryFriendlyByteBuf copyBuf(FriendlyByteBuf src, ServerPlayer recipient) {
        return NetworkBufferUtils.serverCopy(src, recipient);
    }

    private static void safeSend(ServerPlayer p, ResourceLocation id, RegistryFriendlyByteBuf buf) {
        try { NetworkManager.sendToPlayer(p, id, buf); } catch (Exception ignored) {}
    }
}
