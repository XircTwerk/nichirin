package com.xirc.nichirin.common.system.aura;

import com.xirc.nichirin.common.aura.AuraAudience;
import com.xirc.nichirin.common.aura.AuraInstance;
import com.xirc.nichirin.common.aura.AuraManager;
import com.xirc.nichirin.common.data.MovesetData;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.item.katana.BeastKatana;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.item.katana.SoundKatana;
import com.xirc.nichirin.common.outline.OutlineInstance;
import com.xirc.nichirin.common.outline.OutlineManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

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

    // How much bigger the demon aura gets when it rings around a breathing aura.
    private static final float DEMON_RING_SCALE = 1.6f;

    private static void update(ServerPlayer player) {
        MovesetData data = PlayerDataProvider.getMovesetData(player);
        boolean holdingKatana = isHoldingKatana(player);

        // Breathing auras (and their paired outline) only appear while a katana is held.
        // Demon auras show whenever a (non-DD) demon moveset is equipped; when both are visible
        // the demon aura becomes the larger outer ring drawn behind the breathing aura.
        // CQC is intentionally not represented — its "fighting" slot doesn't trigger an aura.
        String breathingId = data.hasBreathingMoveset() ? data.getBreathingMovesetId() : null;
        String demonId = data.hasDemonMoveset() ? data.getDemonMovesetId() : null;
        // Destructive Death has its own dynamic-state ticker. Don't double-publish.
        if (breathingId != null && MovesetAuraPalette.SKIP_IDS.contains(breathingId)) breathingId = null;
        if (demonId != null && MovesetAuraPalette.SKIP_IDS.contains(demonId)) demonId = null;

        MovesetAuraPalette.Entry breathingPalette = (breathingId != null && holdingKatana)
                ? MovesetAuraPalette.get(breathingId) : null;
        MovesetAuraPalette.Entry demonPalette = demonId != null ? MovesetAuraPalette.get(demonId) : null;

        if (breathingPalette == null && demonPalette == null) {
            removeIfPresent(player);
            return;
        }

        String stateKey = (breathingPalette != null ? breathingId : "-")
                + "|" + (demonPalette != null ? demonId : "-");
        CachedAuraState cached = CACHE.get(player.getUUID());
        if (cached != null && cached.stateKey().equals(stateKey)) return; // no change

        removeIfPresent(player);

        UUID breathingAuraId = null;
        UUID demonAuraId = null;
        UUID outlineId = null;

        if (demonPalette != null) {
            // Outer ring when layered under a breathing aura; normal size on its own. The client
            // renderer draws bigger auras first, so this always sits behind the breathing disc.
            float radius = breathingPalette != null
                    ? demonPalette.radius() * DEMON_RING_SCALE
                    : demonPalette.radius();
            AuraInstance demonAura = AuraInstance.builder()
                    .color(demonPalette.r(), demonPalette.g(), demonPalette.b(), demonPalette.alpha())
                    .radius(radius)
                    .jitter(demonPalette.jitter())
                    .build();
            AuraManager.addAura(player, demonAura, AuraAudience.ALL);
            demonAuraId = demonAura.id();
        }

        if (breathingPalette != null) {
            AuraInstance aura = AuraInstance.builder()
                    .color(breathingPalette.r(), breathingPalette.g(), breathingPalette.b(), breathingPalette.alpha())
                    .radius(breathingPalette.radius())
                    .jitter(breathingPalette.jitter())
                    .build();
            AuraManager.addAura(player, aura, AuraAudience.ALL);
            breathingAuraId = aura.id();

            // Route through MC's built-in outline post-shader (clean screen-space edge) rather
            // than the custom cel geometry pass, which front-culls into a flat fill over the whole
            // model on some setups. Tradeoff: the edge shows faintly through walls.
            OutlineInstance outline = OutlineInstance.builder()
                    .color(breathingPalette.r(), breathingPalette.g(), breathingPalette.b(), 0.8f)
                    .thickness(1.04f)
                    .seeThroughWalls(true)
                    .lifetimeTicks(-1)
                    .build();
            OutlineManager.addOutline(player, outline, AuraAudience.ALL);
            outlineId = outline.id();
        }

        CACHE.put(player.getUUID(), new CachedAuraState(stateKey, breathingAuraId, demonAuraId, outlineId));
    }

    public static boolean isHoldingKatana(ServerPlayer player) {
        Item main = player.getMainHandItem().getItem();
        Item off = player.getOffhandItem().getItem();
        return main instanceof SimpleKatana || main instanceof SoundKatana || main instanceof BeastKatana
                || off instanceof SimpleKatana || off instanceof SoundKatana || off instanceof BeastKatana;
    }

    private static void removeIfPresent(ServerPlayer player) {
        CachedAuraState cached = CACHE.remove(player.getUUID());
        if (cached == null) return;
        if (cached.auraId() != null) {
            AuraManager.removeAura(player, cached.auraId());
        }
        if (cached.demonAuraId() != null) {
            AuraManager.removeAura(player, cached.demonAuraId());
        }
        if (cached.outlineId() != null) {
            OutlineManager.removeOutline(player, cached.outlineId());
        }
    }

    public static void clear(UUID playerId) {
        CACHE.remove(playerId);
    }

    private record CachedAuraState(String stateKey, UUID auraId, UUID demonAuraId, UUID outlineId) {}
}
