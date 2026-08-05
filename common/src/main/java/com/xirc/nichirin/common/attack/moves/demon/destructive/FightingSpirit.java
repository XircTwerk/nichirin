package com.xirc.nichirin.common.attack.moves.demon.destructive;

import net.minecraft.world.entity.LivingEntity;

/** Shared fighting-spirit contract used by Compass Needle and future Selfless State techniques. */
public final class FightingSpirit {
    public static final String SELFLESS_STATE_TAG = "nichirin_selfless_state";
    public static final String TRANSPARENT_WORLD_TAG = "nichirin_transparent_world";

    private FightingSpirit() {}

    public static boolean isSelfless(LivingEntity entity) {
        return entity.getTags().contains(SELFLESS_STATE_TAG);
    }

    public static void setSelfless(LivingEntity entity, boolean selfless) {
        if (selfless) entity.addTag(SELFLESS_STATE_TAG);
        else entity.removeTag(SELFLESS_STATE_TAG);
    }

    public static boolean hasTransparentWorld(LivingEntity entity) {
        return entity.getTags().contains(TRANSPARENT_WORLD_TAG);
    }

    public static void setTransparentWorld(LivingEntity entity, boolean active) {
        if (active) entity.addTag(TRANSPARENT_WORLD_TAG);
        else entity.removeTag(TRANSPARENT_WORLD_TAG);
    }
}
