package com.xirc.nichirin.common.event.unlock;

import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Unlocks Beast Breathing when a player kills a Boar and equips the dropped boar head.
 * Step 1: Kill a boar → receive boar head in inventory.
 * Step 2: Equip the boar head → Beast Breathing unlocked.
 */
public class BeastBreathingUnlockHandler {

    public static void register() {
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            onEntityDeath(entity, source);
            return EventResult.pass();
        });
        TickEvent.SERVER_PRE.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkBoarHeadEquipped(player);
            }
        });
    }

    private static void onEntityDeath(LivingEntity entity, DamageSource source) {
        if (entity == null || source == null) return;
        if (entity.getType() != NichirinEntityRegistry.BOAR.get()) return;

        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player player)) return;

        ItemStack boarHead = new ItemStack(NichirinItemRegistry.BOAR_HEAD.get());
        entity.spawnAtLocation(boarHead);
    }

    private static void checkBoarHeadEquipped(ServerPlayer player) {
        if (ProgressionHelper.isStyleUnlocked(player, "beast_breathing")) return;

        ItemStack helmet = player.getInventory().getArmor(3); // Head slot
        if (helmet.getItem() != NichirinItemRegistry.BOAR_HEAD.get()) return;

        unlockBeastBreathing(player);
    }

    private static void unlockBeastBreathing(ServerPlayer player) {
        ProgressionHelper.unlockStyle(player, "beast_breathing");
        PlayerDataProvider.updateAndSync(player, "beast_breathing");

        player.displayClientMessage(
                Component.literal("HAAHH!! Beast Breathing unlocked! You are now one with the beast!")
                        .withStyle(style -> style.withColor(0x8B4513).withBold(true)),
                false
        );

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0f, 0.8f);
    }
}