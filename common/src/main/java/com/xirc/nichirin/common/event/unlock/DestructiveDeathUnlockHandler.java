package com.xirc.nichirin.common.event.unlock;

import com.xirc.nichirin.common.data.PlayerDataStorage;
import com.xirc.nichirin.common.data.ProgressionHelper;
import com.xirc.nichirin.common.entity.npc.DemonNPCEntity;
import com.xirc.nichirin.common.system.DemonManager;
import com.xirc.nichirin.common.util.NichirinDamageSources;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Unlocks the Destructive Death demon art once a demon player slays {@link #REQUIRED_KILLS} demons
 * using only close-quarters combat — i.e. each killing blow is dealt by a CQC attack (damage tagged
 * {@link NichirinDamageSources#CQC}). Progress is tracked as internal progression
 * ({@code MovesetProgression.cqcDemonKills}) so it persists with the player's save and stays out of
 * the vanilla statistics screen.
 */
public class DestructiveDeathUnlockHandler {

    private static final int REQUIRED_KILLS = 5;

    public static void register() {
        EntityEvent.LIVING_DEATH.register((entity, damageSource) -> {
            // Must be a demon NPC
            if (!(entity instanceof DemonNPCEntity)) return EventResult.pass();

            // Must be killed by a player
            if (!(damageSource.getEntity() instanceof ServerPlayer player)) return EventResult.pass();

            // Only demons can earn Destructive Death.
            if (!DemonManager.isDemon(player)) return EventResult.pass();

            // The killing blow must come from a close-quarters-combat attack. Use the wrapper-aware
            // check because percentage-damage mode swaps the source to a PercentageDamageSource.
            if (!NichirinDamageSources.is(damageSource, NichirinDamageSources.CQC)) return EventResult.pass();

            // Already unlocked?
            if (ProgressionHelper.isMovesetUnlocked(player, "destructive_death")) return EventResult.pass();

            recordCqcDemonKill(player);
            return EventResult.pass();
        });
    }

    private static void recordCqcDemonKill(ServerPlayer player) {
        // Tracked as internal progression (shown in the GUI's obtainment tab), not a vanilla stat.
        int kills = ProgressionHelper.getProgression(player).addCqcDemonKill();
        PlayerDataStorage.savePlayerData(player);

        if (kills >= REQUIRED_KILLS) {
            unlockDestructiveDeath(player);
        } else {
            player.displayClientMessage(
                    Component.literal("Demon slain by hand (" + kills + "/" + REQUIRED_KILLS + ")")
                            .withStyle(style -> style.withColor(0xAA0000)),
                    true
            );
        }
    }

    private static void unlockDestructiveDeath(ServerPlayer player) {
        ProgressionHelper.unlockMoveset(player, "destructive_death");

        player.displayClientMessage(
                Component.literal("You tore demons apart with your bare hands. Destructive Death unlocked!")
                        .withStyle(style -> style.withColor(0xAA0000).withBold(true)),
                false
        );

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SPAWN,
                SoundSource.PLAYERS, 0.6f, 1.4f);
    }
}
