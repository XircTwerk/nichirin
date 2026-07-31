package com.xirc.nichirin.common.system;

import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.data.PlayerDataStorage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Tracks which Upper Moon demons have spared a given player.
 *
 * <p>When an un-enraged Upper Moon lands a killing blow on a human, it spares them and offers demon
 * blood instead. That demon then stays neutral toward the player forever — unless the player attacks
 * it again, in which case it's a fight to the death. The mark is <em>per demon type</em>: being
 * spared by Akaza does nothing for the other Upper Moons.</p>
 */
public final class UpperMoonPact {

    private UpperMoonPact() {}

    /** True if {@code demonType} has already spared this player. */
    public static boolean isSpared(Player player, String demonType) {
        return PlayerDataProvider.getData(player).isSparedBy(demonType);
    }

    /** Records that {@code demonType} spared this player, then persists and syncs the change. */
    public static void mark(ServerPlayer player, String demonType) {
        PlayerDataProvider.getData(player).setSparedBy(demonType);
        PlayerDataStorage.savePlayerData(player);
        if (player.server != null) {
            PlayerDataProvider.forceSync(player.server);
        }
    }

    /** Removes {@code demonType}'s pact from this player (debug/admin). Returns true if one existed. */
    public static boolean unmark(ServerPlayer player, String demonType) {
        boolean removed = PlayerDataProvider.getData(player).removeSparedBy(demonType);
        if (removed) {
            PlayerDataStorage.savePlayerData(player);
            if (player.server != null) {
                PlayerDataProvider.forceSync(player.server);
            }
        }
        return removed;
    }
}
