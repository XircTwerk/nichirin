package com.xirc.nichirin.common.event.system;

import com.xirc.nichirin.common.network.s2c.PlayerAnimationPacket;
import com.xirc.nichirin.common.system.blocking.HandToHandBlock;
import com.xirc.nichirin.common.system.blocking.KatanaBlock;
import com.xirc.nichirin.common.util.NichirinDamageSources;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Plays a flinch/recoil animation on a player whenever they take damage. The tier
 * (small / medium / large) scales with how hard the hit was, and it re-triggers on every hit so
 * rapid hits keep snapping the reaction back to the start.
 */
public final class HitAnimationHandler {

    private static final float MEDIUM_THRESHOLD = 4.0f;
    private static final float LARGE_THRESHOLD = 8.0f;

    private HitAnimationHandler() {}

    public static void register() {
        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer player)) return EventResult.pass();
            if (amount <= 0.0f) return EventResult.pass();
            // Only Nichirin attacks should flinch the player — not vanilla mobs, fall damage, etc.
            if (!NichirinDamageSources.isNichirinAttack(source)) return EventResult.pass();
            // Creative (and spectator) players shouldn't react to hits.
            if (player.isCreative() || player.isSpectator()) return EventResult.pass();
            // The guard pose owns the visual while blocking — don't stomp it with a flinch.
            if (KatanaBlock.isBlocking(player) || HandToHandBlock.isBlocking(player)) return EventResult.pass();

            NichirinPacketRegistry.broadcastPlayerAnimation(player,
                    new PlayerAnimationPacket(player.getId(), pickTier(player, amount)));
            return EventResult.pass();
        });
    }

    private static String pickTier(ServerPlayer player, float amount) {
        // Hard control effects always read as a heavy hit regardless of raw damage.
        if (player.hasEffect(NichirinEffectRegistry.stunned())
                || player.hasEffect(NichirinEffectRegistry.slammed())) {
            return "large_hit";
        }
        if (amount >= LARGE_THRESHOLD) return "large_hit";
        if (amount >= MEDIUM_THRESHOLD) return "medium_hit";
        return "small_hit";
    }
}
