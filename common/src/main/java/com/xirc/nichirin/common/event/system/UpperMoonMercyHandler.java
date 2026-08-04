package com.xirc.nichirin.common.event.system;

import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.entity.npc.UpperMoonDemonEntity;
import com.xirc.nichirin.common.system.UpperMoonPact;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Drives the Upper Moon "mercy / recruitment" behavior off {@link EntityEvent#LIVING_HURT}:
 *
 * <ul>
 *   <li>An un-enraged Upper Moon's <em>killing</em> blow on an un-spared human is cancelled — the
 *       demon spares and recruits them instead (see {@link UpperMoonDemonEntity#sparePlayer}).</li>
 *   <li>A player attacking an Upper Moon that already spared them triggers the re-engage line; no
 *       further mercy is given (the pact check below fails), so it becomes a fight to the death.</li>
 * </ul>
 */
public final class UpperMoonMercyHandler {

    private UpperMoonMercyHandler() {}

    public static void register() {
        EntityEvent.LIVING_HURT.register(UpperMoonMercyHandler::onLivingHurt);
        EntityEvent.LIVING_DEATH.register(UpperMoonMercyHandler::onLivingDeath);
    }

    /** When an Upper Moon actually kills a player (mercy off, or enraged), it vanishes afterward. */
    private static EventResult onLivingDeath(LivingEntity entity, DamageSource source) {
        if (entity instanceof ServerPlayer && source.getEntity() instanceof UpperMoonDemonEntity demon) {
            demon.despawnAfterKill();
        }
        return EventResult.pass();
    }

    private static EventResult onLivingHurt(LivingEntity target, DamageSource source, float amount) {
        if (target.level().isClientSide) return EventResult.pass();

        // A player frozen in a standoff is immune to everything until it resolves — this stops
        // lingering shockwaves / in-flight attacks from killing them before they kneel or refuse.
        if (target instanceof ServerPlayer pinned
                && UpperMoonDemonEntity.isStandoffProtected(pinned.getUUID())) {
            return EventResult.interruptFalse();
        }

        Entity src = source.getEntity(); // owner for owned shockwaves, the demon for melee

        // The demon is about to kill a human it hasn't spared — open the mercy standoff instead.
        // With the pact system disabled he just kills you (and then despawns via LIVING_DEATH).
        if (target instanceof ServerPlayer player && src instanceof UpperMoonDemonEntity demon) {
            if (NichirinModConfig.get().demon.upperMoonPact
                    && !demon.isInStandoff()
                    && !demon.isEnraged()
                    && !MovesetHelper.hasDemonMoveset(player)
                    && !UpperMoonPact.isSpared(player, demon.getDemonType())
                    && amount >= player.getHealth()) {
                demon.beginStandoff(player);
                return EventResult.interruptFalse(); // cancel the killing blow
            }
        }

        if (target instanceof UpperMoonDemonEntity demon && src instanceof ServerPlayer player) {
            // Attacking the demon mid-standoff breaks the truce — instant death.
            if (demon.isInStandoffWith(player)) {
                demon.executeStandoff(player);
                return EventResult.interruptFalse(); // he takes no damage from the betrayal
            }
            // A previously-spared player re-engaging — quip once; from here it's a real fight.
            if (UpperMoonPact.isSpared(player, demon.getDemonType())) {
                demon.onReengaged(player);
            }
        }

        return EventResult.pass();
    }
}
