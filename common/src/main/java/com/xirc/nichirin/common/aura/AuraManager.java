package com.xirc.nichirin.common.aura;

import com.xirc.nichirin.common.network.s2c.AddAuraPacket;
import com.xirc.nichirin.common.network.s2c.ClearAurasPacket;
import com.xirc.nichirin.common.network.s2c.RemoveAuraPacket;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
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
 * Server-side registry of all live aura instances, keyed by host entity UUID.
 *
 * On the server, mods/commands call addAura/removeAura/clearAuras. Each call sends
 * the packet to recipients filtered by the aura's {@link AuraAudience}. The audience
 * is NOT serialised to the client — the server is the only place that decides who
 * sees what.
 *
 * Entity-tracker change handling: when a player starts tracking a host, the server
 * pushes any audience-permitted auras to them via {@link #syncAurasToNewTracker}.
 */
public final class AuraManager {
    private record Stored(AuraInstance instance, AuraAudience audience) {}

    private static final Map<UUID, List<Stored>> ENTITY_AURAS = new ConcurrentHashMap<>();

    private AuraManager() {}

    public static void addAura(Entity host, AuraInstance instance, AuraAudience audience) {
        if (host == null || host.level().isClientSide) return;
        ENTITY_AURAS.computeIfAbsent(host.getUUID(), k -> new ArrayList<>()).add(new Stored(instance, audience));
        sendAdd(host, instance, audience);
    }

    public static void removeAura(Entity host, UUID instanceId) {
        if (host == null || host.level().isClientSide) return;
        List<Stored> list = ENTITY_AURAS.get(host.getUUID());
        if (list == null) return;
        boolean removed = list.removeIf(s -> s.instance.id().equals(instanceId));
        if (list.isEmpty()) ENTITY_AURAS.remove(host.getUUID());
        if (removed) sendRemove(host, instanceId);
    }

    public static void clearAuras(Entity host) {
        if (host == null || host.level().isClientSide) return;
        List<Stored> list = ENTITY_AURAS.remove(host.getUUID());
        if (list == null || list.isEmpty()) return;
        sendClear(host);
    }

    public static List<AuraInstance> getAuras(UUID entityId) {
        List<Stored> list = ENTITY_AURAS.get(entityId);
        if (list == null) return List.of();
        List<AuraInstance> out = new ArrayList<>(list.size());
        for (Stored s : list) out.add(s.instance);
        return out;
    }

    /** Push every audience-permitted aura on this host to the given new tracker. */
    public static void syncAurasToNewTracker(Entity host, ServerPlayer newTracker) {
        List<Stored> list = ENTITY_AURAS.get(host.getUUID());
        if (list == null) return;
        for (Stored s : list) {
            if (s.audience.includes(newTracker, host)) {
                sendAddTo(newTracker, host, s.instance);
            }
        }
    }

    public static void onEntityRemoved(UUID entityId) {
        ENTITY_AURAS.remove(entityId);
    }

    public static void clearAll() {
        ENTITY_AURAS.clear();
    }

    private static void sendAdd(Entity host, AuraInstance instance, AuraAudience audience) {
        if (!(host.level() instanceof ServerLevel level)) return;
        FriendlyByteBuf buf = encodeAdd(host, instance);
        ResourceLocation id = NichirinPacketRegistry.AURA_ADD_ID;
        audience.filter(level.getServer().getPlayerList().getPlayers().stream(), host)
                .forEach(p -> safeSend(p, id, copyBuf(buf, p)));
        buf.release();
    }

    private static void sendAddTo(ServerPlayer recipient, Entity host, AuraInstance instance) {
        FriendlyByteBuf buf = encodeAdd(host, instance);
        safeSend(recipient, NichirinPacketRegistry.AURA_ADD_ID, copyBuf(buf, recipient));
        buf.release();
    }

    private static FriendlyByteBuf encodeAdd(Entity host, AuraInstance instance) {
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
        ResourceLocation id = NichirinPacketRegistry.AURA_REMOVE_ID;
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            safeSend(p, id, copyBuf(buf, p));
        }
        buf.release();
    }

    private static void sendClear(Entity host) {
        if (!(host.level() instanceof ServerLevel level)) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUUID(host.getUUID());
        ResourceLocation id = NichirinPacketRegistry.AURA_CLEAR_ID;
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            safeSend(p, id, copyBuf(buf, p));
        }
        buf.release();
    }

    private static net.minecraft.network.RegistryFriendlyByteBuf copyBuf(FriendlyByteBuf src, ServerPlayer recipient) {
        // Each NetworkManager.sendToPlayer consumes the buffer; clone before each send.
        // Architectury 1.7+ requires RegistryFriendlyByteBuf for sendToPlayer.
        return com.xirc.nichirin.common.util.NetworkBufferUtils.serverCopy(src, recipient);
    }

    private static void safeSend(ServerPlayer p, ResourceLocation id, net.minecraft.network.RegistryFriendlyByteBuf buf) {
        try {
            NetworkManager.sendToPlayer(p, id, buf);
        } catch (Exception ignored) {}
    }

    // Convenience wrappers so AddAuraPacket etc. can be empty marker classes if you prefer.
    public static void noopReferences() {
        Class<?> a = AddAuraPacket.class;
        Class<?> r = RemoveAuraPacket.class;
        Class<?> c = ClearAurasPacket.class;
    }
}
