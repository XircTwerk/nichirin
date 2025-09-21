package com.xirc.nichirin.common.event;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.system.DemonManager;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles demon-specific food restrictions and blood mechanics
 */
public class DemonFoodHandler {

    // Track accumulated damage per player for blood loss calculation
    private static final java.util.Map<java.util.UUID, Float> accumulatedDamage = new java.util.HashMap<>();
    // Track half-blood points separately since DemonManager uses integers
    private static final java.util.Map<java.util.UUID, Integer> halfBloodPoints = new java.util.HashMap<>();
    private static final float DAMAGE_PER_HALF_BLOOD = 10.0f; // 5 hearts = 10 damage = 0.5 blood

    public static void register() {
        // Prevent demons from eating regular food
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> {
            if (player == null || hand != InteractionHand.MAIN_HAND) {
                return CompoundEventResult.pass();
            }

            // Only apply to demons
            if (!MovesetHelper.hasDemonMoveset(player)) {
                return CompoundEventResult.pass();
            }

            // Don't restrict in creative mode
            if (player.isCreative()) {
                return CompoundEventResult.pass();
            }

            ItemStack itemStack = player.getItemInHand(hand);

            // Check if item is edible
            if (itemStack.isEdible()) {
                player.displayClientMessage(
                        Component.literal("Demons cannot consume regular food!")
                                .withStyle(style -> style.withColor(0x8B0000)), // Dark red
                        true
                );
                return CompoundEventResult.interruptFalse(itemStack); // Cancel the food consumption
            }

            return CompoundEventResult.pass();
        });

        // Handle demon taking damage - reduce blood points
        EntityEvent.LIVING_HURT.register((entity, damageSource, amount) -> {
            if (!(entity instanceof Player player)) {
                return EventResult.pass();
            }

            // Only apply to demons
            if (!MovesetHelper.hasDemonMoveset(player)) {
                return EventResult.pass();
            }

            // Don't affect blood in creative mode
            if (player.isCreative()) {
                return EventResult.pass();
            }

            // Track cumulative damage for blood loss calculation
            trackDamageForBloodLoss(player, amount);

            return EventResult.pass();
        });
    }

    /**
     * Tracks damage accumulation for blood loss calculation
     * Every 10 damage (5 hearts) = 0.5 blood points lost
     */
    private static void trackDamageForBloodLoss(Player player, float damage) {
        java.util.UUID playerUUID = player.getUUID();

        // Add to accumulated damage with overflow protection
        float accumulated = accumulatedDamage.getOrDefault(playerUUID, 0.0f);

        // Prevent float overflow that causes infinite loops
        if (accumulated > 1000.0f) {
            accumulated = 0.0f;
        }

        accumulated += damage;

        // Safety check: if accumulated becomes too large, cap it
        if (accumulated > 1000.0f || Float.isInfinite(accumulated) || Float.isNaN(accumulated)) {
            accumulated = 100.0f; // Enough for several blood losses but not infinite
        }

        // Check if we've accumulated enough damage to lose half blood
        int safetyCounter = 0; // Prevent infinite loops
        while (accumulated >= DAMAGE_PER_HALF_BLOOD && safetyCounter < 10) {
            safetyCounter++;

            int currentBlood = DemonManager.getBloodPoints(player);
            int currentHalfBlood = halfBloodPoints.getOrDefault(playerUUID, 0);

            if (currentBlood > 0 || currentHalfBlood > 0) {
                // Increment half-blood counter
                currentHalfBlood++;
                halfBloodPoints.put(playerUUID, currentHalfBlood);

                // Every 2 half-bloods = 1 full blood point lost
                if (currentHalfBlood >= 2) {
                    DemonManager.removeBloodPoints(player, 1);
                    halfBloodPoints.put(playerUUID, 0); // Reset half-blood counter
                    System.out.println("DEBUG: Player " + player.getName().getString() + " lost 1 full blood point. New total: " + DemonManager.getBloodPoints(player));

                    // Sync half-blood data to client (now 0)
                    syncHalfBloodToClient(player, 0);
                } else {
                    System.out.println("DEBUG: Player " + player.getName().getString() + " lost 0.5 blood points. Half-bloods: " + currentHalfBlood);

                    // Sync half-blood data to client
                    syncHalfBloodToClient(player, currentHalfBlood);
                }
            } else {
                // No blood left, break the loop
                break;
            }

            accumulated -= DAMAGE_PER_HALF_BLOOD;
        }

        if (safetyCounter >= 10) {
            accumulated = 0.0f;
        }

        accumulatedDamage.put(playerUUID, accumulated);
    }

    /**
     * Sync half-blood points to client for GUI display
     */
    private static void syncHalfBloodToClient(Player player, int halfBloodPoints) {
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            // Send updated packet with both full blood and half blood
            int fullBlood = DemonManager.getBloodPoints(player);
            com.xirc.nichirin.registry.NichirinPacketRegistry.sendDemonSync(serverPlayer, fullBlood, halfBloodPoints, true);
        }
    }

    /**
     * Get half-blood points for a player (for other systems that might need it)
     */
    public static int getHalfBloodPoints(Player player) {
        return halfBloodPoints.getOrDefault(player.getUUID(), 0);
    }

    /**
     * Clean up damage tracking when player disconnects
     */
    public static void cleanupPlayer(Player player) {
        accumulatedDamage.remove(player.getUUID());
        halfBloodPoints.remove(player.getUUID());
    }
}