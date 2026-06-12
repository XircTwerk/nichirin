package com.xirc.nichirin.common.attack.moves.demon.destructive;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Destructive Death M2 — Compass Needle.
 *
 * <p>Activates a 6-second tracking window: refreshes a list of nearby living entities each tick,
 * stores their UUIDs in {@link CompassNeedleTracker}, and toggles the global state flag so attacks
 * can apply the damage multiplier. Crouch variant ({@link CompassOverdriveAttack}) adds buffs and
 * red-shockwave behaviour.</p>
 */
public class CompassNeedleAttack extends DestructiveDeathAttackBase {

    protected static final int DEFAULT_DURATION_TICKS = 120; // 6 seconds
    protected static final double TRACK_RADIUS = 20.0;

    protected boolean activated;

    @Override
    protected void onStart() {
        activated = false;
        if (!(user instanceof ServerPlayer sp)) return;
        DestructiveDeathState.activateCompass(sp, DEFAULT_DURATION_TICKS, isOverdriveMode());
        activated = true;
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.5f);
        sp.displayClientMessage(
                Component.literal("Compass Needle activated")
                        .withStyle(s -> s.withColor(0xAACCFF)), true);
    }

    @Override
    protected void perform() {
        if (!(user instanceof ServerPlayer sp)) return;
        if (!DestructiveDeathState.isCompassActive(sp.getUUID(), world.getGameTime())) return;
        refreshTrackedTargets(sp);
    }

    protected void refreshTrackedTargets(ServerPlayer sp) {
        AABB box = new AABB(
                sp.getX() - TRACK_RADIUS, sp.getY() - TRACK_RADIUS, sp.getZ() - TRACK_RADIUS,
                sp.getX() + TRACK_RADIUS, sp.getY() + TRACK_RADIUS, sp.getZ() + TRACK_RADIUS);
        List<LivingEntity> candidates = world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != sp && e.isAlive());
        Set<UUID> ids = new HashSet<>();
        for (LivingEntity e : candidates) ids.add(e.getUUID());
        CompassNeedleTracker.setTrackedTargets(sp.getUUID(), ids);
    }

    @Override
    protected void onStop() {
        // Compass keeps running on its own timer in DestructiveDeathState; this attack just kicked
        // it off. Don't clear the tracker here — Compass keeps tracking until its 6s expires.
    }

    protected boolean isOverdriveMode() {
        return false;
    }
}
