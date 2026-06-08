package com.xirc.nichirin.common.aura;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Who is allowed to see a given aura instance.
 *
 *   ALL          — every client tracking the host entity
 *   SELF_ONLY    — only the host entity itself (when it is a player)
 *   PLAYERS(set) — explicit allowlist of player UUIDs
 *
 * Used by the server to filter aura-state packet recipients before sending.
 */
public sealed interface AuraAudience permits AuraAudience.All,
                                             AuraAudience.SelfOnly,
                                             AuraAudience.Players {

    AuraAudience ALL = new All();
    AuraAudience SELF_ONLY = new SelfOnly();

    static AuraAudience players(Set<UUID> uuids) {
        return new Players(Set.copyOf(uuids));
    }

    /** Returns true if this player should receive aura packets for the given host entity. */
    boolean includes(ServerPlayer recipient, Entity host);

    void write(FriendlyByteBuf buf);

    static AuraAudience read(FriendlyByteBuf buf) {
        byte tag = buf.readByte();
        return switch (tag) {
            case 0 -> ALL;
            case 1 -> SELF_ONLY;
            case 2 -> {
                int n = buf.readVarInt();
                Set<UUID> set = new HashSet<>(n);
                for (int i = 0; i < n; i++) set.add(buf.readUUID());
                yield players(set);
            }
            default -> ALL;
        };
    }

    record All() implements AuraAudience {
        @Override public boolean includes(ServerPlayer recipient, Entity host) { return true; }
        @Override public void write(FriendlyByteBuf buf) { buf.writeByte(0); }
    }

    record SelfOnly() implements AuraAudience {
        @Override public boolean includes(ServerPlayer recipient, Entity host) {
            return host != null && recipient.getUUID().equals(host.getUUID());
        }
        @Override public void write(FriendlyByteBuf buf) { buf.writeByte(1); }
    }

    record Players(Set<UUID> uuids) implements AuraAudience {
        @Override public boolean includes(ServerPlayer recipient, Entity host) {
            return uuids.contains(recipient.getUUID());
        }
        @Override public void write(FriendlyByteBuf buf) {
            buf.writeByte(2);
            buf.writeVarInt(uuids.size());
            for (UUID u : uuids) buf.writeUUID(u);
        }
    }

    /** Filter a stream of players down to recipients allowed by this audience. */
    default Stream<ServerPlayer> filter(Stream<ServerPlayer> players, Entity host) {
        return players.filter(p -> includes(p, host));
    }
}
