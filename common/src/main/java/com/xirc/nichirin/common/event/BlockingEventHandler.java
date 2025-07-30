package com.xirc.nichirin.common.event;

import com.xirc.nichirin.common.system.StanceManager;
import com.xirc.nichirin.common.system.blocking.KatanaBlock;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Event handler for blocking system and stance management
 * Damage reduction is handled by vanilla Resistance IV effect
 */
public class BlockingEventHandler {

    /**
     * Registers all blocking and stance-related events
     */
    public static void register() {
        // Server-side player tick for stance and blocking
        TickEvent.PLAYER_POST.register(player -> {
            if (!player.level().isClientSide) { // SERVER SIDE ONLY
                // Tick stance system
                StanceManager.tick(player);

                // Tick blocking system
                KatanaBlock.tick(player);
            }
        });

        // Initial sync when player joins
        PlayerEvent.PLAYER_JOIN.register(player -> {
            if (!player.level().isClientSide) {
                // Force initialize stance if needed
                float currentStance = StanceManager.getStance(player);
                float maxStance = StanceManager.getMaxStance(player);

                // If stance is 0, restore it to full
                if (currentStance <= 0) {
                    StanceManager.restoreFull(player);
                }

                // Force sync
                StanceManager.forceSyncToClient(player);
            }
        });

        // Clean up when player disconnects
        PlayerEvent.PLAYER_QUIT.register(player -> {
            StanceManager.cleanupPlayer(player);
            KatanaBlock.cleanupPlayer(player);
        });

        // Restore full stance on respawn
        PlayerEvent.PLAYER_RESPAWN.register((newPlayer, conqueredEnd) -> {
            if (!conqueredEnd) {
                StanceManager.restoreFull(newPlayer);
                // Force sync after respawn
                StanceManager.forceSyncToClient(newPlayer);
            }
        });

        // Handle parry success (perfect damage negation) - WORKS AGAINST ALL DAMAGE SOURCES
        EntityEvent.LIVING_HURT.register((entity, damageSource, amount) -> {
            if (entity instanceof Player player && !player.level().isClientSide) {
                // Check if this player is blocking
                if (KatanaBlock.isBlocking(player)) {
                    // Get the attacking entity (player OR mob)
                    Player playerAttacker = null;
                    net.minecraft.world.entity.LivingEntity attackingEntity = null;

                    System.out.println("DEBUG: Damage source entity: " +
                            (damageSource.getEntity() != null ? damageSource.getEntity().getClass().getSimpleName() : "null"));

                    if (damageSource.getEntity() instanceof Player playerAtk) {
                        playerAttacker = playerAtk;
                        attackingEntity = playerAtk;
                        System.out.println("DEBUG: Found player attacker: " + playerAtk.getName().getString());
                    } else if (damageSource.getEntity() instanceof net.minecraft.world.entity.LivingEntity livingAtk) {
                        attackingEntity = livingAtk;
                        System.out.println("DEBUG: Found mob attacker: " + livingAtk.getType().getDescription().getString());
                    } else {
                        System.out.println("DEBUG: Non-entity damage source: " + damageSource.getMsgId() +
                                ", direct entity: " + (damageSource.getDirectEntity() != null ?
                                damageSource.getDirectEntity().getClass().getSimpleName() : "null"));
                    }

                    System.out.println("DEBUG: Player " + player.getName().getString() + " is blocking, calling handleIncomingDamage");

                    // Handle the damage through blocking system - works for ALL damage sources
                    boolean handled = KatanaBlock.handleIncomingDamage(player, playerAttacker, amount);

                    System.out.println("DEBUG: handleIncomingDamage returned: " + handled);
                    System.out.println("DEBUG: Current blocking stance: " + KatanaBlock.getStance(player));

                    if (handled) {
                        // Check if it was a perfect parry
                        if (KatanaBlock.getStance(player) == KatanaBlock.BlockingStance.PARRY_SUCCESS) {
                            // Perfect parry - cancel ALL damage from ANY source
                            System.out.println("DEBUG: Perfect parry - negating all damage from " + damageSource.getMsgId());

                            // Apply stun to ANY living entity that attacked (including mobs!)
                            if (attackingEntity != null && attackingEntity != player) {
                                System.out.println("DEBUG: Attempting to apply stun and damage to " + attackingEntity.getType().getDescription().getString());

                                MobEffectInstance stunEffect = new MobEffectInstance(
                                        com.xirc.nichirin.registry.NichirinEffectRegistry.STUNNED.get(),
                                        30, // 1.5 seconds (30 ticks)
                                        0, // Amplifier
                                        false, // Ambient
                                        true, // Show particles
                                        true   // Show icon
                                );
                                boolean stunApplied = attackingEntity.addEffect(stunEffect);

                                // Deal parry damage to the attacker (3 hearts = 6.0 damage)
                                float parryDamage = 6.0f;
                                boolean damageDealt = attackingEntity.hurt(player.damageSources().playerAttack(player), parryDamage);

                                System.out.println("DEBUG: Stun applied: " + stunApplied + ", Damage dealt: " + damageDealt +
                                        " (" + parryDamage + " damage) to " +
                                        (attackingEntity instanceof Player p ? p.getName().getString() :
                                                attackingEntity.getType().getDescription().getString()));
                            } else {
                                System.out.println("DEBUG: No valid attacking entity to stun/damage - attackingEntity: " +
                                        (attackingEntity != null ? attackingEntity.getType().getDescription().getString() : "null"));
                            }

                            return EventResult.interruptFalse(); // Completely negate damage
                        }
                        // Regular blocking damage reduction is handled by Resistance IV effect
                        System.out.println("DEBUG: Regular block - damage will be reduced by Resistance IV");
                    }
                }
            }
            return EventResult.pass(); // Allow damage to proceed (potentially reduced by Resistance IV)
        });
    }
}