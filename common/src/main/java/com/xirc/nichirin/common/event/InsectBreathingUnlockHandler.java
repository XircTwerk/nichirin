package com.xirc.nichirin.common.event;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.advancement.NichirinCriteriaTriggers;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.entity.player.Player;

/**
 * Handles unlocking Insect Breathing when throwing poison potions
 */
public class InsectBreathingUnlockHandler {

    public static void register() {
        // Register for when player uses an item (throws potion)
        PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wonGame) -> {;
        });

        // We need to hook into entity spawn event to detect thrown poison potions
        dev.architectury.event.events.common.EntityEvent.ADD.register((entity, world) -> {
            if (entity instanceof ThrownPotion thrownPotion && !world.isClientSide) {
                // Check if it's a poison potion thrown by a player
                if (thrownPotion.getOwner() instanceof ServerPlayer player) {
                    checkPoisonPotionThrow(player, thrownPotion);
                }
            }

            return dev.architectury.event.EventResult.pass();
        });
    }

    /**
     * Checks if thrown potion is a poison potion and unlocks Insect Breathing
     */
    private static void checkPoisonPotionThrow(ServerPlayer player, ThrownPotion thrownPotion) {
        // Check if player already has Insect Breathing unlocked
        if (ProgressionHelper.isStyleUnlocked(player, "insect_breathing")) {
            return; // Already unlocked
        }

        // Check if the thrown potion is a poison potion
        if (PotionUtils.getPotion(thrownPotion.getItem()) == Potions.POISON ||
                PotionUtils.getPotion(thrownPotion.getItem()) == Potions.LONG_POISON ||
                PotionUtils.getPotion(thrownPotion.getItem()) == Potions.STRONG_POISON) {

            // Unlock Insect Breathing!
            unlockInsectBreathing(player);
        }
    }

    /**
     * Unlocks Insect Breathing for the player
     */
    private static void unlockInsectBreathing(ServerPlayer player) {
        // Record the unlock in progression system (this handles all advancement triggers)
        ProgressionHelper.unlockStyle(player, "insect_breathing");

        // Set Insect Breathing as the player's style
        PlayerDataProvider.updateAndSync(player, "insect_breathing");

        // Send success message
        player.displayClientMessage(
                Component.literal("🦋 Toxic Elegance achieved! You have mastered poison and unlocked Insect Breathing! 🦋")
                        .withStyle(style -> style.withColor(0x9932CC).withBold(true)), // Purple color
                false
        );

        // Play a nature/poison-related sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.BREWING_STAND_BREW,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.2f);

        // Spawn insect/nature particles around the player
        if (player.level() instanceof ServerLevel serverLevel) {
            // Spawn purple particles to represent poison adaptation
            for (int i = 0; i < 25; i++) {
                double offsetX = (player.getRandom().nextDouble() - 0.5) * 2.5;
                double offsetY = player.getRandom().nextDouble() * 2.5;
                double offsetZ = (player.getRandom().nextDouble() - 0.5) * 2.5;

                // Use a nature/poison-themed particle if available, otherwise use a suitable substitute
                // Try to use custom insect particle if it exists
                serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
                        player.getX() + offsetX,
                        player.getY() + offsetY,
                        player.getZ() + offsetZ,
                        1, 0, 0, 0, 0);
            }
        }

        // Remove poison effect as a reward for unlocking
        player.removeEffect(MobEffects.POISON);

        // Give a small health boost as reward
        player.heal(2.0f);
    }
}