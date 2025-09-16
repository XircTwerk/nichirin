package com.xirc.nichirin.common.event;

import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;

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
        // Check if player already has Thunder Breathing unlocked
        if (ProgressionHelper.isStyleUnlocked(player, "thunder_breathing")) {
            return; // Already unlocked
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
        // Record the unlock in progression system (this handles all advancement triggers)
        ProgressionHelper.unlockStyle(player, "thunder_breathing");

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

        // Spawn thunder particles around the player
        if (player.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
                double offsetX = (player.getRandom().nextDouble() - 0.5) * 3.0;
                double offsetY = player.getRandom().nextDouble() * 2.0;
                double offsetZ = (player.getRandom().nextDouble() - 0.5) * 3.0;

                serverLevel.sendParticles(NichirinParticleRegistry.THUNDER.get(),
                        player.getX() + offsetX,
                        player.getY() + offsetY,
                        player.getZ() + offsetZ,
                        1, 0, 0, 0, 0);
            }
        }
    }


}