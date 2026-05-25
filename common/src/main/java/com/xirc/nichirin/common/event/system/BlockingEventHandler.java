package com.xirc.nichirin.common.event.system;

import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.system.StanceManager;
import com.xirc.nichirin.common.system.blocking.KatanaBlock;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import com.xirc.nichirin.registry.NicirinSoundRegistry;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;

/** Event handler for the blocking/parry system and stance management. */
public class BlockingEventHandler {

    public static void register() {
        TickEvent.PLAYER_POST.register(player -> {
            if (!player.level().isClientSide) {
                StanceManager.tick(player);
                KatanaBlock.tick(player);
            }
        });

        PlayerEvent.PLAYER_JOIN.register(player -> {
            if (!player.level().isClientSide) {
                if (StanceManager.getStance(player) <= 0) {
                    StanceManager.restoreFull(player);
                }
                StanceManager.forceSyncToClient(player);
            }
        });

        PlayerEvent.PLAYER_QUIT.register(player -> {
            StanceManager.cleanupPlayer(player);
            KatanaBlock.cleanupPlayer(player);
        });

        PlayerEvent.PLAYER_RESPAWN.register((newPlayer, conqueredEnd) -> {
            if (!conqueredEnd) {
                StanceManager.restoreFull(newPlayer);
                StanceManager.forceSyncToClient(newPlayer);
            }
        });

        EntityEvent.LIVING_HURT.register((entity, damageSource, amount) -> {
            if (entity instanceof Player player && !player.level().isClientSide) {
                if (KatanaBlock.isBlocking(player)) {
                    Player playerAttacker = null;
                    net.minecraft.world.entity.LivingEntity attackingEntity = null;

                    if (damageSource.getEntity() instanceof Player playerAtk) {
                        playerAttacker = playerAtk;
                        attackingEntity = playerAtk;
                    } else if (damageSource.getEntity() instanceof net.minecraft.world.entity.LivingEntity livingAtk) {
                        attackingEntity = livingAtk;
                    }

                    boolean handled = KatanaBlock.handleIncomingDamage(player, playerAttacker, amount);

                    if (handled && KatanaBlock.getStance(player) == KatanaBlock.BlockingStance.PARRY_SUCCESS) {
                        if (!NichirinModConfig.get().combat.enableParrySystem) {
                            return EventResult.pass();
                        }

                        if (attackingEntity != null && attackingEntity != player) {
                            attackingEntity.addEffect(new MobEffectInstance(
                                    NichirinEffectRegistry.STUNNED.get(),
                                    30, 0, false, false, true));
                        }

                        NichirinPacketRegistry.sendParrySpark(player);

                        net.minecraft.sounds.SoundEvent clashSound = player.level().random.nextBoolean()
                                ? NicirinSoundRegistry.PARRY_CLASH.get()
                                : NicirinSoundRegistry.PARRY_CLASH_2.get();
                        player.level().playSound(null,
                                player.getX(), player.getY(), player.getZ(),
                                clashSound, SoundSource.PLAYERS, 1.0f, 1.0f);

                        return EventResult.interruptTrue();
                    }
                }
            }
            return EventResult.pass();
        });
    }
}