package com.xirc.nichirin.common.attack.moves.demon.destructive;

import com.xirc.nichirin.common.attack.moveset.demon.DestructiveDeathMoveset;
import com.xirc.nichirin.common.aura.AuraAudience;
import com.xirc.nichirin.common.aura.AuraInstance;
import com.xirc.nichirin.common.aura.AuraManager;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.system.aura.MovesetAuraTicker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side ticker that maintains a passive aura on every player who has Destructive Death
 * equipped as their BDA.
 *
 * <p>Color follows Overdrive (red while on, blue otherwise). Radius follows Compass Needle
 * (bigger while active, smaller when idle). The aura is removed automatically when the player
 * unequips Destructive Death.</p>
 */
public final class DestructiveDeathPlayerAura {

    // Floor radius matches the breathing-aura default so DD doesn't look smaller than other styles.
    private static final float RADIUS_IDLE = 2.0f;
    private static final float RADIUS_COMPASS = 2.8f;
    // Outer-ring multiplier when layered behind a visible breathing aura (matches MovesetAuraTicker).
    private static final float RING_SCALE = 1.6f;
    private static final float JITTER = 2.4f;
    // Max alpha for a bright, vivid presence — the aura system applies its own falloff per pixel
    // so this is the rim opacity, not a flat overlay.
    private static final float ALPHA = 1.0f;

    /** Cached per-player aura state so we only re-publish on actual change. */
    private static final Map<UUID, CachedAuraState> CACHE = new HashMap<>();

    private DestructiveDeathPlayerAura() {}

    /** Called every server tick from the global tick handler. */
    public static void tick(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            update(player);
        }
    }

    private static void update(ServerPlayer player) {
        UUID id = player.getUUID();
        boolean ddEquipped = "destructive_death".equals(MovesetHelper.getDemonMovesetId(player));

        if (!ddEquipped) {
            removeIfPresent(player);
            return;
        }

        long gameTime = player.level().getGameTime();
        // Auto-expire overdrive once the timer runs out and sync the new state to the client.
        DestructiveDeathState.State ddState = DestructiveDeathState.get(id);
        if (ddState.overdriveEnabled && gameTime >= ddState.overdriveExpiryTick) {
            DestructiveDeathState.setOverdrive(player, false);
        }
        boolean overdrive = DestructiveDeathState.isOverdriveEnabled(id, gameTime);
        boolean compass = DestructiveDeathState.isCompassActive(id, gameTime);
        // Same layering rule as the generic demon aura: when a breathing aura is visible (katana
        // in hand), the DD aura becomes the larger outer ring drawn behind it.
        boolean ring = PlayerDataProvider.getMovesetData(player).hasBreathingMoveset()
                && MovesetAuraTicker.isHoldingKatana(player);

        CachedAuraState desired = new CachedAuraState(overdrive, compass, ring);
        CachedAuraState cached = CACHE.get(id);
        if (desired.equals(cached)) return; // no visual change → no packet

        // Remove the old aura before publishing the new one (different params = different visual).
        removeIfPresent(player);

        float radius = (compass ? RADIUS_COMPASS : RADIUS_IDLE) * (ring ? RING_SCALE : 1.0f);
        AuraInstance aura = AuraInstance.builder()
                // Pure blue / pure red — green pegged at 0.0 so the inner-whiten pass can lift it
                // without ever drifting toward cyan/teal. Combined with max alpha this reads as a
                // crisp, vivid Akaza-blue (or Overdrive-red) rim around the player.
                .color(
                        overdrive ? 1.0f : 0.0f,
                        overdrive ? 0.0f  : 0.0f,
                        overdrive ? 0.05f : 1.0f,
                        ALPHA)
                .radius(radius)
                .jitter(JITTER)
                .build();
        AuraManager.addAura(player, aura, AuraAudience.ALL);

        desired.auraId = aura.id();
        CACHE.put(id, desired);
    }

    private static void removeIfPresent(ServerPlayer player) {
        CachedAuraState cached = CACHE.remove(player.getUUID());
        if (cached != null && cached.auraId != null) {
            AuraManager.removeAura(player, cached.auraId);
        }
    }

    /** Called from {@link DestructiveDeathMoveset#cleanupPlayer} on logout / redemption. */
    public static void clear(UUID playerId) {
        CACHE.remove(playerId);
    }

    private static final class CachedAuraState {
        final boolean overdrive;
        final boolean compass;
        final boolean ring;
        UUID auraId;

        CachedAuraState(boolean overdrive, boolean compass, boolean ring) {
            this.overdrive = overdrive;
            this.compass = compass;
            this.ring = ring;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CachedAuraState other
                    && other.overdrive == overdrive
                    && other.compass == compass
                    && other.ring == ring;
        }

        @Override
        public int hashCode() {
            return (overdrive ? 1 : 0) | (compass ? 2 : 0) | (ring ? 4 : 0);
        }
    }
}
