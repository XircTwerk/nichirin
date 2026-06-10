package com.xirc.nichirin.common.attack.moves.demon.destructive;

import com.xirc.nichirin.common.attack.moveset.demon.DestructiveDeathMoveset;
import com.xirc.nichirin.common.aura.AuraAudience;
import com.xirc.nichirin.common.aura.AuraInstance;
import com.xirc.nichirin.common.aura.AuraManager;
import com.xirc.nichirin.common.data.MovesetHelper;
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

    // Floor radius is 1.25 — even with no compass active, the aura is always at least this big.
    private static final float RADIUS_IDLE = 1.25f;
    private static final float RADIUS_COMPASS = 1.9f;
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

        boolean overdrive = DestructiveDeathState.isOverdriveEnabled(id);
        boolean compass = DestructiveDeathState.isCompassActive(id, player.level().getGameTime());

        CachedAuraState desired = new CachedAuraState(overdrive, compass);
        CachedAuraState cached = CACHE.get(id);
        if (desired.equals(cached)) return; // no visual change → no packet

        // Remove the old aura before publishing the new one (different params = different visual).
        removeIfPresent(player);

        AuraInstance aura = AuraInstance.builder()
                // Pure blue / pure red — green pegged at 0.0 so the inner-whiten pass can lift it
                // without ever drifting toward cyan/teal. Combined with max alpha this reads as a
                // crisp, vivid Akaza-blue (or Overdrive-red) rim around the player.
                .color(
                        overdrive ? 1.0f : 0.0f,
                        overdrive ? 0.0f  : 0.0f,
                        overdrive ? 0.05f : 1.0f,
                        ALPHA)
                .radius(compass ? RADIUS_COMPASS : RADIUS_IDLE)
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
        UUID auraId;

        CachedAuraState(boolean overdrive, boolean compass) {
            this.overdrive = overdrive;
            this.compass = compass;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CachedAuraState other
                    && other.overdrive == overdrive
                    && other.compass == compass;
        }

        @Override
        public int hashCode() {
            return (overdrive ? 1 : 0) | (compass ? 2 : 0);
        }
    }
}
