package com.xirc.nichirin.common.event.unlock;

import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Handles unlocking Insect Breathing when throwing poison potions
 */
public class InsectBreathingUnlockHandler {

    public static void register() {
        PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wonGame) -> {;
        });

        EntityEvent.ADD.register((entity, world) -> {
            if (entity instanceof ThrownPotion thrownPotion && !world.isClientSide) {
                if (thrownPotion.getOwner() instanceof ServerPlayer player) {
                    checkPoisonPotionThrow(player, thrownPotion);
                }
            }

            return EventResult.pass();
        });
    }

    private static void checkPoisonPotionThrow(ServerPlayer player, ThrownPotion thrownPotion) {
        if (ProgressionHelper.isStyleUnlocked(player, "insect_breathing")) return;

        if (PotionUtils.getPotion(thrownPotion.getItem()) == Potions.POISON ||
                PotionUtils.getPotion(thrownPotion.getItem()) == Potions.LONG_POISON ||
                PotionUtils.getPotion(thrownPotion.getItem()) == Potions.STRONG_POISON) {
            unlockInsectBreathing(player);
        }
    }

    private static void unlockInsectBreathing(ServerPlayer player) {
        ProgressionHelper.unlockStyle(player, "insect_breathing");
        PlayerDataProvider.updateAndSync(player, "insect_breathing");

        player.displayClientMessage(
                Component.literal(" Toxic Elegance achieved! You have mastered poison and unlocked Insect Breathing! ")
                        .withStyle(style -> style.withColor(0x9932CC).withBold(true)),
                false
        );

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BREWING_STAND_BREW,
                SoundSource.PLAYERS, 1.0f, 1.2f);

        if (player.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 25; i++) {
                double offsetX = (player.getRandom().nextDouble() - 0.5) * 2.5;
                double offsetY = player.getRandom().nextDouble() * 2.5;
                double offsetZ = (player.getRandom().nextDouble() - 0.5) * 2.5;

                serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
                        player.getX() + offsetX,
                        player.getY() + offsetY,
                        player.getZ() + offsetZ,
                        1, 0, 0, 0, 0);
            }
        }

        player.removeEffect(MobEffects.POISON);
        player.heal(2.0f);
    }
}
