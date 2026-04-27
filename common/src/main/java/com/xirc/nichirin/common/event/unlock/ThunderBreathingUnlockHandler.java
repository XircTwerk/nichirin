package com.xirc.nichirin.common.event.unlock;

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
        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            if ((source.getDirectEntity() instanceof LightningBolt ||
                    source.getEntity() instanceof LightningBolt ||
                    source.getMsgId().equals("lightningBolt"))
                    && entity instanceof Player
                    && !entity.level().isClientSide) {

                Player player = (Player) entity;

                if (isWearingNoArmor(player)) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        checkThunderBreathingUnlock(serverPlayer);
                    }
                }
            }

            return dev.architectury.event.EventResult.pass();
        });
    }

    private static void checkThunderBreathingUnlock(ServerPlayer player) {
        if (ProgressionHelper.isStyleUnlocked(player, "thunder_breathing")) return;
        if (!isWearingNoArmor(player)) return;
        unlockThunderBreathing(player);
    }

    private static boolean isWearingNoArmor(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() &&
                player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() &&
                player.getItemBySlot(EquipmentSlot.LEGS).isEmpty() &&
                player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
    }

    private static void unlockThunderBreathing(ServerPlayer player) {
        ProgressionHelper.unlockStyle(player, "thunder_breathing");
        PlayerDataProvider.updateAndSync(player, "thunder_breathing");

        player.displayClientMessage(
                Component.literal("⚡ You have been baptized by the storm! Thunder Breathing unlocked! ⚡")
                        .withStyle(style -> style.withColor(0xFFFF55).withBold(true)),
                false
        );

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

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