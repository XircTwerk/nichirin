package com.xirc.nichirin.common.event.system;

import com.xirc.nichirin.common.system.perks.PerkManager;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Wires all combat-related perk effects:
 * glass_cannon, juggernaut, iron_will, killer_instinct, slayers_resolve,
 * adrenaline_rush, moonlit_fury (kill stacks), bloodthirst (kill heal),
 * unbreakable (fatal blow prevention), vermilion_soul (death revive).
 */
public class PerkDamageHandler {

    /** Guards against recursive re-entry when we re-apply modified damage. */
    private static final Set<UUID> inPerkDamageCalc = new HashSet<>();

    public static void register() {
        EntityEvent.LIVING_HURT.register(PerkDamageHandler::onEntityHurt);
        EntityEvent.LIVING_DEATH.register(PerkDamageHandler::onEntityDeath);
    }

    private static EventResult onEntityHurt(LivingEntity entity, DamageSource source, float amount) {
        if (entity.level().isClientSide) return EventResult.pass();

        if (source.getEntity() instanceof ServerPlayer attacker
                && !inPerkDamageCalc.contains(attacker.getUUID())) {

            float targetHpFrac = entity.getHealth() / entity.getMaxHealth();

            float outMult = PerkManager.getDamageMultiplier(attacker);
            outMult *= PerkManager.getKillerInstinctMultiplier(attacker, targetHpFrac);
            float selfHpFrac = attacker.getHealth() / attacker.getMaxHealth();
            outMult *= PerkManager.getSlayersResolveMultiplier(attacker, selfHpFrac);
            outMult *= PerkManager.getAdrenalineRushMultiplier(attacker, selfHpFrac);
            outMult *= PerkManager.getMoonlitFuryDamageBonus(attacker);

            if (outMult != 1.0f) {
                inPerkDamageCalc.add(attacker.getUUID());
                entity.hurt(source, amount * outMult);
                inPerkDamageCalc.remove(attacker.getUUID());
                return EventResult.interruptFalse();
            }
        }

        if (entity instanceof ServerPlayer defender
                && !inPerkDamageCalc.contains(defender.getUUID())) {

            float selfHpFrac = defender.getHealth() / defender.getMaxHealth();

            float inMult = PerkManager.getIncomingDamageMultiplier(defender);
            inMult *= PerkManager.getIronWillMultiplier(defender, selfHpFrac);

            // Unbreakable: prevent fatal hit
            boolean wouldKill = defender.getHealth() - amount * inMult <= 0f;
            if (wouldKill && PerkManager.tryConsumeUnbreakable(defender, defender.level().getGameTime())) {
                defender.setHealth(1.0f);
                return EventResult.interruptFalse();
            }

            if (inMult != 1.0f) {
                inPerkDamageCalc.add(defender.getUUID());
                defender.hurt(source, amount * inMult);
                inPerkDamageCalc.remove(defender.getUUID());
                return EventResult.interruptFalse();
            }
        }

        return EventResult.pass();
    }

    private static EventResult onEntityDeath(LivingEntity entity, DamageSource source) {
        if (entity.level().isClientSide) return EventResult.pass();

        if (source.getEntity() instanceof ServerPlayer attacker) {
            PerkManager.applyBloodthirst(attacker);
            PerkManager.addMoonlitFuryStack(attacker);
        }

        if (entity instanceof ServerPlayer defender) {
            long gameTime = defender.level().getGameTime();
            if (PerkManager.tryConsumeVermilionRevive(defender, gameTime)) {
                return EventResult.interruptFalse();
            }
        }

        return EventResult.pass();
    }
}
