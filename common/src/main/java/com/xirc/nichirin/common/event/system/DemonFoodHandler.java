package com.xirc.nichirin.common.event.system;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.system.DemonManager;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles demon-specific food restrictions and blood mechanics with multihit resistance
 */
public class DemonFoodHandler {

    private static final Map<UUID, Float> accumulatedDamage = new HashMap<>();
    private static final Map<UUID, Integer> halfBloodPoints = new HashMap<>();
    private static final Map<UUID, Long> lastDamageTimes = new HashMap<>();

    private static final float DAMAGE_PER_HALF_BLOOD = 10.0f;
    private static final long MULTIHIT_WINDOW_MS = 1000; // Milliseconds

    public static void register() {
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> {
            if (player == null) {
                return CompoundEventResult.pass();
            }

            // Only enforce the restriction server-side to avoid stale client data
            // blocking food use after demon status is removed
            if (player.level().isClientSide()) {
                return CompoundEventResult.pass();
            }

            if (!MovesetHelper.hasDemonMoveset(player)) {
                return CompoundEventResult.pass();
            }

            if (player.isCreative()) {
                return CompoundEventResult.pass();
            }

            ItemStack itemStack = player.getItemInHand(hand);

            if (itemStack.isEdible()) {
                player.displayClientMessage(
                        Component.literal("Demons cannot consume regular food!")
                                .withStyle(style -> style.withColor(0x8B0000)),
                        true
                );
                return CompoundEventResult.interruptFalse(itemStack);
            }

            return CompoundEventResult.pass();
        });

        EntityEvent.LIVING_HURT.register((entity, damageSource, amount) -> {
            if (!(entity instanceof Player player)) {
                return EventResult.pass();
            }

            if (!MovesetHelper.hasDemonMoveset(player)) {
                return EventResult.pass();
            }

            if (player.isCreative()) {
                return EventResult.pass();
            }

            trackDamageForBloodLoss(player, amount);
            return EventResult.pass();
        });
    }

    /**
     * Tracks damage accumulation for blood loss calculation with multihit resistance
     * Every 10 damage (5 hearts) = 0.5 blood points lost, but multihits are reduced
     */
    private static void trackDamageForBloodLoss(Player player, float damage) {
        UUID playerUUID = player.getUUID();

        // Apply multihit resistance
        float adjustedDamage = applyMultihitResistance(playerUUID, damage);

        float accumulated = accumulatedDamage.getOrDefault(playerUUID, 0.0f);

        if (accumulated > 1000.0f) {
            accumulated = 0.0f;
        }

        accumulated += adjustedDamage;

        if (accumulated > 1000.0f || Float.isInfinite(accumulated) || Float.isNaN(accumulated)) {
            accumulated = 100.0f;
        }

        int safetyCounter = 0;
        while (accumulated >= DAMAGE_PER_HALF_BLOOD && safetyCounter < 10) {
            safetyCounter++;

            int currentBlood = DemonManager.getBloodPoints(player);
            int currentHalfBlood = halfBloodPoints.getOrDefault(playerUUID, 0);

            if (currentBlood > 0 || currentHalfBlood > 0) {
                currentHalfBlood++;
                halfBloodPoints.put(playerUUID, currentHalfBlood);

                if (currentHalfBlood >= 2) {
                    DemonManager.removeBloodPoints(player, 1);
                    halfBloodPoints.put(playerUUID, 0);
                    syncHalfBloodToClient(player, 0);
                } else {
                    syncHalfBloodToClient(player, currentHalfBlood);
                }
            } else {
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
     * Apply multihit resistance - reduce blood loss from rapid consecutive hits
     */
    private static float applyMultihitResistance(UUID playerUUID, float damage) {
        long currentTime = System.currentTimeMillis();
        Long lastDamage = lastDamageTimes.get(playerUUID);

        if (lastDamage != null && (currentTime - lastDamage) < MULTIHIT_WINDOW_MS) {
            // Recent damage within window - apply 50% reduction for blood loss calculation
            lastDamageTimes.put(playerUUID, currentTime);
            return damage * 0.5f;
        } else {
            // First hit or after window - full blood loss
            lastDamageTimes.put(playerUUID, currentTime);
            return damage;
        }
    }

    /**
     * Sync half-blood points to client for GUI display
     */
    private static void syncHalfBloodToClient(Player player, int halfBloodPoints) {
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            int fullBlood = DemonManager.getBloodPoints(player);
            NichirinPacketRegistry.sendDemonSync(serverPlayer, fullBlood, halfBloodPoints, true);
        }
    }

    /**
     * Get half-blood points for a player (for other systems that might need it)
     */
    public static int getHalfBloodPoints(Player player) {
        return halfBloodPoints.getOrDefault(player.getUUID(), 0);
    }

    /**
     * Sets half blood points directly (for loading from NBT)
     */
    public static void setHalfBloodPointsDirectly(Player player, int halfBloodPointsValue) {
        halfBloodPoints.put(player.getUUID(), Math.max(0, Math.min(halfBloodPointsValue, 1)));
    }

    /**
     * Clean up damage tracking when player disconnects
     */
    public static void cleanupPlayer(Player player) {
        UUID playerUUID = player.getUUID();
        accumulatedDamage.remove(playerUUID);
        halfBloodPoints.remove(playerUUID);
        lastDamageTimes.remove(playerUUID);
    }
}