package com.xirc.nichirin.common.event.system;

import com.xirc.nichirin.common.system.DemonManager;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.phys.AABB;

/**
 * Makes golems (iron/snow) hunt demon players on sight. Hostile-mob neutrality toward demons is
 * handled separately by {@code MobDemonTargetMixin}; golems need this nudge because they have no
 * vanilla goal that targets players, so we periodically point nearby idle golems at demons.
 */
public class DemonAggroHandler {

    private static final int SCAN_INTERVAL = 20;
    private static final double SCAN_RADIUS = 24.0;
    private static int tickCounter = 0;

    public static void register() {
        TickEvent.SERVER_POST.register(server -> {
            if (++tickCounter < SCAN_INTERVAL) return;
            tickCounter = 0;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!DemonManager.isDemon(player) || player.isCreative() || player.isSpectator()) continue;

                AABB area = player.getBoundingBox().inflate(SCAN_RADIUS);
                for (Mob mob : player.serverLevel().getEntitiesOfClass(AbstractGolem.class, area)) {
                    if (mob.getTarget() == null || !mob.getTarget().isAlive()) {
                        mob.setTarget(player);
                    }
                }
            }
        });
    }
}
