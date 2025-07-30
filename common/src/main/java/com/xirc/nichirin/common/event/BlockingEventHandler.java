package com.xirc.nichirin.common.event;

import com.xirc.nichirin.common.system.StanceManager;
import com.xirc.nichirin.common.system.blocking.KatanaBlock;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.world.entity.player.Player;

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

        EntityEvent.LIVING_HURT.register((entity, damageSource, amount) -> {
            if (entity instanceof Player player && !player.level().isClientSide) {
                // Check if this player is blocking
                if (KatanaBlock.isBlocking(player)) {
                    // Get the player attacker (if any) - but parrying works regardless
                    Player attacker = null;
                    if (damageSource.getEntity() instanceof Player playerAttacker) {
                        attacker = playerAttacker;
                        System.out.println("DEBUG: Found player attacker: " + playerAttacker.getName().getString());
                    } else {
                        System.out.println("DEBUG: Non-player damage source: " + damageSource.getMsgId() +
                                " from entity: " + (damageSource.getEntity() != null ? damageSource.getEntity().getType().getDescription().getString() : "null"));
                    }

                    System.out.println("DEBUG: Player " + player.getName().getString() + " is blocking, calling handleIncomingDamage");

                    // Handle the damage through blocking system - works for ALL damage sources
                    boolean handled = KatanaBlock.handleIncomingDamage(player, attacker, amount);

                    System.out.println("DEBUG: handleIncomingDamage returned: " + handled);
                    System.out.println("DEBUG: Current blocking stance: " + KatanaBlock.getStance(player));

                    if (handled) {
                        // Check if it was a perfect parry
                        if (KatanaBlock.getStance(player) == KatanaBlock.BlockingStance.PARRY_SUCCESS) {
                            // Perfect parry - cancel ALL damage from ANY source
                            System.out.println("DEBUG: Perfect parry - negating all damage from " + damageSource.getMsgId());

                            // Double-check that stun was applied to attacker (if it's a player)
                            if (attacker != null) {
                                boolean hasStun = attacker.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.STUNNED.get());
                                System.out.println("DEBUG: Attacker " + attacker.getName().getString() + " has stun effect: " + hasStun);
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