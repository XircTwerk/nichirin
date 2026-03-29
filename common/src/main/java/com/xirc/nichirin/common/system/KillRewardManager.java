package com.xirc.nichirin.common.system;

import com.xirc.nichirin.common.attack.moveset.breathing.*;
import com.xirc.nichirin.common.attack.moveset.demon.DefaultDemonMoveset;
import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.util.BreathingManager;
import com.xirc.nichirin.common.util.StaminaManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Handles universal kill rewards (heal, cooldown reset, stamina/breath/stance restore).
 * Applied to any player who kills another entity regardless of their moveset.
 */
public class KillRewardManager {

    public static void onKill(ServerPlayer killer, LivingEntity killed) {
        NichirinModConfig.KillRewardsConfig cfg = NichirinModConfig.get().killRewards;
        boolean killedPlayer = killed instanceof Player;

        if (killedPlayer) {
            if (cfg.healOnKillPlayer) {
                if (cfg.healOnKillPlayerAsMaxHealth) {
                    killer.setHealth(killer.getMaxHealth());
                } else {
                    killer.heal(cfg.healOnKillPlayerAmount);
                }
            }
        } else {
            if (cfg.healOnKillMob) {
                killer.heal(cfg.healOnKillMobAmount);
            }
        }

        if (cfg.resetCooldownsOnKill) {
            ThunderBreathingMoveset.resetCooldowns(killer);
            FlameBreathingMoveset.resetCooldowns(killer);
            WaterBreathingMoveset.resetCooldowns(killer);
            InsectBreathingMoveset.resetCooldowns(killer);
            SoundBreathingMoveset.resetCooldowns(killer);
            DefaultDemonMoveset.resetCooldowns(killer);
        }

        if (cfg.resetStaminaOnKill) {
            StaminaManager.restoreFull(killer);
        }

        if (cfg.resetBreathOnKill) {
            BreathingManager.restoreFull(killer);
        }

        if (cfg.resetStanceOnKill) {
            StanceManager.restoreFull(killer);
        }
    }
}
