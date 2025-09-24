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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles demon-specific food restrictions and blood mechanics
 */
public class DemonFoodHandler {

    private static final Map<UUID, Float> accumulatedDamage = new HashMap<>();
    private static final Map<UUID, Integer> halfBloodPoints = new HashMap<>();
    private static final float DAMAGE_PER_HALF_BLOOD = 10.0f;

    public static void register() {
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> {
            if (player == null || hand != InteractionHand.MAIN_HAND) {
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
     * Tracks damage accumulation for blood loss calculation
     * Every 10 damage (5 hearts) = 0.5 blood points lost
     */
    private static void trackDamageForBloodLoss(Player player, float damage) {
        UUID playerUUID = player.getUUID();

        float accumulated = accumulatedDamage.getOrDefault(playerUUID, 0.0f);

        if (accumulated > 1000.0f) {
            accumulated = 0.0f;
        }

        accumulated += damage;

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
     * Sync half-blood points to client for GUI display
     */
    private static void syncHalfBloodToClient(Player player, int halfBloodPoints) {
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
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
     * Sets half blood points directly (for loading from NBT)
     */
    public static void setHalfBloodPointsDirectly(Player player, int halfBloodPointsValue) {
        halfBloodPoints.put(player.getUUID(), Math.max(0, Math.min(halfBloodPointsValue, 1)));
    }

    /**
     * Clean up damage tracking when player disconnects
     */
    public static void cleanupPlayer(Player player) {
        accumulatedDamage.remove(player.getUUID());
        halfBloodPoints.remove(player.getUUID());
    }
}