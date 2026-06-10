package com.xirc.nichirin.common.system.aura;

import com.xirc.nichirin.common.aura.AuraAudience;
import com.xirc.nichirin.common.aura.AuraInstance;
import com.xirc.nichirin.common.aura.AuraManager;
import com.xirc.nichirin.common.data.MovesetData;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side ticker that maintains a passive aura on every player based on their currently
 * equipped moveset (breathing style or BDA). CQC has no aura — auras are reserved for breathing
 * and demon-art content, matching the in-universe convention.
 *
 * <p>Destructive Death is handled separately by {@code DestructiveDeathPlayerAura} since its colour
 * and radius shift dynamically with Overdrive + Compass Needle state. Every other moveset gets a
 * fixed colour + radius from {@link MovesetAuraPalette}.</p>
 *
 * <p>Per-player cache so the aura is only re-published when the equipped moveset changes — keeps
 * network traffic flat across the steady state.</p>
 */
public final class MovesetAuraTicker {

    private static final Map<UUID, CachedAuraState> CACHE = new HashMap<>();

    private MovesetAuraTicker() {}

    public static void tick(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            update(player);
        }
    }

    private static void update(ServerPlayer player) {
        MovesetData data = PlayerDataProvider.getMovesetData(player);
        // Priority: breathing > BDA (demon moveset) > none. CQC is intentionally not represented —
        // its "fighting" slot doesn't trigger an aura. Breathing wins when both are set so the
        // visible aura matches the style the player thinks they're using.
        String movesetId = data.hasBreathingMoveset() ? data.getBreathingMovesetId()
                : data.hasDemonMoveset() ? data.getDemonMovesetId()
                : null;

        // Destructive Death has its own dynamic-state ticker. Don't double-publish.
        if (movesetId != null && MovesetAuraPalette.SKIP_IDS.contains(movesetId)) {
            removeIfPresent(player);
            return;
        }

        MovesetAuraPalette.Entry palette = MovesetAuraPalette.get(movesetId);
        if (palette == null) {
            removeIfPresent(player);
            return;
        }

        CachedAuraState cached = CACHE.get(player.getUUID());
        if (cached != null && cached.movesetId.equals(movesetId)) return; // no change

        removeIfPresent(player);

        AuraInstance aura = AuraInstance.builder()
                .color(palette.r(), palette.g(), palette.b(), palette.alpha())
                .radius(palette.radius())
                .jitter(palette.jitter())
                .build();
        AuraManager.addAura(player, aura, AuraAudience.ALL);

        CACHE.put(player.getUUID(), new CachedAuraState(movesetId, aura.id()));
    }

    private static void removeIfPresent(ServerPlayer player) {
        CachedAuraState cached = CACHE.remove(player.getUUID());
        if (cached != null && cached.auraId != null) {
            AuraManager.removeAura(player, cached.auraId);
        }
    }

    public static void clear(UUID playerId) {
        CACHE.remove(playerId);
    }

    private record CachedAuraState(String movesetId, UUID auraId) {}
}
