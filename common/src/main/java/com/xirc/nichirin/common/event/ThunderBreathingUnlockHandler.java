package com.xirc.nichirin.common.event;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.advancement.NichirinCriteriaTriggers;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Handles unlocking Thunder Breathing when struck by lightning
 */
public class ThunderBreathingUnlockHandler {

    public static void register() {
        // Register entity hurt event to detect lightning damage
        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            // Check if damage is from lightning and entity is a player
            if ((source.getDirectEntity() instanceof LightningBolt ||
                    source.getEntity() instanceof LightningBolt ||
                    source.getMsgId().equals("lightningBolt"))
                    && entity instanceof Player
                    && !entity.level().isClientSide) {

                Player player = (Player) entity;

                // Check if wearing no armor
                if (isWearingNoArmor(player)) {
                    // Unlock immediately when struck
                    if (player instanceof ServerPlayer serverPlayer) {
                        checkThunderBreathingUnlock(serverPlayer);
                    }
                }
            }

            return dev.architectury.event.EventResult.pass(); // Pass the event through
        });
    }

    /**
     * Checks if player should unlock Thunder Breathing
     */
    private static void checkThunderBreathingUnlock(ServerPlayer player) {
        // Check if player already has Thunder Breathing
        String currentStyle = BreathingStyleHelper.getMovesetId(player);
        if ("thunder_breathing".equals(currentStyle)) {
            return; // Already has it
        }

        // Check if player is wearing no armor (double check)
        if (!isWearingNoArmor(player)) {
            return;
        }

        // Unlock Thunder Breathing!
        unlockThunderBreathing(player);
    }

    /**
     * Checks if player has no armor equipped
     */
    private static boolean isWearingNoArmor(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() &&
                player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() &&
                player.getItemBySlot(EquipmentSlot.LEGS).isEmpty() &&
                player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
    }

    /**
     * Unlocks Thunder Breathing for the player
     */
    private static void unlockThunderBreathing(ServerPlayer player) {
        // Set Thunder Breathing as the player's style
        PlayerDataProvider.updateAndSync(player, "thunder_breathing");

        // Send success message
        player.displayClientMessage(
                Component.literal("⚡ You have been baptized by the storm! Thunder Breathing unlocked! ⚡")
                        .withStyle(style -> style.withColor(0xFFFF55).withBold(true)),
                false
        );

        // Play thunder sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

        // Grant advancement
        grantThunderBreathingAdvancement(player);
    }

    /**
     * Grants the Thunder Breathing advancement
     */
    private static void grantThunderBreathingAdvancement(ServerPlayer player) {
        // Trigger the custom advancement
        if (NichirinCriteriaTriggers.THUNDER_BREATHING_TRIGGER != null) {
            NichirinCriteriaTriggers.THUNDER_BREATHING_TRIGGER.trigger(player);
        }
    }
}