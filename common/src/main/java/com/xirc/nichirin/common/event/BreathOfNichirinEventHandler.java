package com.xirc.nichirin.common.event;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.component.AbstractDemonAttack;
import com.xirc.nichirin.common.attack.moveset.DefaultKatanaMoveset;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.event.item.RiceInteractionHandler;
import com.xirc.nichirin.common.event.system.DemonFoodHandler;
import com.xirc.nichirin.common.system.BloodMoonManager;
import com.xirc.nichirin.common.system.DemonManager;
import com.xirc.nichirin.common.system.KillRewardManager;
import com.xirc.nichirin.common.system.movement.MovementContext;
import com.xirc.nichirin.common.system.sheathing.SheathingManager;
import com.xirc.nichirin.common.util.InputHandler;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.LootEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Set;

public class BreathOfNichirinEventHandler {

    private static MinecraftServer currentServer;

    public static void init() {
        LifecycleEvent.SERVER_STARTING.register(BreathOfNichirinEventHandler::onServerStarting);
        LifecycleEvent.SERVER_STOPPING.register(BreathOfNichirinEventHandler::onServerStopping);
        TickEvent.SERVER_PRE.register(BreathOfNichirinEventHandler::onServerTick);

        PlayerDataProvider.register();
        registerDemonEvents();
        RiceInteractionHandler.register();
        registerLootInjection();
    }

    private static final Set<ResourceLocation> RICE_LOOT_TABLES = Set.of(
            new ResourceLocation("minecraft", "chests/shipwreck_supply"),
            new ResourceLocation("minecraft", "chests/village/village_plains_house"),
            new ResourceLocation("minecraft", "chests/village/village_taiga_house"),
            new ResourceLocation("minecraft", "chests/village/village_snowy_house"),
            new ResourceLocation("minecraft", "chests/village/village_savanna_house"),
            new ResourceLocation("minecraft", "chests/village/village_desert_house"),
            new ResourceLocation("minecraft", "chests/pillager_outpost"),
            new ResourceLocation("minecraft", "chests/abandoned_mineshaft")
    );

    private static void registerLootInjection() {
        LootEvent.MODIFY_LOOT_TABLE.register((manager, id, context, builtin) -> {
            if (RICE_LOOT_TABLES.contains(id)) {
                context.addPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(NichirinItemRegistry.RICE.get())
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 6))))
                        .build());
            }
        });
    }

    public static void syncPerksToPlayer(ServerPlayer player) {
    }

    private static void registerDemonEvents() {
        PlayerEvent.PLAYER_RESPAWN.register((oldPlayer, keepEverything) -> {
            if (MovesetHelper.hasDemonMoveset(oldPlayer)) {
                handlePlayerRespawn(oldPlayer);
            }
        });

        PlayerEvent.PLAYER_QUIT.register((player) -> {
            handlePlayerDisconnect(player);
        });
    }

    private static void onServerStarting(MinecraftServer server) {
        currentServer = server;
        clearTransientCombatState();
    }

    private static void onServerStopping(MinecraftServer server) {
        currentServer = null;
        clearTransientCombatState();
        DemonManager.clearAll();
    }

    private static void clearTransientCombatState() {
        NichirinPacketRegistry.clearServerPlayerStates();
        MovementContext.clearAll();
        SheathingManager.clearAll();
        DefaultKatanaMoveset.clearAll();
        InputHandler.clearAll();
    }

    private static void onServerTick(MinecraftServer server) {
        if (server != null) {
            BloodMoonManager.onServerTick(server);
            MoveExecutor.tickAllAttacks(server);
            AbstractDemonAttack.tickAllActiveAttacks(server);

            // Check for demon players with 0 blood and kill them; keep night vision active
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (MovesetHelper.hasDemonMoveset(player)) {
                    int bloodPoints = DemonManager.getBloodPoints(player);

                    if (bloodPoints <= 0 && player.isAlive()) {
                        AbstractDemonAttack.clearSelfTickingAttacks(player);
                        player.hurt(player.damageSources().magic(), Float.MAX_VALUE);
                    }

                    // Maintain infinite night vision while a demon
                    MobEffectInstance existing = player.getEffect(MobEffects.NIGHT_VISION);
                    if (existing == null || existing.getDuration() < 200) {
                        player.addEffect(new MobEffectInstance(
                                MobEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
                    }
                } else {
                    // Not a demon — strip demon-granted night vision (long duration) immediately
                    MobEffectInstance existing = player.getEffect(MobEffects.NIGHT_VISION);
                    if (existing != null && existing.getDuration() > 1000) {
                        player.removeEffect(MobEffects.NIGHT_VISION);
                    }
                }
            }
        }
    }

    private static void handlePlayerRespawn(Player player) {
        try {
            AbstractDemonAttack.clearSelfTickingAttacks(player);
            DemonManager.cleanupPlayer(player);
            DemonFoodHandler.cleanupPlayer(player);
            DemonManager.setBloodPoints(player, 10);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handlePlayerDisconnect(Player player) {
        try {
            AbstractDemonAttack.clearSelfTickingAttacks(player);
            DemonManager.cleanupPlayer(player);
            DemonFoodHandler.cleanupPlayer(player);
            NichirinPacketRegistry.cleanupPlayer(player);
            MovementContext.cleanupPlayer(player);
            SheathingManager.cleanupPlayer(player);
            DefaultKatanaMoveset.cleanupPlayer(player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onMobKilled(ServerPlayer player, LivingEntity killedEntity) {
        DemonManager.onMobKilled((Player) player, killedEntity);
        KillRewardManager.onKill(player, killedEntity);
    }

    public static void onBiteHit(ServerPlayer player, LivingEntity target) {
        DemonManager.onBiteHit((Player) player, target);
    }
}
